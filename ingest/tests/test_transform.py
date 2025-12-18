from ingest.spotify.transform import transform_plays_to_events


def test_transform_plays_to_events_basic():
    # Arrange
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

    # Act
    events = transform_plays_to_events(raw_items)

    # Assert
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
    # Arrange
    raw_items = [{"played_at": "2023-10-27T10:00:00Z", "track": None}]

    # Act
    events = transform_plays_to_events(raw_items)

    # Assert
    assert len(events) == 0


def test_transform_plays_uuids_for_missing_ids():
    # Arrange
    raw_items = [
        {
            # No played_at
            "track": {"name": "Unknown Song"}
        }
    ]

    # Act
    events = transform_plays_to_events(raw_items)

    # Assert
    assert len(events) == 1
    # Check if play_id is generated (not empty)
    assert events[0]["play_id"]
    assert len(events[0]["play_id"]) > 0
