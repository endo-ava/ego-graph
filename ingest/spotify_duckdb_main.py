"""Spotify → DuckDB データ取り込みパイプライン。

このシンプル化された DuckDB ファーストなパイプラインは以下を行う:
1. R2 から既存の DuckDB をダウンロード (R2設定がある場合)
2. Spotify から最近再生したトラックを取得
3. DuckDB に upsert（再生履歴 + 楽曲マスタ）
4. 更新された DuckDB を R2 にアップロード (R2設定がある場合)
"""

import logging
import os
import sys
from datetime import datetime, timezone

from ingest.spotify.collector import SpotifyCollector
from ingest.spotify.r2_sync import R2Sync
from ingest.spotify.schema import SpotifySchema
from ingest.spotify.writer import SpotifyDuckDBWriter
from shared import Config, iso8601_to_unix_ms, log_execution_time

logger = logging.getLogger(__name__)


@log_execution_time
def main():
    """メイン DuckDB パイプライン実行処理。"""
    logger.info("=" * 60)
    logger.info("EgoGraph Spotify DuckDB Pipeline")
    logger.info(f"Started at: {datetime.now(timezone.utc).isoformat()}")
    logger.info("=" * 60)

    try:
        # 設定をロード
        logger.info("Loading configuration...")
        config = Config.from_env()

        if not config.spotify:
            raise ValueError("Spotify configuration is required")
        if not config.duckdb:
            raise ValueError("DuckDB configuration is required")

        db_path = config.duckdb.db_path

        # Step 1: R2 から既存の DuckDB をダウンロード (R2が設定されている場合のみ)
        r2_sync = None
        if config.duckdb.r2:
            logger.info("\n[Step 1/5] Syncing DuckDB from R2...")
            r2_sync = R2Sync(
                endpoint_url=config.duckdb.r2.endpoint_url,
                access_key_id=config.duckdb.r2.access_key_id,
                secret_access_key=config.duckdb.r2.secret_access_key.get_secret_value(),
                bucket_name=config.duckdb.r2.bucket_name,
                key_prefix=config.duckdb.r2.key_prefix,
            )

            db_exists = r2_sync.download_db(db_path)
            if db_exists:
                metadata = r2_sync.get_db_metadata()
                logger.info(f"Downloaded existing DB: {metadata}")
            else:
                logger.info("No existing DB found, will create new one")
        else:
            logger.info("\n[Step 1/5] Checking local DuckDB (R2 sync disabled)...")
            db_exists = os.path.exists(db_path)
            if db_exists:
                logger.info(f"Found existing local DB at {db_path}")
            else:
                logger.info("No existing DB found, will create new one")

        # Step 2: DuckDB スキーマを初期化
        logger.info("\n[Step 2/5] Initializing DuckDB schema...")
        conn = SpotifySchema.initialize_db(db_path)
        SpotifySchema.create_indexes(conn)

        # 初期統計を取得
        writer = SpotifyDuckDBWriter(conn)
        initial_stats = writer.get_stats()
        logger.info(f"Initial DB stats: {initial_stats}")

        # Step 3: Spotify からデータ収集
        logger.info("\n[Step 3/5] Collecting data from Spotify API...")
        collector = SpotifyCollector(
            client_id=config.spotify.client_id,
            client_secret=config.spotify.client_secret.get_secret_value(),
            refresh_token=config.spotify.refresh_token.get_secret_value(),
            redirect_uri=config.spotify.redirect_uri,
            scope=config.spotify.scope,
        )

        # 増分取得のため、最新の再生日時を取得
        after_ms = None
        if initial_stats["latest_play"]:
            latest_play_iso = initial_stats["latest_play"]
            try:
                after_ms = iso8601_to_unix_ms(latest_play_iso)
                logger.info(
                    f"Incremental fetch enabled. Latest play in DB: {latest_play_iso} "
                    f"(Unix ms: {after_ms})"
                )
            except ValueError as e:
                logger.warning(
                    f"Failed to parse latest_play timestamp '{latest_play_iso}': {e}. "
                    f"Falling back to full fetch."
                )
                after_ms = None
        else:
            logger.info("No existing data. Performing full fetch.")

        recently_played = collector.get_recently_played(after=after_ms)
        logger.info(f"Collected {len(recently_played)} recently played tracks")

        if not recently_played:
            logger.warning("No data to process. Exiting.")
            conn.close()
            return

        # Step 4: DuckDB に書き込み
        logger.info("\n[Step 4/5] Writing to DuckDB...")
        plays_count = writer.upsert_plays(recently_played)
        tracks_count = writer.upsert_tracks(recently_played)

        final_stats = writer.get_stats()
        logger.info(f"Final DB stats: {final_stats}")

        # コネクションをクローズ
        conn.close()

        # Step 5: R2 にアップロード (R2が設定されている場合のみ)
        if r2_sync:
            logger.info("\n[Step 5/5] Uploading DuckDB to R2...")
            r2_sync.upload_db(db_path)
        else:
            logger.info(
                f"\n[Step 5/5] DuckDB saved locally at {db_path} (R2 sync disabled)"
            )

        # サマリー
        logger.info("\n" + "=" * 60)
        logger.info("Pipeline completed successfully!")
        logger.info(f"Plays upserted: {plays_count}")
        logger.info(f"Tracks upserted: {tracks_count}")
        logger.info(f"Total plays in DB: {final_stats['total_plays']}")
        logger.info(f"Total tracks in DB: {final_stats['total_tracks']}")
        logger.info(f"Latest play: {final_stats['latest_play']}")
        logger.info(f"Completed at: {datetime.now(timezone.utc).isoformat()}")
        logger.info("=" * 60)

    except Exception as e:
        logger.error(f"\nFailed pipeline: {e}", exc_info=True)
        sys.exit(1)


if __name__ == "__main__":
    main()
