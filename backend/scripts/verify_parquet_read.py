"""R2上のParquetファイルをDuckDBから直接読み込む検証スクリプト。

BackendがParquetデータレイクを参照する仕組みの実証実験用コードです。
DuckDBの httpfs 拡張機能を使用して、S3互換ストレージ(R2)上のファイルを
テーブルとしてクエリします。

Usage:
    uv run python backend/scripts/verify_parquet_read.py
"""

import logging
import sys
import os
import duckdb
from tabulate import tabulate

# プロジェクトルートをパスに追加
sys.path.append(os.getcwd())

from shared.config import Config

# ロギング設定
logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)


def test_parquet_read():
    """R2上のParquetファイルを読み込んで表示する。"""
    logger.info("🦆 Testing DuckDB Parquet Read from R2...")

    try:
        config = Config.from_env()
    except Exception as e:
        logger.error(f"Failed to load config: {e}")
        return

    if not config.duckdb or not config.duckdb.r2:
        logger.error("R2 configuration is missing.")
        return

    r2_conf = config.duckdb.r2

    # DuckDB インメモリデータベースの初期化
    conn = duckdb.connect(":memory:")

    # S3(R2) 設定の適用
    # httpfs 拡張機能が自動的に使用されます
    logger.info("Configuring DuckDB S3 secrets...")
    conn.execute(f"""
        INSTALL httpfs;
        LOAD httpfs;
        CREATE SECRET (
            TYPE S3,
            KEY_ID '{r2_conf.access_key_id}',
            SECRET '{r2_conf.secret_access_key.get_secret_value()}',
            REGION 'auto',
            ENDPOINT '{r2_conf.endpoint_url.replace("https://", "")}',
            URL_STYLE 'path'
        );
    """)

    # Parquetファイルのパスパターン
    # events/spotify/plays/year=*/month=*/ -> 再帰的に読み込むには **/*.parquet が便利ですが
    # glob構文はDuckDBのバージョンやhttpfsの実装によるため、まずはワイルドカードを試します。
    parquet_url = (
        f"s3://{r2_conf.bucket_name}/{r2_conf.events_path}spotify/plays/**/*.parquet"
    )

    logger.info(f"Querying Parquet from: {parquet_url}")

    try:
        # 件数確認
        count = conn.execute(
            f"SELECT COUNT(*) FROM read_parquet('{parquet_url}')"
        ).fetchone()[0]
        logger.info(f"✅ Total Records found in R2 Parquet: {count}")

        if count > 0:
            # 最新5件を表示
            logger.info("\n📊 Latest 5 Records:")
            df = conn.execute(f"""
                SELECT played_at_utc, track_name, artist_names, album_name 
                FROM read_parquet('{parquet_url}')
                ORDER BY played_at_utc DESC
                LIMIT 5
            """).df()
            print(tabulate(df, headers="keys", tablefmt="simple_grid"))

    except duckdb.IOException as e:
        logger.error(f"❌ Failed to read Parquet: {e}")
        logger.info(
            "Hint: Parquetファイルがまだ生成されていない可能性があります。Ingestを実行してください。"
        )
    except Exception as e:
        logger.error(f"❌ Unexpected error: {e}")
    finally:
        conn.close()


if __name__ == "__main__":
    test_parquet_read()
