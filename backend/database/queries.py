"""Spotify データ用のSQLクエリテンプレートとヘルパー関数。"""

import logging
from datetime import date
from typing import Any

import duckdb

logger = logging.getLogger(__name__)

# Parquetパスパターン
SPOTIFY_PLAYS_PATH = "s3://{bucket}/{events_path}spotify/plays/**/*.parquet"


def get_parquet_path(bucket: str, events_path: str) -> str:
    """Spotify再生履歴のS3パスパターンを生成します。

    Args:
        bucket: R2バケット名
        events_path: イベントデータのパスプレフィックス

    Returns:
        S3パスパターン（例: s3://egograph/events/spotify/plays/**/*.parquet）
    """
    return SPOTIFY_PLAYS_PATH.format(bucket=bucket, events_path=events_path)


def execute_query(
    conn: duckdb.DuckDBPyConnection, sql: str, params: list[Any] | None = None
) -> list[dict[str, Any]]:
    """SQLクエリを実行し、結果を辞書のリストとして返します。

    Args:
        conn: DuckDBコネクション
        sql: 実行するSQLクエリ
        params: SQLパラメータ（オプション）

    Returns:
        クエリ結果（辞書のリスト）

    Raises:
        duckdb.Error: SQLクエリ実行に失敗した場合
    """
    result = conn.execute(sql, params or [])
    df = result.df()
    return df.to_dict(orient="records")


def get_top_tracks(
    conn: duckdb.DuckDBPyConnection,
    parquet_path: str,
    start_date: date,
    end_date: date,
    limit: int = 10,
) -> list[dict[str, Any]]:
    """指定期間で最も再生された曲を取得します。

    Args:
        conn: DuckDBコネクション
        parquet_path: ParquetファイルのS3パス
        start_date: 開始日
        end_date: 終了日
        limit: 取得する曲数（デフォルト: 10）

    Returns:
        トップトラックのリスト（各要素は辞書）
        [
            {
                "track_name": str,
                "artist": str,
                "play_count": int,
                "total_minutes": float
            },
            ...
        ]
    """
    query = """
        SELECT
            track_name,
            CASE
                WHEN len(artist_names) >= 1 THEN artist_names[1] ELSE NULL
            END as artist,
            COUNT(*) as play_count,
            SUM(ms_played) / 60000.0 as total_minutes
        FROM read_parquet(?)
        WHERE played_at_utc::DATE BETWEEN ? AND ?
        GROUP BY track_name, artist
        ORDER BY play_count DESC
        LIMIT ?
    """
    logger.debug(f"Executing get_top_tracks: {start_date} to {end_date}, limit={limit}")
    return execute_query(conn, query, [parquet_path, start_date, end_date, limit])


def get_listening_stats(
    conn: duckdb.DuckDBPyConnection,
    parquet_path: str,
    start_date: date,
    end_date: date,
    granularity: str = "day",
) -> list[dict[str, Any]]:
    """期間別の視聴統計を取得します。

    Args:
        conn: DuckDBコネクション
        parquet_path: ParquetファイルのS3パス
        start_date: 開始日
        end_date: 終了日
        granularity: 集計単位（"day", "week", "month"）

    Returns:
        期間別統計のリスト
        [
            {
                "period": str,
                "total_ms": int,
                "track_count": int,
                "unique_tracks": int
            },
            ...
        ]

    Raises:
        ValueError: granularityが無効な場合
    """
    # 粒度に応じた期間フォーマットを選択
    date_format_map = {
        "day": "%Y-%m-%d",
        "week": "%Y-W%V",  # ISO週番号
        "month": "%Y-%m",
    }

    if granularity not in date_format_map:
        allowed = list(date_format_map.keys())
        raise ValueError(
            f"Invalid granularity: {granularity}. Must be one of {allowed}"
        )

    date_format = date_format_map[granularity]

    query = f"""
        SELECT
            strftime(played_at_utc::DATE, '{date_format}') as period,
            SUM(ms_played) as total_ms,
            COUNT(*) as track_count,
            COUNT(DISTINCT track_id) as unique_tracks
        FROM read_parquet(?)
        WHERE played_at_utc::DATE BETWEEN ? AND ?
        GROUP BY period
        ORDER BY period ASC
    """

    logger.debug(
        "Executing get_listening_stats: %s to %s, granularity=%s",
        start_date,
        end_date,
        granularity,
    )
    return execute_query(conn, query, [parquet_path, start_date, end_date])


def search_tracks_by_name(
    conn: duckdb.DuckDBPyConnection,
    parquet_path: str,
    query: str,
    limit: int = 20,
) -> list[dict[str, Any]]:
    """トラック名またはアーティスト名で検索します。

    Args:
        conn: DuckDBコネクション
        parquet_path: ParquetファイルのS3パス
        query: 検索クエリ（部分一致）
        limit: 取得する結果数（デフォルト: 20）

    Returns:
        検索結果のリスト
        [
            {
                "track_name": str,
                "artist": str,
                "play_count": int,
                "last_played": str
            },
            ...
        ]
    """
    search_pattern = f"%{query}%"
    sql = """
        SELECT
            track_name,
            CASE
                WHEN len(artist_names) >= 1 THEN artist_names[1] ELSE NULL
            END as artist,
            COUNT(*) as play_count,
            MAX(played_at_utc)::VARCHAR as last_played
        FROM read_parquet(?)
        WHERE LOWER(track_name) LIKE LOWER(?)
           OR (len(artist_names) >= 1 AND LOWER(artist_names[1]) LIKE LOWER(?))
        GROUP BY track_name, artist
        ORDER BY play_count DESC
        LIMIT ?
    """

    logger.debug(f"Searching tracks with query: {query}, limit={limit}")
    return execute_query(
        conn, sql, [parquet_path, search_pattern, search_pattern, limit]
    )
