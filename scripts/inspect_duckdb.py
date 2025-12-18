"""R2上のDuckDBファイルを検査するユーティリティスクリプト。

このスクリプトは、Cloudflare R2に保存されたDuckDBファイルを一時的にダウンロードし、
その内容(テーブル一覧や各テーブルのレコード数、サンプルデータ)を表示します。

Usage:
    uv run --with pandas --with tabulate python scripts/inspect_duckdb.py

Requirements:
    - pandas
    - tabulate
    - duckdb
    - boto3
"""

import logging
import os
import sys

import duckdb
from tabulate import tabulate

# プロジェクトルートをパスに追加
sys.path.append(os.getcwd())

from ingest.spotify.r2_sync import R2Sync
from shared.config import Config

# ロギング設定
logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)


def inspect_duckdb():
    """R2からDuckDBをダウンロードして内容を検査する。"""
    logger.info("🦆 Inspecting DuckDB from R2...")

    try:
        config = Config.from_env()
    except Exception:
        logger.exception("Failed to load config")
        return

    if not config.duckdb or not config.duckdb.r2:
        logger.error("R2 configuration is missing.")
        return

    r2_config = config.duckdb.r2

    # R2Syncを初期化
    r2 = R2Sync(
        endpoint_url=r2_config.endpoint_url,
        access_key_id=r2_config.access_key_id,
        secret_access_key=r2_config.secret_access_key.get_secret_value(),
        bucket_name=r2_config.bucket_name,
        key_prefix=r2_config.key_prefix,
    )

    local_db_path = "temp_inspect.duckdb"

    # 重複を避けるため既存の一時ファイルを削除
    if os.path.exists(local_db_path):
        os.remove(local_db_path)

    logger.info(
        f"Attempting to download from Bucket: {r2.bucket_name}, Prefix: {r2.key_prefix}"
    )

    # ダウンロード試行
    if r2.download_db(local_db_path):
        logger.info("✅ Downloaded from standard path.")
    else:
        logger.error("❌ Could not find DuckDB file in R2.")
        return

    # DB内容の検査
    try:
        conn = duckdb.connect(local_db_path, read_only=True)

        # 1. テーブル一覧の表示 (全スキーマ)
        logger.info("\n📊 Tables (all schemas):")

        tables = conn.execute("""
            SELECT table_schema, table_name 
            FROM information_schema.tables 
            WHERE table_schema NOT IN ('information_schema', 'pg_catalog')
            ORDER BY table_schema, table_name
        """).df()

        if tables.empty:
            logger.info("No tables found.")
        else:
            print(tabulate(tables, headers="keys", tablefmt="simple_grid"))

            # 2. 各テーブルの件数とサンプルデータの表示
            for _, row in tables.iterrows():
                schema = row["table_schema"]
                name = row["table_name"]
                full_name = f"{schema}.{name}"

                logger.info(f"\n🔎 Inspecting table: {full_name}")

                # レコード数
                try:
                    count = conn.execute(
                        f"SELECT COUNT(*) FROM {full_name}"
                    ).fetchone()[0]
                    logger.info(f"Count: {count}")

                    if count > 0:
                        # 直近のレコードを表示(時刻カラムがある場合)
                        columns = conn.execute(f"DESCRIBE {full_name}").df()
                        time_col = None
                        for col in columns["column_name"]:
                            if "at" in col or "time" in col or "date" in col:
                                time_col = col
                                break

                        query = f"SELECT * FROM {full_name}"
                        if time_col:
                            query += f" ORDER BY {time_col} DESC"
                        query += " LIMIT 5"

                        df = conn.execute(query).df()
                        print(tabulate(df, headers="keys", tablefmt="simple_grid"))
                except Exception:
                    logger.exception(f"Failed to query table {full_name}")

        conn.close()

    except Exception:
        logger.exception("Error inspecting DuckDB")
    finally:
        # クリーンアップ
        if os.path.exists(local_db_path):
            os.remove(local_db_path)
            logger.info(f"\n🧹 Cleaned up temporary file: {local_db_path}")


if __name__ == "__main__":
    inspect_duckdb()
