"""FastAPI dependency functions.

設定の取得、DuckDB接続ファクトリなどの依存関数を提供します。
"""

import logging
from collections.abc import Generator

import duckdb
from fastapi import Depends

from backend.config import BackendConfig
from backend.infrastructure.database import DuckDBConnection

logger = logging.getLogger(__name__)

# グローバル設定（1回だけロード）
_config: BackendConfig | None = None


def get_config() -> BackendConfig:
    """Backend設定を取得します。

    初回呼び出し時に環境変数から設定をロードし、キャッシュします。

    Returns:
        BackendConfig

    Raises:
        ValueError: 必須設定が不足している場合
    """
    global _config
    if _config is None:
        logger.info("Loading backend configuration")
        _config = BackendConfig.from_env()
    return _config


def get_db_connection(
    config: BackendConfig = Depends(get_config),
) -> Generator[duckdb.DuckDBPyConnection, None, None]:
    """DuckDB接続を取得します（R2データレイク用）。

    DuckDBConnectionをコンテキストマネージャーとして使用し、
    開かれた接続をyieldします。接続は自動的にクローズされます。

    Args:
        config: Backend設定

    Yields:
        duckdb.DuckDBPyConnection: 開かれたDuckDB接続

    Raises:
        ValueError: R2設定が不足している場合
    """
    if not config.r2:
        raise ValueError("R2 configuration is required")

    with DuckDBConnection(config.r2) as conn:
        yield conn
