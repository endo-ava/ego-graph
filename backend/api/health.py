"""Health check endpoint."""

import logging

from fastapi import APIRouter, Depends

from backend.api.deps import get_config, get_db_connection
from backend.config import BackendConfig
from backend.database.connection import DuckDBConnection
from backend.database.queries import get_parquet_path

logger = logging.getLogger(__name__)

router = APIRouter(tags=["health"])


@router.get("/health")
@router.get("/v1/health")
async def health_check(
    db_connection: DuckDBConnection = Depends(get_db_connection),
    config: BackendConfig = Depends(get_config),
):
    """ヘルスチェックエンドポイント。

    DuckDB + R2接続を確認し、システムの状態を返します。

    Returns:
        dict: システム状態

    Example Response:
        {
            "status": "ok",
            "duckdb": "connected",
            "r2": "accessible",
            "total_plays": 12345
        }
    """
    try:
        # DuckDB + R2接続のテスト
        parquet_path = get_parquet_path(config.r2.bucket_name, config.r2.events_path)

        with db_connection as conn:
            count = conn.execute(
                "SELECT COUNT(*) FROM read_parquet(?)", [parquet_path]
            ).fetchone()[0]

        return {
            "status": "ok",
            "duckdb": "connected",
            "r2": "accessible",
            "total_plays": count,
        }

    except Exception as e:
        logger.exception("Health check failed")
        return {"status": "error", "error": str(e)}
