"""Last.fm データの変換ロジック。"""

from datetime import datetime, timezone
from typing import Any


def transform_track_info(info: dict[str, Any]) -> dict[str, Any]:
    """Last.fm API から取得したトラック情報を Parquet 保存用に変換します。"""
    return {
        "track_name": info.get("track_name"),
        "artist_name": info.get("artist_name"),
        "album_name": info.get("album_name"),
        "playcount": info.get("playcount"),
        "listeners": info.get("listeners"),
        "duration_ms": info.get("duration_ms"),
        "tags": info.get("tags", []),
        "url": info.get("url"),
        "mbid": info.get("mbid"),
        "fetched_at": datetime.now(timezone.utc),
    }


def transform_artist_info(info: dict[str, Any]) -> dict[str, Any]:
    """Last.fm API から取得したアーティスト情報を Parquet 保存用に変換します。"""
    return {
        "artist_name": info.get("artist_name"),
        "playcount": info.get("playcount"),
        "listeners": info.get("listeners"),
        "tags": info.get("tags", []),
        "bio_summary": info.get("bio_summary"),
        "url": info.get("url"),
        "mbid": info.get("mbid"),
        "fetched_at": datetime.now(timezone.utc),
    }
