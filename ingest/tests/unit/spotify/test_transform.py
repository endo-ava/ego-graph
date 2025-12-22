from ingest.spotify.transform import transform_plays_to_events


def test_transform_plays_to_events_basic():
    """再生履歴からイベントへの基本的な変換をテストする。"""
    # Arrange: 生の再生履歴データの準備
    raw_items = [
        {
            "played_at": "2023-10-27T10:00:00Z",
            "track": {
                "id": "track1",
                "name": "Song A",
                "artists": [{"id": "art1", "name": "Artist A"}],
                "album": {"id": "alb1", "name": "Album A"},
                "duration_ms": 1000,
                "popularity": 50,
                "explicit": False,
            },
            "context": {"type": "playlist"},
        }
    ]

    # Act: 変換を実行
    events = transform_plays_to_events(raw_items)

    # Assert: 変換後のイベント内容を検証
    assert len(events) == 1
    event = events[0]
    assert event["play_id"] == "2023-10-27T10:00:00Z_track1"
    assert event["played_at_utc"] == "2023-10-27T10:00:00Z"
    assert event["track_id"] == "track1"
    assert event["track_name"] == "Song A"
    assert event["artist_names"] == ["Artist A"]
    assert event["context_type"] == "playlist"
    assert event["popularity"] == 50


def test_transform_plays_missing_track():
    """トラック情報が欠落している場合の処理をテストする。"""
    # Arrange: トラックが None のアイテムを準備
    raw_items = [{"played_at": "2023-10-27T10:00:00Z", "track": None}]

    # Act: 変換を実行
    events = transform_plays_to_events(raw_items)

    # Assert: 結果が空であることを検証
    assert len(events) == 0


def test_transform_plays_uuids_for_missing_ids():
    """ID が欠落している場合に UUID が生成されることをテストする。"""
    # Arrange: 楽曲名のみが指定された不完全なデータを準備
    raw_items = [
        {
            "track": {"name": "Unknown Song"}
        }
    ]

    # Act: 変換を実行
    events = transform_plays_to_events(raw_items)

    # Assert: play_id が自動生成されていることを検証
    assert len(events) == 1
    assert events[0]["play_id"]
    assert len(events[0]["play_id"]) > 0
