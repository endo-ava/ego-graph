"""Tests for Last.fm collector."""

from unittest.mock import MagicMock, patch

import pytest
import pylast
from ingest.lastfm.collector import LastFmCollector


@pytest.fixture
def mock_pylast_network():
    with patch("pylast.LastFMNetwork") as mock:
        yield mock


def test_collector_initialization(mock_pylast_network):
    """コレクターがネットワークと制限を初期化することをテストする。"""
    # Act: コレクターを初期化
    collector = LastFmCollector("key", "secret")

    # Assert: ネットワークが正しい引数で初期化され、レート制限が有効化されていることを検証
    mock_pylast_network.assert_called_with(api_key="key", api_secret="secret")
    collector.network.enable_rate_limit.assert_called_once()


def test_get_track_info_success(mock_pylast_network):
    """トラック情報の取得成功をテストする。"""
    # Arrange: モックレスポンスの設定
    collector = LastFmCollector("key", "secret")
    mock_track = MagicMock()
    mock_track.get_name.return_value = "Track Name"
    mock_track.get_artist().get_name.return_value = "Artist Name"
    mock_track.get_playcount.return_value = 100
    mock_track.get_listener_count.return_value = 50
    mock_track.get_duration.return_value = 200000
    mock_track.get_url.return_value = "http://url"
    mock_track.get_mbid.return_value = "mbid-123"

    mock_tag = MagicMock()
    mock_tag.item.get_name.return_value = "Tag1"
    mock_track.get_top_tags.return_value = [mock_tag]

    mock_album = MagicMock()
    mock_album.get_name.return_value = "Album Name"
    mock_track.get_album.return_value = mock_album

    collector.network.get_track.return_value = mock_track

    # Act: トラック情報を取得
    result = collector.get_track_info("Artist", "Track")

    # Assert: 取得結果を検証
    assert result is not None
    assert result["track_name"] == "Track Name"
    assert result["artist_name"] == "Artist Name"
    assert result["playcount"] == 100
    assert result["tags"] == ["Tag1"]
    assert result["album_name"] == "Album Name"


def test_get_track_info_fallback_success(mock_pylast_network):
    """直接の取得に失敗した際に検索にフォールバックして成功することをテストする。"""
    # Arrange: 直接取得の失敗と検索結果のモック設定
    collector = LastFmCollector("key", "secret")

    # 1. 直接取得の失敗をモック
    collector.network.get_track.side_effect = pylast.WSError(
        None, "6", "Track not found"
    )

    # 2. 検索結果をモック
    mock_search = MagicMock()
    mock_result_track = MagicMock()
    mock_result_track.get_name.return_value = "Found Track"
    mock_result_track.get_artist().get_name.return_value = "Found Artist"
    mock_result_track.get_playcount.return_value = 500
    mock_result_track.get_listener_count.return_value = 200
    mock_result_track.get_duration.return_value = 180000
    mock_result_track.get_url.return_value = "http://found"
    mock_result_track.get_mbid.return_value = "mbid-found"
    mock_result_track.get_album.return_value = None
    mock_result_track.get_top_tags.return_value = []

    mock_search.get_next_page.return_value = [mock_result_track]
    collector.network.search_for_track.return_value = mock_search

    # Act: 情報を取得
    result = collector.get_track_info("Typo Artist", "Typo Track")

    # Assert: 検索結果から正しく取得できていることを検証
    assert result is not None
    assert result["track_name"] == "Found Track"
    assert result["artist_name"] == "Found Artist"
    collector.network.search_for_track.assert_called_with("Typo Artist", "Typo Track")


def test_get_track_info_totally_not_found(mock_pylast_network):
    """直接取得も検索も失敗する場合をテストする。"""
    # Arrange: 直接取得と検索の両方が失敗する状態をモック
    collector = LastFmCollector("key", "secret")

    # 1. 直接取得の失敗
    collector.network.get_track.side_effect = pylast.WSError(None, "6", "Not found")

    # 2. 検索の失敗（空リスト）
    mock_search = MagicMock()
    mock_search.get_next_page.return_value = []
    collector.network.search_for_track.return_value = mock_search

    # Act: 情報を取得
    result = collector.get_track_info("Unknown", "Unknown")

    # Assert: 結果が None であることを検証
    assert result is None
