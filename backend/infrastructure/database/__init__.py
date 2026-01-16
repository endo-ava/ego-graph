"""Infrastructure Database Layer.

DuckDB接続管理とクエリヘルパー関数を提供します。
"""

from backend.infrastructure.database.chat_connection import (
    ChatDuckDBConnection,
    create_chat_tables,
)
from backend.infrastructure.database.connection import DuckDBConnection
from backend.infrastructure.database.queries import (
    execute_query,
    get_listening_stats,
    get_parquet_path,
    get_top_tracks,
    search_tracks_by_name,
)

__all__ = [
    "DuckDBConnection",
    "ChatDuckDBConnection",
    "create_chat_tables",
    "execute_query",
    "get_parquet_path",
    "get_top_tracks",
    "get_listening_stats",
    "search_tracks_by_name",
]
