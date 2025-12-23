"""Last.fm → R2 エンリッチメントパイプライン。

R2 上の Spotify 再生履歴から未取得のトラック/アーティストを特定し、
Last.fm API からメタデータを取得して R2 に保存します。
"""

import logging
import sys
from datetime import datetime, timezone

import duckdb
from pydantic import ValidationError

from ingest.lastfm.collector import LastFmCollector
from ingest.lastfm.storage import LastFmStorage
from ingest.lastfm.transform import transform_artist_info, transform_track_info
from ingest.lastfm.schema import LastFmSchema
from shared import Config, log_execution_time

logger = logging.getLogger(__name__)


def setup_duckdb_r2(conn: duckdb.DuckDBPyConnection, config: Config):
    """DuckDB で R2 (S3) を読み書きするための設定を行います。"""
    r2 = config.duckdb.r2
    conn.execute(f"SET s3_endpoint='{r2.endpoint_url.replace('https://', '')}';")
    conn.execute(f"SET s3_access_key_id='{r2.access_key_id}';")
    conn.execute(
        f"SET s3_secret_access_key='{r2.secret_access_key.get_secret_value()}';"
    )
    conn.execute("SET s3_region='auto';")
    conn.execute("SET s3_url_style='path';")


@log_execution_time
def main():
    """メインパイプライン実行処理。"""
    logger.info("=" * 60)
    logger.info("EgoGraph Last.fm Enrichment Pipeline (R2)")
    logger.info(f"Started at: {datetime.now(timezone.utc).isoformat()}")
    logger.info("=" * 60)

    try:
        config = Config.from_env()
        if not config.lastfm or not config.duckdb or not config.duckdb.r2:
            raise ValueError("Last.fm and R2 configuration are required")

        r2_conf = config.duckdb.r2

        # Storage & Collector 初期化
        storage = LastFmStorage(
            endpoint_url=r2_conf.endpoint_url,
            access_key_id=r2_conf.access_key_id,
            secret_access_key=r2_conf.secret_access_key.get_secret_value(),
            bucket_name=r2_conf.bucket_name,
            raw_path=r2_conf.raw_path,
            events_path=r2_conf.events_path,
            master_path=r2_conf.master_path,
        )

        collector = LastFmCollector(
            api_key=config.lastfm.api_key,
            api_secret=config.lastfm.api_secret.get_secret_value(),
        )

        # DuckDB を使用して R2 上のデータを分析
        conn = duckdb.connect(":memory:")
        setup_duckdb_r2(conn, config)

        # 1. 既に取得済みのトラック/アーティストを取得 (重複排除用)
        tracks_glob = f"s3://{r2_conf.bucket_name}/{r2_conf.master_path}lastfm/tracks/*/*/*.parquet"
        artists_glob = f"s3://{r2_conf.bucket_name}/{r2_conf.master_path}lastfm/artists/*/*/*.parquet"
        spotify_plays_glob = f"s3://{r2_conf.bucket_name}/{r2_conf.events_path}spotify/plays/*/*/*.parquet"

        # Mart ビューの初期化
        LastFmSchema.initialize_mart(conn, tracks_glob, artists_glob)

        logger.info("Identifying unique tracks and artists from Spotify plays...")

        # Spotify 再生履歴からユニークな楽曲を抽出
        query_candidates = f"""
            SELECT DISTINCT 
                track_name, 
                artist_names[1] as artist_name
            FROM read_parquet('{spotify_plays_glob}')
            WHERE track_name IS NOT NULL AND artist_names[1] IS NOT NULL
        """
        candidates = conn.execute(query_candidates).fetchall()
        logger.info(f"Found {len(candidates)} unique candidates from Spotify history.")

        # 既知のトラックを取得
        try:
            known_tracks = set(
                conn.execute(
                    "SELECT track_name, artist_name FROM mart.lastfm_tracks"
                ).fetchall()
            )
        except Exception:
            logger.info("No existing Last.fm tracks found in mart.")
            known_tracks = set()

        # 2. 未取得のトラックをフィルタリング
        to_enrich_tracks = [c for c in candidates if (c[0], c[1]) not in known_tracks]
        logger.info(f"Tracks to enrich: {len(to_enrich_tracks)}")

        # 3. エンリッチメント実行
        limit = 50  # 1回の実行あたりの上限
        enrich_tracks(collector, storage, to_enrich_tracks[:limit])

        # 既知のアーティストを取得
        unique_artists = {c[1] for c in candidates}
        try:
            known_artists = {
                r[0]
                for r in conn.execute(
                    "SELECT artist_name FROM mart.lastfm_artists"
                ).fetchall()
            }
        except Exception:
            logger.info("No existing Last.fm artists found in mart.")
            known_artists = set()

        to_enrich_artists = [a for a in unique_artists if a not in known_artists]
        logger.info(f"Artists to enrich: {len(to_enrich_artists)}")

        enrich_artists(collector, storage, to_enrich_artists[:limit])

        logger.info("Pipeline completed successfully!")

    except Exception:
        logger.exception("Last.fm enrichment pipeline failed")
        sys.exit(1)


def enrich_tracks(
    collector: LastFmCollector, storage: LastFmStorage, tracks: list[tuple[str, str]]
) -> str | None:
    """楽曲リストをエンリッチメントして保存します。"""
    if not tracks:
        logger.info("No new tracks to enrich.")
        return None

    enriched_results = []
    for track_name, artist_name in tracks:
        logger.info(f"Enriching: {artist_name} - {track_name}")
        try:
            info = collector.get_track_info(artist_name, track_name)
            if info:
                enriched_results.append(transform_track_info(info))
            else:
                logger.info("  -> Not found on Last.fm")
        except Exception:
            logger.exception(f"  -> Error enriching {artist_name} - {track_name}")

    if enriched_results:
        now = datetime.now(timezone.utc)
        return storage.save_parquet(
            enriched_results, now.year, now.month, prefix="lastfm/tracks"
        )
    return None


def enrich_artists(
    collector: LastFmCollector, storage: LastFmStorage, artists: list[str]
) -> str | None:
    """アーティストリストをエンリッチメントして保存します。"""
    if not artists:
        logger.info("No new artists to enrich.")
        return None

    artist_results = []
    for artist_name in artists:
        logger.info(f"Enriching Artist: {artist_name}")
        try:
            info = collector.get_artist_info(artist_name)
            if info:
                artist_results.append(transform_artist_info(info))
        except Exception:
            logger.exception(f"  -> Error enriching artist {artist_name}")

    if artist_results:
        now = datetime.now(timezone.utc)
        return storage.save_parquet(
            artist_results, now.year, now.month, prefix="lastfm/artists"
        )
    return None


if __name__ == "__main__":
    main()
