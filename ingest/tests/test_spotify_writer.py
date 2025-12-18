"""DuckDB ライターのテスト。"""

from ingest.spotify.writer import SpotifyDuckDBWriter

from .fixtures.spotify_responses import get_mock_recently_played


def test_upsert_plays(temp_db):
    """再生履歴の挿入をテストする。"""
    writer = SpotifyDuckDBWriter(temp_db)
    mock_data = get_mock_recently_played(2)

    count = writer.upsert_plays(mock_data["items"])

    assert count == 2

    # データを検証
    result = temp_db.execute("SELECT COUNT(*) FROM raw.spotify_plays").fetchone()
    assert result[0] == 2

    # べき等性をテスト
    count2 = writer.upsert_plays(mock_data["items"])
    assert count2 == 2

    result = temp_db.execute("SELECT COUNT(*) FROM raw.spotify_plays").fetchone()
    assert result[0] == 2  # 依然として2（4ではない）


def test_upsert_plays_empty(temp_db):
    """空の入力の処理をテストする。"""
    writer = SpotifyDuckDBWriter(temp_db)

    count = writer.upsert_plays([])
    assert count == 0


def test_upsert_tracks(temp_db):
    """楽曲マスタデータの挿入をテストする。"""
    writer = SpotifyDuckDBWriter(temp_db)
    mock_data = get_mock_recently_played(2)

    count = writer.upsert_tracks(mock_data["items"])

    assert count == 2

    # データを検証
    result = temp_db.execute("SELECT COUNT(*) FROM mart.spotify_tracks").fetchone()
    assert result[0] == 2


def test_upsert_tracks_deduplication(temp_db):
    """同じ楽曲が複数回現れた際の重複除去をテストする。"""
    writer = SpotifyDuckDBWriter(temp_db)
    mock_data = get_mock_recently_played(2)

    # 重複除去をテストするため、最初の楽曲を2回追加
    duplicate_items = [mock_data["items"][0], mock_data["items"][0]]

    count = writer.upsert_tracks(duplicate_items)

    # 1つのユニーク楽曲のみ挿入されるべき
    assert count == 1

    result = temp_db.execute("SELECT COUNT(*) FROM mart.spotify_tracks").fetchone()
    assert result[0] == 1


def test_get_stats(temp_db):
    """統計情報の取得をテストする。"""
    writer = SpotifyDuckDBWriter(temp_db)
    mock_data = get_mock_recently_played(2)

    writer.upsert_plays(mock_data["items"])
    writer.upsert_tracks(mock_data["items"])

    stats = writer.get_stats()

    assert stats["total_plays"] == 2
    assert stats["total_tracks"] == 2
    assert stats["latest_play"] is not None


def test_play_id_generation(temp_db):
    """play_id が正しく生成されることをテストする。"""
    writer = SpotifyDuckDBWriter(temp_db)
    mock_data = get_mock_recently_played(1)

    writer.upsert_plays(mock_data["items"])

    # play_id のフォーマットを確認
    result = temp_db.execute("SELECT play_id FROM raw.spotify_plays").fetchone()
    play_id = result[0]

    # タイムスタンプと track_id を含むべき
    assert "2025-12-14T02:30:00.000Z" in play_id
    assert "3n3Ppam7vgaVa1iaRUc9Lp" in play_id
