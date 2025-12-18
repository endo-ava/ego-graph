"""Spotify → R2 (Parquet Data Lake) データ取り込みパイプライン。

Rawデータ(JSON)と構造化データ(Parquet)をR2に保存します。
DuckDBファイル自体の同期は行わず、ステートファイルで増分取得を管理します。
"""

import logging
import sys
from collections import defaultdict
from datetime import datetime, timezone

from dateutil import parser

from ingest.spotify.collector import SpotifyCollector
from ingest.spotify.storage import SpotifyStorage
from ingest.spotify.transform import transform_plays_to_events
from shared import Config, iso8601_to_unix_ms, log_execution_time

logger = logging.getLogger(__name__)


@log_execution_time
def main():
    """メイン Ingestion パイプライン実行処理。"""
    logger.info("=" * 60)
    logger.info("EgoGraph Spotify Ingestion Pipeline (Parquet)")
    logger.info(f"Started at: {datetime.now(timezone.utc).isoformat()}")
    logger.info("=" * 60)

    try:
        # 設定をロード
        config = Config.from_env()
        if not config.spotify:
            raise ValueError("Spotify configuration is required")
        if not config.duckdb or not config.duckdb.r2:
            # R2必須 (Parquet保存のため)
            raise ValueError("R2 configuration is required for this pipeline")

        r2_conf = config.duckdb.r2

        # 1. Storage 初期化
        storage = SpotifyStorage(
            endpoint_url=r2_conf.endpoint_url,
            access_key_id=r2_conf.access_key_id,
            secret_access_key=r2_conf.secret_access_key.get_secret_value(),
            bucket_name=r2_conf.bucket_name,
            raw_path=r2_conf.raw_path,
            events_path=r2_conf.events_path,
        )

        # 2. ステート取得 (最新の再生日時)
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

        # 3. Spotify からデータ取集
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

        # 4. Raw JSON 保存
        storage.save_raw_json(items, prefix="spotify/recently_played")

        # 5. Parquet 保存 (パーティショニング)
        events = transform_plays_to_events(items)

        # 最新のタイムスタンプを元データから取得 (堅牢性の向上)
        played_ats = [item.get("played_at") for item in items if item.get("played_at")]
        latest_played_at_in_batch = max(played_ats) if played_ats else None

        # 年月でグルーピング
        grouped_events = defaultdict(list)

        for event in events:
            played_at = event["played_at_utc"]
            # パーティションキー
            try:
                dt = parser.parse(played_at)
                key = (dt.year, dt.month)
                grouped_events[key].append(event)
            except (ValueError, parser.ParserError) as e:
                logger.warning(f"Failed to parse date {played_at}: {e}")
                continue

        # Parquet保存の成功を追跡
        all_saved = True
        for (year, month), partition_events in grouped_events.items():
            result = storage.save_parquet(
                partition_events, year, month, prefix="spotify/plays"
            )
            if result is None:
                logger.error(f"Failed to save Parquet for {year}-{month:02d}")
                all_saved = False

        # 6. ステート更新
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

    except Exception:
        logger.exception("Pipeline failed")
        sys.exit(1)


if __name__ == "__main__":
    main()
