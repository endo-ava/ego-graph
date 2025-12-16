"""Spotify コレクタのテスト。"""

import pytest
import responses
from tests.fixtures.spotify_responses import get_mock_recently_played

from ingest.spotify.collector import SpotifyCollector


@pytest.fixture
def spotify_collector():
    """テスト用の SpotifyCollector インスタンスを作成する。"""
    # 注: トークンリフレッシュのため初期化時に失敗する可能性があるので、
    # 実際のテストでは auth manager をモックする必要がある
    return SpotifyCollector(
        client_id="test_client_id",
        client_secret="test_client_secret",
        refresh_token="test_refresh_token",
    )


@responses.activate
def test_get_recently_played_success():
    """最近再生したトラックの取得成功をテストする。"""
    # トークンリフレッシュエンドポイントをモック
    responses.add(
        responses.POST,
        "https://accounts.spotify.com/api/token",
        json={"access_token": "mock_token", "expires_in": 3600, "token_type": "Bearer"},
        status=200,
    )

    # 最近再生したトラックエンドポイントをモック
    responses.add(
        responses.GET,
        "https://api.spotify.com/v1/me/player/recently-played",
        json=get_mock_recently_played(2),
        status=200,
    )

    collector = SpotifyCollector(
        client_id="test_client_id",
        client_secret="test_client_secret",
        refresh_token="test_refresh_token",
    )

    result = collector.get_recently_played(limit=2)

    assert len(result) == 2
    assert result[0]["track"]["name"] == "Mr. Brightside"
    assert result[1]["track"]["name"] == "Blinding Lights"


@responses.activate
def test_get_recently_played_empty():
    """空のレスポンスの処理をテストする。"""
    # トークンリフレッシュをモック
    responses.add(
        responses.POST,
        "https://accounts.spotify.com/api/token",
        json={"access_token": "mock_token", "expires_in": 3600, "token_type": "Bearer"},
        status=200,
    )

    # 空のレスポンスをモック
    responses.add(
        responses.GET,
        "https://api.spotify.com/v1/me/player/recently-played",
        json={"items": []},
        status=200,
    )

    collector = SpotifyCollector(
        client_id="test_client_id",
        client_secret="test_client_secret",
        refresh_token="test_refresh_token",
    )

    result = collector.get_recently_played()
    assert len(result) == 0


@responses.activate
def test_get_recently_played_with_after_parameter():
    """afterパラメータを使用した増分取得をテストする。"""
    from tests.fixtures.spotify_responses import (
        INCREMENTAL_TEST_TIMESTAMPS,
        get_mock_recently_played_with_timestamps,
    )

    from shared.utils import iso8601_to_unix_ms

    # トークンリフレッシュをモック
    responses.add(
        responses.POST,
        "https://accounts.spotify.com/api/token",
        json={"access_token": "mock_token", "expires_in": 3600, "token_type": "Bearer"},
        status=200,
    )

    # afterより新しいトラックのみ返す
    newer_tracks = get_mock_recently_played_with_timestamps(
        [INCREMENTAL_TEST_TIMESTAMPS["newer"], INCREMENTAL_TEST_TIMESTAMPS["newest"]]
    )

    responses.add(
        responses.GET,
        "https://api.spotify.com/v1/me/player/recently-played",
        json=newer_tracks,
        status=200,
    )

    collector = SpotifyCollector(
        client_id="test_client_id",
        client_secret="test_client_secret",
        refresh_token="test_refresh_token",
    )

    after_ms = iso8601_to_unix_ms(INCREMENTAL_TEST_TIMESTAMPS["recent"])
    result = collector.get_recently_played(after=after_ms)

    assert len(result) == 2
    assert result[0]["played_at"] == INCREMENTAL_TEST_TIMESTAMPS["newer"]
    assert result[1]["played_at"] == INCREMENTAL_TEST_TIMESTAMPS["newest"]


@responses.activate
def test_get_recently_played_incremental_no_new_data():
    """増分取得で新しいデータがない場合をテストする。"""
    from shared.utils import iso8601_to_unix_ms

    responses.add(
        responses.POST,
        "https://accounts.spotify.com/api/token",
        json={"access_token": "mock_token", "expires_in": 3600, "token_type": "Bearer"},
        status=200,
    )

    responses.add(
        responses.GET,
        "https://api.spotify.com/v1/me/player/recently-played",
        json={"items": []},
        status=200,
    )

    collector = SpotifyCollector(
        client_id="test_client_id",
        client_secret="test_client_secret",
        refresh_token="test_refresh_token",
    )

    after_ms = iso8601_to_unix_ms("2025-12-14T03:00:00.000Z")
    result = collector.get_recently_played(after=after_ms)

    assert len(result) == 0


@responses.activate
def test_get_recently_played_backward_compatible():
    """afterパラメータなしの従来の動作が保たれることをテストする。"""
    responses.add(
        responses.POST,
        "https://accounts.spotify.com/api/token",
        json={"access_token": "mock_token", "expires_in": 3600, "token_type": "Bearer"},
        status=200,
    )

    responses.add(
        responses.GET,
        "https://api.spotify.com/v1/me/player/recently-played",
        json=get_mock_recently_played(2),
        status=200,
    )

    collector = SpotifyCollector(
        client_id="test_client_id",
        client_secret="test_client_secret",
        refresh_token="test_refresh_token",
    )

    result = collector.get_recently_played()

    assert len(result) == 2
    assert result[0]["track"]["name"] == "Mr. Brightside"


@responses.activate
def test_get_audio_features_success():
    """Audio Featuresの取得成功をテストする。"""
    # トークンリフレッシュをモック
    responses.add(
        responses.POST,
        "https://accounts.spotify.com/api/token",
        json={"access_token": "mock_token", "expires_in": 3600, "token_type": "Bearer"},
        status=200,
    )

    # Audio Featuresエンドポイントをモック
    mock_features = {
        "audio_features": [
            {"id": "track1", "danceability": 0.5, "energy": 0.8, "valence": 0.3},
            {"id": "track2", "danceability": 0.7, "energy": 0.4, "valence": 0.9},
        ]
    }

    # URLパラメータにidsが含まれるため、正規表現またはquery string matchingが必要だが、
    # シンプルにパスでマッチさせてクエリパラメータは検証しない簡易実装、
    # または responses の match_querystring 機能を使う
    import re

    responses.add(
        responses.GET,
        re.compile(r"https://api.spotify.com/v1/audio-features.*"),
        json=mock_features,
        status=200,
    )

    collector = SpotifyCollector(
        client_id="test_client_id",
        client_secret="test_client_secret",
        refresh_token="test_refresh_token",
    )

    result = collector.get_audio_features(track_ids=["track1", "track2"])

    assert len(result) == 2
    assert result[0]["id"] == "track1"
    assert result[0]["danceability"] == 0.5
    assert result[1]["id"] == "track2"
    assert result[1]["valence"] == 0.9
