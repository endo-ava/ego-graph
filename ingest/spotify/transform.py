"""Spotify生データを分析用スキーマに変換するモジュール。"""

from typing import Any
from uuid import uuid4


def transform_plays_to_events(items: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Spotifyの最近の再生履歴(Raw)を分析用イベント形式に変換する。

    Args:
        items: Spotify API (recently_played) の items リスト

    Returns:
        フラット化されたイベントデータのリスト
    """
    events = []
    for item in items:
        track = item.get("track", {})
        if not track:
            continue

        # played_at + track_id から決定的な play_id を生成
        played_at_str = item.get("played_at", "")
        track_id = track.get("id", "")
        # played_at がない場合はスキップするか UUID を生成
        play_id = (
            f"{played_at_str}_{track_id}"
            if (played_at_str and track_id)
            else str(uuid4())
        )

        event = {
            "play_id": play_id,
            "played_at_utc": played_at_str,
            "track_id": track_id,
            "track_name": track.get("name", "Unknown"),
            "artist_ids": [a.get("id") for a in track.get("artists", [])],
            "artist_names": [a.get("name") for a in track.get("artists", [])],
            "album_id": track.get("album", {}).get("id"),
            "album_name": track.get("album", {}).get("name"),
            "ms_played": track.get("duration_ms"),
            "context_type": item.get("context", {}).get("type")
            if item.get("context")
            else None,
            "popularity": track.get("popularity"),
            "explicit": track.get("explicit"),
        }
        events.append(event)

    return events
