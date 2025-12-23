"""Backend database layer.

DuckDB接続管理とクエリヘルパー関数を提供します。
"""

from backend.database.connection import DuckDBConnection
from backend.database.queries import (
    execute_query,
    get_listening_stats,
    get_parquet_path,
    get_top_tracks,
)

__all__ = [
    "DuckDBConnection",
    "execute_query",
    "get_parquet_path",
    "get_top_tracks",
    "get_listening_stats",
]
