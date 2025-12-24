"""Tests for Last.fm data transformation."""

from datetime import datetime, timezone
import pytest
from ingest.lastfm.transform import transform_track_info, transform_artist_info


def test_transform_track_info():
    """生の Last.fm トラック情報の変換をテストする。"""
    # Arrange: 生データの準備
    raw_info = {
        "track_name": "Test Track",
        "artist_name": "Test Artist",
        "album_name": "Test Album",
        "playcount": 100,
        "listeners": 50,
        "duration_ms": 200000,
        "tags": ["Rock", "Pop"],
        "url": "http://last.fm/test",
        "mbid": "mbid-123",
    }

    # Act: 変換を実行
    transformed = transform_track_info(raw_info)

    # Assert: 変換後の各フィールドを検証
    assert transformed["track_name"] == "Test Track"
    assert transformed["artist_name"] == "Test Artist"
    assert transformed["album_name"] == "Test Album"
    assert transformed["playcount"] == 100
    assert transformed["listeners"] == 50
    assert transformed["duration_ms"] == 200000
    assert transformed["tags"] == ["Rock", "Pop"]
    assert transformed["url"] == "http://last.fm/test"
    assert transformed["mbid"] == "mbid-123"
    assert isinstance(transformed["fetched_at"], datetime)
    assert transformed["fetched_at"].tzinfo == timezone.utc


def test_transform_track_info_missing_fields():
    """欠落しているフィールドがある場合の変換をテストする。"""
    # Arrange: 必要最小限のデータを準備
    raw_info = {"track_name": "Test Track", "artist_name": "Test Artist"}

    # Act: 変換を実行
    transformed = transform_track_info(raw_info)

    # Assert: 欠落フィールドが適切に処理（None または空リスト）されていることを検証
    assert transformed["track_name"] == "Test Track"
    assert transformed["artist_name"] == "Test Artist"
    assert transformed["album_name"] is None
    assert transformed["playcount"] is None
    assert transformed["tags"] == []


def test_transform_artist_info():
    """生の Last.fm アーティスト情報の変換をテストする。"""
    # Arrange: 生データの準備
    raw_info = {
        "artist_name": "Test Artist",
        "playcount": 500,
        "listeners": 200,
        "tags": ["Jazz"],
        "bio_summary": "Cool jazz artist.",
        "url": "http://last.fm/artist",
        "mbid": "mbid-456",
    }

    # Act: 変換を実行
    transformed = transform_artist_info(raw_info)

    # Assert: 変換後の各フィールドを検証
    assert transformed["artist_name"] == "Test Artist"
    assert transformed["playcount"] == 500
    assert transformed["listeners"] == 200
    assert transformed["tags"] == ["Jazz"]
    assert transformed["bio_summary"] == "Cool jazz artist."
    assert transformed["url"] == "http://last.fm/artist"
    assert transformed["mbid"] == "mbid-456"
    assert isinstance(transformed["fetched_at"], datetime)
    assert transformed["fetched_at"].tzinfo == timezone.utc
