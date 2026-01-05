"""Spotify統計取得のユースケース。"""

from datetime import date

import duckdb

from backend.database.queries import (
    get_listening_stats,
    get_parquet_path,
    get_top_tracks,
)
from shared.config import R2Config


def fetch_top_tracks(
    conn: duckdb.DuckDBPyConnection,
    r2_config: R2Config,
    start_date: date,
    end_date: date,
    limit: int,
) -> list[dict[str, object]]:
    """トップトラックを取得する。

    Args:
        conn: 開かれたDuckDB接続（呼び出し側でコンテキストマネージャーを使用して管理）
        r2_config: R2設定
        start_date: 開始日
        end_date: 終了日
        limit: 取得する曲数

    Returns:
        トップトラックのリスト
    """
    parquet_path = get_parquet_path(r2_config.bucket_name, r2_config.events_path)
    return get_top_tracks(conn, parquet_path, start_date, end_date, limit)


def fetch_listening_stats(
    conn: duckdb.DuckDBPyConnection,
    r2_config: R2Config,
    start_date: date,
    end_date: date,
    granularity: str,
) -> list[dict[str, object]]:
    """視聴統計を取得する。

    Args:
        conn: 開かれたDuckDB接続（呼び出し側でコンテキストマネージャーを使用して管理）
        r2_config: R2設定
        start_date: 開始日
        end_date: 終了日
        granularity: 集計単位

    Returns:
        期間別統計のリスト
    """
    parquet_path = get_parquet_path(r2_config.bucket_name, r2_config.events_path)
    return get_listening_stats(conn, parquet_path, start_date, end_date, granularity)
