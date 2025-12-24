"""Direct data access API endpoints.

LLMを介さず、直接データを取得するための汎用REST APIエンドポイントです。
ダッシュボードやデータ可視化などの用途に最適です。
"""

import logging
from datetime import date

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel

from backend.api.deps import get_config, get_db_connection, verify_api_key
from backend.config import BackendConfig
from backend.database.connection import DuckDBConnection
from backend.database.queries import (
    get_listening_stats,
    get_parquet_path,
    get_top_tracks,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/v1/data/spotify", tags=["data"])


class TopTrackResponse(BaseModel):
    """トップトラックのレスポンス。"""

    track_name: str
    artist: str
    play_count: int
    total_minutes: float


class ListeningStatsResponse(BaseModel):
    """視聴統計のレスポンス。"""

    period: str
    total_ms: int
    track_count: int
    unique_tracks: int


@router.get("/stats/top-tracks", response_model=list[TopTrackResponse])
async def get_top_tracks_endpoint(
    start_date: date = Query(..., description="開始日（YYYY-MM-DD）"),
    end_date: date = Query(..., description="終了日（YYYY-MM-DD）"),
    limit: int = Query(10, ge=1, le=100, description="取得する曲数"),
    db_connection: DuckDBConnection = Depends(get_db_connection),
    config: BackendConfig = Depends(get_config),
    _: None = Depends(verify_api_key),
):
    """指定期間で最も再生された曲を取得します。

    Args:
        start_date: 開始日
        end_date: 終了日
        limit: 取得する曲数（1-100）

    Returns:
        トップトラックのリスト

    Example:
        GET /v1/data/spotify/stats/top-tracks?start_date=2024-01-01&end_date=2024-01-31&limit=5
    """
    # 日付範囲の検証
    if start_date > end_date:
        from fastapi import HTTPException
        raise HTTPException(
            status_code=400,
            detail="start_date must be on or before end_date"
        )

    logger.info(f"Getting top tracks: {start_date} to {end_date}, limit={limit}")

    parquet_path = get_parquet_path(config.r2.bucket_name, config.r2.events_path)

    with db_connection as conn:
        results = get_top_tracks(conn, parquet_path, start_date, end_date, limit)

    return results


@router.get("/stats/listening", response_model=list[ListeningStatsResponse])
async def get_listening_stats_endpoint(
    start_date: date = Query(..., description="開始日（YYYY-MM-DD）"),
    end_date: date = Query(..., description="終了日（YYYY-MM-DD）"),
    granularity: str = Query("day", pattern="^(day|week|month)$", description="集計単位"),
    db_connection: DuckDBConnection = Depends(get_db_connection),
    config: BackendConfig = Depends(get_config),
    _: None = Depends(verify_api_key),
):
    """期間別の視聴統計を取得します。

    Args:
        start_date: 開始日
        end_date: 終了日
        granularity: 集計単位（"day", "week", "month"）

    Returns:
        期間別統計のリスト

    Example:
        GET /v1/data/spotify/stats/listening?start_date=2024-01-01&end_date=2024-01-31&granularity=week
    """
    # 日付範囲の検証
    if start_date > end_date:
        from fastapi import HTTPException
        raise HTTPException(
            status_code=400,
            detail="start_date must be on or before end_date"
        )

    logger.info(
        f"Getting listening stats: {start_date} to {end_date}, granularity={granularity}"
    )

    parquet_path = get_parquet_path(config.r2.bucket_name, config.r2.events_path)

    with db_connection as conn:
        results = get_listening_stats(
            conn, parquet_path, start_date, end_date, granularity
        )

    return results
