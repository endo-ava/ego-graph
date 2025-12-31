"""Spotify統計取得のユースケース。"""

from datetime import date

from backend.database.connection import DuckDBConnection
from backend.database.queries import (
    get_listening_stats,
    get_parquet_path,
    get_top_tracks,
)
from shared.config import R2Config


def fetch_top_tracks(
    db_connection: DuckDBConnection,
    r2_config: R2Config,
    start_date: date,
    end_date: date,
    limit: int,
) -> list[dict[str, object]]:
    """トップトラックを取得する。"""
    parquet_path = get_parquet_path(r2_config.bucket_name, r2_config.events_path)
    with db_connection as conn:
        return get_top_tracks(conn, parquet_path, start_date, end_date, limit)


def fetch_listening_stats(
    db_connection: DuckDBConnection,
    r2_config: R2Config,
    start_date: date,
    end_date: date,
    granularity: str,
) -> list[dict[str, object]]:
    """視聴統計を取得する。"""
    parquet_path = get_parquet_path(r2_config.bucket_name, r2_config.events_path)
    with db_connection as conn:
        return get_listening_stats(
            conn, parquet_path, start_date, end_date, granularity
        )
