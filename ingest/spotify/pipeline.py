"""Spotify ingestion pipeline orchestration."""

import logging
from collections import defaultdict
from datetime import datetime, timezone
from typing import Any
from urllib.parse import urlparse

import duckdb
from dateutil import parser

from ingest.spotify.collector import SpotifyCollector
from ingest.spotify.storage import SpotifyStorage
from ingest.spotify.transform import (
    transform_artist_info,
    transform_plays_to_events,
    transform_track_info,
)
from shared import iso8601_to_unix_ms
from shared.config import Config

logger = logging.getLogger(__name__)


def setup_duckdb_r2(conn: duckdb.DuckDBPyConnection, r2_conf) -> None:
    """DuckDBでR2(S3互換)を読み込む設定を行う。"""
    parsed_url = urlparse(r2_conf.endpoint_url)
    endpoint_host = parsed_url.netloc if parsed_url.netloc else parsed_url.path

    conn.execute("INSTALL httpfs; LOAD httpfs;")
    conn.execute(
        """
        CREATE SECRET (
            TYPE S3,
            KEY_ID ?,
            SECRET ?,
            REGION 'auto',
            ENDPOINT ?,
            URL_STYLE 'path'
        );
        """,
        [
            r2_conf.access_key_id,
            r2_conf.secret_access_key.get_secret_value(),
            endpoint_host,
        ],
    )


def _load_existing_ids(
    conn: duckdb.DuckDBPyConnection, parquet_url: str, id_column: str
) -> set[str]:
    """既存マスターのID一覧を取得する。"""
    try:
        rows = conn.execute(
            f"SELECT DISTINCT {id_column} FROM read_parquet(?, union_by_name=true)",
            [parquet_url],
        ).fetchall()
        return {r[0] for r in rows if r[0]}
    except duckdb.IOException as e:
        if "No files found" in str(e):
            return set()
        raise


def _extract_unique_ids(items: list[dict[str, Any]]) -> tuple[set[str], set[str]]:
    """再生履歴からユニークなトラック/アーティストIDを抽出する。"""
    track_ids: set[str] = set()
    artist_ids: set[str] = set()
    for item in items:
        track = item.get("track") or {}
        track_id = track.get("id")
        if track_id:
            track_ids.add(track_id)
        for artist in track.get("artists", []):
            artist_id = artist.get("id")
            if artist_id:
                artist_ids.add(artist_id)
    return track_ids, artist_ids


def enrich_master_data(
    items: list[dict[str, Any]],
    collector: SpotifyCollector,
    storage: SpotifyStorage,
    r2_conf,
    existing_track_ids: set[str] | None = None,
    existing_artist_ids: set[str] | None = None,
) -> None:
    """再生履歴からマスター情報を補完して保存する。"""
    track_ids, artist_ids = _extract_unique_ids(items)
    if not track_ids and not artist_ids:
        logger.info("No master candidates found in recently played data.")
        return

    if existing_track_ids is None or existing_artist_ids is None:
        conn = None
        try:
            conn = duckdb.connect(":memory:")
            setup_duckdb_r2(conn, r2_conf)

            tracks_url = (
                f"s3://{r2_conf.bucket_name}/"
                f"{r2_conf.master_path}spotify/tracks/**/*.parquet"
            )
            artists_url = (
                f"s3://{r2_conf.bucket_name}/"
                f"{r2_conf.master_path}spotify/artists/**/*.parquet"
            )
            existing_track_ids = _load_existing_ids(conn, tracks_url, "track_id")
            existing_artist_ids = _load_existing_ids(conn, artists_url, "artist_id")
        except Exception:
            logger.exception("Failed to load existing Spotify master IDs")
            existing_track_ids = existing_track_ids or set()
            existing_artist_ids = existing_artist_ids or set()
        finally:
            if conn is not None:
                conn.close()

    new_track_ids = [tid for tid in track_ids if tid not in existing_track_ids]
    new_artist_ids = [aid for aid in artist_ids if aid not in existing_artist_ids]

    if new_track_ids:
        logger.info(f"Fetching {len(new_track_ids)} new track details.")
        try:
            tracks = collector.get_tracks(new_track_ids)
            # APIの生レスポンスを保存
            storage.save_raw_json(tracks, prefix="spotify/tracks")
            track_rows = [transform_track_info(t) for t in tracks if t and t.get("id")]
            if track_rows:
                now = datetime.now(timezone.utc)
                storage.save_master_parquet(
                    track_rows,
                    prefix="spotify/tracks",
                    year=now.year,
                    month=now.month,
                )
        except Exception:
            logger.exception("Failed to enrich track master data")
    else:
        logger.info("No new tracks to enrich.")

    if new_artist_ids:
        logger.info(f"Fetching {len(new_artist_ids)} new artist details.")
        try:
            artists = collector.get_artists(new_artist_ids)
            # APIの生レスポンスを保存
            storage.save_raw_json(artists, prefix="spotify/artists")
            artist_rows = [
                transform_artist_info(a) for a in artists if a and a.get("id")
            ]
            if artist_rows:
                storage.save_master_parquet(artist_rows, prefix="spotify/artists")
        except Exception:
            logger.exception("Failed to enrich artist master data")
    else:
        logger.info("No new artists to enrich.")


def run_pipeline(config: Config) -> None:
    """Spotifyインジェストの実行ロジック。"""
    if not config.spotify:
        raise ValueError("Spotify configuration is required")
    if not config.duckdb or not config.duckdb.r2:
        raise ValueError("R2 configuration is required for this pipeline")

    r2_conf = config.duckdb.r2

    storage = SpotifyStorage(
        endpoint_url=r2_conf.endpoint_url,
        access_key_id=r2_conf.access_key_id,
        secret_access_key=r2_conf.secret_access_key.get_secret_value(),
        bucket_name=r2_conf.bucket_name,
        raw_path=r2_conf.raw_path,
        events_path=r2_conf.events_path,
        master_path=r2_conf.master_path,
    )

    state_key = "state/spotify_ingest_state.json"
    state = storage.get_ingest_state(key=state_key)

    after_ms = None
    if state and state.get("latest_played_at"):
        latest_iso = state["latest_played_at"]
        try:
            after_ms = iso8601_to_unix_ms(latest_iso)
            logger.info(
                f"Incremental fetch enabled. Latest play: {latest_iso} "
                f"(Unix: {after_ms})"
            )
        except ValueError:
            logger.warning(f"Invalid timestamp in state: {latest_iso}. Full fetch.")
    else:
        logger.info("No state found. Performing full fetch (limit=50).")

    collector = SpotifyCollector(
        client_id=config.spotify.client_id,
        client_secret=config.spotify.client_secret.get_secret_value(),
        refresh_token=config.spotify.refresh_token.get_secret_value(),
        redirect_uri=config.spotify.redirect_uri,
        scope=config.spotify.scope,
    )

    items = collector.get_recently_played(after=after_ms)
    if not items:
        logger.info("No new tracks found. Exiting.")
        return

    logger.info(f"Collected {len(items)} new tracks.")

    storage.save_raw_json(items, prefix="spotify/recently_played")

    events = transform_plays_to_events(items)

    played_ats = [item.get("played_at") for item in items if item.get("played_at")]
    latest_played_at_in_batch = max(played_ats) if played_ats else None

    grouped_events = defaultdict(list)
    for event in events:
        played_at = event["played_at_utc"]
        try:
            dt = parser.parse(played_at)
            key = (dt.year, dt.month)
            grouped_events[key].append(event)
        except (ValueError, parser.ParserError) as e:
            logger.warning(f"Failed to parse date {played_at}: {e}")
            continue

    all_saved = True
    for (year, month), partition_events in grouped_events.items():
        result = storage.save_parquet(
            partition_events, year, month, prefix="spotify/plays"
        )
        if result is None:
            logger.error(f"Failed to save Parquet for {year}-{month:02d}")
            all_saved = False

    enrich_master_data(items, collector, storage, r2_conf)

    if latest_played_at_in_batch and all_saved:
        new_state = {
            "latest_played_at": latest_played_at_in_batch,
            "updated_at": datetime.now(timezone.utc).isoformat(),
        }
        storage.save_ingest_state(new_state, key=state_key)
        logger.info(f"State updated to {latest_played_at_in_batch}")
    elif latest_played_at_in_batch and not all_saved:
        logger.warning(
            "Some Parquet saves failed. State not updated to prevent data loss."
        )

    logger.info("Pipeline completed successfully!")
