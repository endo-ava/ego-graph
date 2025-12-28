"""DuckDB接続管理。

ステートレス設計：:memory:モードで毎回新規接続を作成し、
R2のParquetファイルを直接クエリします。
"""

import logging
from typing import Optional
from urllib.parse import urlparse

import duckdb

from shared.config import R2Config

logger = logging.getLogger(__name__)


class DuckDBConnection:
    """ステートレスDuckDB接続マネージャー。

    コンテキストマネージャーとして使用し、:memory:接続を作成して
    R2のParquetデータに直接アクセスします。

    Example:
        >>> r2_config = R2Config.from_env()
        >>> with DuckDBConnection(r2_config) as conn:
        ...     sql = "SELECT COUNT(*) FROM read_parquet(?)"
        ...     result = conn.execute(sql, [parquet_url])
        ...     count = result.fetchone()[0]
    """

    def __init__(self, r2_config: R2Config):
        """DuckDBConnectionを初期化します。

        Args:
            r2_config: R2設定（認証情報とバケット情報）
        """
        self.r2_config = r2_config
        self.conn: Optional[duckdb.DuckDBPyConnection] = None

    def __enter__(self) -> duckdb.DuckDBPyConnection:
        """コンテキストマネージャーのエントリー。

        :memory:接続を作成し、R2アクセス用の設定を行います。

        Returns:
            設定済みのDuckDBコネクション

        Raises:
            duckdb.Error: DuckDB接続またはR2設定に失敗した場合
        """
        logger.debug("Creating DuckDB :memory: connection")
        self.conn = duckdb.connect(":memory:")

        try:
            # httpfs拡張のインストールとロード
            self.conn.execute("INSTALL httpfs;")
            self.conn.execute("LOAD httpfs;")
            logger.debug("Loaded httpfs extension")

            # R2認証情報の設定（CREATE SECRET）
            parsed = urlparse(self.r2_config.endpoint_url)
            endpoint = parsed.netloc or parsed.path
            if not endpoint:
                raise ValueError(
                    f"Invalid R2 endpoint URL: '{self.r2_config.endpoint_url}'. "
                    "Could not extract hostname or path."
                )
            self.conn.execute(
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
                    self.r2_config.access_key_id,
                    self.r2_config.secret_access_key.get_secret_value(),
                    endpoint,
                ],
            )
            logger.debug(f"Configured R2 secret for endpoint: {endpoint}")

        except Exception:
            logger.exception("Failed to configure DuckDB connection")
            if self.conn:
                self.conn.close()
                self.conn = None
            raise

        return self.conn

    def __exit__(self, exc_type, exc_val, exc_tb):
        """コンテキストマネージャーの終了。

        接続をクローズします。
        """
        if self.conn:
            self.conn.close()
            self.conn = None
            logger.debug("Closed DuckDB connection")
