"""R2上のSpotify再生履歴データを検証・確認するスクリプト。

総レコード数の確認と、最新50件の再生履歴を表示します。
DuckDBの httpfs 拡張を使用して、R2上のファイルを直接クエリします。

Usage:
    uv run python backend/scripts/verify_spotify_parquet.py
"""

import logging
import os
import sys

import duckdb
from tabulate import tabulate

# プロジェクトルートをパスに追加
sys.path.append(os.getcwd())

from shared.config import Config

# ロギング設定
logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)


def verify_r2_data():
    """R2上のParquetデータを検証し、最新の履歴を表示する。"""
    logger.info("🦆 Verifying EgoGraph R2 Data Lake...")

    try:
        config = Config.from_env()
    except Exception:
        logger.exception("Failed to load config")
        return

    if not config.duckdb or not config.duckdb.r2:
        logger.error("R2 configuration is missing.")
        return

    r2_conf = config.duckdb.r2
    conn = duckdb.connect(":memory:")

    try:
        # S3(R2) 設定の適用
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
                r2_conf.endpoint_url.replace("https://", ""),
            ],
        )

        # Parquetファイルのパスパターン
        parquet_url = f"s3://{r2_conf.bucket_name}/{r2_conf.events_path}spotify/plays/**/*.parquet"

        # 1. 総件数の確認
        count = conn.execute(
            "SELECT COUNT(*) FROM read_parquet(?)", [parquet_url]
        ).fetchone()[0]
        logger.info(f"✅ Connection successful. Total records in R2: {count}")

        if count == 0:
            logger.info("ℹ️ R2 is empty. Run ingestion first.")
            return

        # 2. 最新50件の曲名リスト表示 (シンプル表示)
        logger.info("\n📊 Latest 50 Tracks:")
        query_simple = """
            SELECT track_name, artist_names[1] as artist, played_at_utc
            FROM read_parquet(?)
            ORDER BY played_at_utc DESC
            LIMIT 50
        """
        df_simple = conn.execute(query_simple, [parquet_url]).df()

        # インデックスを1から振る
        df_simple.index = df_simple.index + 1
        print(
            tabulate(
                df_simple[["track_name", "artist"]],
                headers=["#", "Track Name", "Artist"],
                tablefmt="simple",
            )
        )

        # 3. 直近5件の詳細表示 (デバッグ用)
        logger.info("\n🔍 Detailed View (Latest 5):")
        query_detail = """
            SELECT played_at_utc, track_name, artist_names, album_name
            FROM read_parquet(?)
            ORDER BY played_at_utc DESC
            LIMIT 5
        """
        df_detail = conn.execute(query_detail, [parquet_url]).df()
        print(tabulate(df_detail, headers="keys", tablefmt="simple_grid"))

    except duckdb.IOException as e:
        if "No files found" in str(e):
            logger.warning("⚠️ No Parquet files found in the specified path.")
        else:
            logger.error(f"❌ DuckDB IO Error: {e}")
    except Exception as e:
        logger.error(f"❌ Unexpected Error: {e}")
    finally:
        conn.close()


if __name__ == "__main__":
    verify_r2_data()
