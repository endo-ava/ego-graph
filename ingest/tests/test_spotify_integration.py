"""Spotify パイプラインのインテグレーションテスト。"""

import pytest
import responses
from tests.fixtures.spotify_responses import get_mock_recently_played

from ingest.spotify.collector import SpotifyCollector
from ingest.spotify.schema import SpotifySchema
from ingest.spotify.writer import SpotifyDuckDBWriter


@pytest.mark.integration
@responses.activate
def test_full_pipeline(tmp_path):
    """収集から保存までの完全なパイプラインをテストする。"""
    # セットアップ
    db_path = tmp_path / "analytics.duckdb"

    # Spotify 認証をモック
    responses.add(
        responses.POST,
        "https://accounts.spotify.com/api/token",
        json={"access_token": "mock_token", "expires_in": 3600, "token_type": "Bearer"},
        status=200,
    )

    # Spotify API をモック
    responses.add(
        responses.GET,
        "https://api.spotify.com/v1/me/player/recently-played",
        json=get_mock_recently_played(2),
        status=200,
    )

    # 1. データ収集
    collector = SpotifyCollector(
        client_id="test_id", client_secret="test_secret", refresh_token="test_token"
    )
    recently_played = collector.get_recently_played()

    assert len(recently_played) == 2

    # 2. DB 初期化
    conn = SpotifySchema.initialize_db(str(db_path))
    SpotifySchema.create_indexes(conn)

    # 3. データ書き込み
    writer = SpotifyDuckDBWriter(conn)
    plays_count = writer.upsert_plays(recently_played)
    tracks_count = writer.upsert_tracks(recently_played)

    assert plays_count == 2
    assert tracks_count == 2

    # 4. 統計情報を検証
    stats = writer.get_stats()
    assert stats["total_plays"] == 2
    assert stats["total_tracks"] == 2
    assert stats["latest_play"] is not None

    # 5. データ整合性を検証
    # 再生履歴テーブルを確認
    plays_result = conn.execute("""
        SELECT track_name, artist_names
        FROM raw.spotify_plays
        ORDER BY played_at_utc DESC
    """).fetchall()

    assert len(plays_result) == 2
    assert plays_result[0][0] == "Mr. Brightside"
    assert "The Killers" in plays_result[0][1]

    # 楽曲マスタテーブルを確認
    tracks_result = conn.execute("""
        SELECT name, duration_ms, popularity
        FROM mart.spotify_tracks
        ORDER BY popularity DESC
    """).fetchall()

    assert len(tracks_result) == 2
    assert tracks_result[0][0] == "Blinding Lights"  # 高い人気度 (92)
    assert tracks_result[0][2] == 92

    conn.close()


@pytest.mark.integration
def test_idempotent_pipeline(tmp_path):
    """パイプラインがべき等であることをテストする - 2回実行してもデータが重複しない。"""
    db_path = tmp_path / "analytics.duckdb"
    mock_data = get_mock_recently_played(2)

    # 1回目実行
    conn = SpotifySchema.initialize_db(str(db_path))
    writer = SpotifyDuckDBWriter(conn)
    writer.upsert_plays(mock_data["items"])
    writer.upsert_tracks(mock_data["items"])

    stats_1 = writer.get_stats()
    conn.close()

    # 2回目実行（同じデータ）
    conn = SpotifySchema.initialize_db(str(db_path))
    writer = SpotifyDuckDBWriter(conn)
    writer.upsert_plays(mock_data["items"])
    writer.upsert_tracks(mock_data["items"])

    stats_2 = writer.get_stats()
    conn.close()

    # 同じ件数である必要がある
    assert stats_1["total_plays"] == stats_2["total_plays"]
    assert stats_1["total_tracks"] == stats_2["total_tracks"]
    assert stats_1["total_plays"] == 2
    assert stats_1["total_tracks"] == 2


@pytest.mark.integration
@responses.activate
def test_incremental_pipeline_run(tmp_path):
    """増分取得モードでのパイプライン実行をテストする。"""
    from tests.fixtures.spotify_responses import (
        INCREMENTAL_TEST_TIMESTAMPS,
        get_mock_recently_played_with_timestamps,
    )

    from shared.utils import iso8601_to_unix_ms

    db_path = tmp_path / "analytics.duckdb"

    # === 1回目の実行 ===
    responses.add(
        responses.POST,
        "https://accounts.spotify.com/api/token",
        json={"access_token": "mock_token", "expires_in": 3600, "token_type": "Bearer"},
        status=200,
    )

    initial_data = get_mock_recently_played_with_timestamps(
        [INCREMENTAL_TEST_TIMESTAMPS["old"], INCREMENTAL_TEST_TIMESTAMPS["recent"]]
    )

    responses.add(
        responses.GET,
        "https://api.spotify.com/v1/me/player/recently-played",
        json=initial_data,
        status=200,
    )

    collector = SpotifyCollector(
        client_id="test_id", client_secret="test_secret", refresh_token="test_token"
    )
    recently_played_1 = collector.get_recently_played()

    conn = SpotifySchema.initialize_db(str(db_path))
    writer = SpotifyDuckDBWriter(conn)
    writer.upsert_plays(recently_played_1)
    writer.upsert_tracks(recently_played_1)

    stats_1 = writer.get_stats()
    assert stats_1["total_plays"] == 2
    # DuckDBはdatetimeオブジェクトを返す - タイムスタンプの先頭部分が一致することを確認
    latest_play_str = stats_1["latest_play"].isoformat()
    assert latest_play_str.startswith("2025-12-14T02:30:00")

    conn.close()

    # === 2回目の実行（増分） ===
    responses.add(
        responses.POST,
        "https://accounts.spotify.com/api/token",
        json={"access_token": "mock_token", "expires_in": 3600, "token_type": "Bearer"},
        status=200,
    )

    incremental_data = get_mock_recently_played_with_timestamps(
        [INCREMENTAL_TEST_TIMESTAMPS["newer"], INCREMENTAL_TEST_TIMESTAMPS["newest"]]
    )

    responses.add(
        responses.GET,
        "https://api.spotify.com/v1/me/player/recently-played",
        json=incremental_data,
        status=200,
    )

    conn = SpotifySchema.initialize_db(str(db_path))
    writer = SpotifyDuckDBWriter(conn)

    stats_before = writer.get_stats()
    latest_play = stats_before["latest_play"]
    # iso8601_to_unix_ms()はdatetimeオブジェクトも処理可能
    after_ms = iso8601_to_unix_ms(latest_play)

    collector_2 = SpotifyCollector(
        client_id="test_id", client_secret="test_secret", refresh_token="test_token"
    )
    recently_played_2 = collector_2.get_recently_played(after=after_ms)

    assert len(recently_played_2) == 2

    writer.upsert_plays(recently_played_2)
    writer.upsert_tracks(recently_played_2)

    stats_2 = writer.get_stats()
    assert stats_2["total_plays"] == 4
    # DuckDBはdatetimeオブジェクトを返す - タイムスタンプの先頭部分が一致することを確認
    latest_play_str_2 = stats_2["latest_play"].isoformat()
    assert latest_play_str_2.startswith("2025-12-14T03:00:00")

    conn.close()
