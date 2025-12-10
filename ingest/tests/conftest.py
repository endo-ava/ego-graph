"""Pytest configuration and shared fixtures."""

import pytest
from datetime import datetime
from typing import Dict, Any, List


@pytest.fixture
def mock_spotify_track() -> Dict[str, Any]:
    """Mock Spotify recently played track item."""
    return {
        "track": {
            "id": "track123",
            "name": "Test Track",
            "artists": [
                {"name": "Test Artist 1"},
                {"name": "Test Artist 2"},
            ],
            "album": {
                "name": "Test Album",
            },
            "duration_ms": 180000,
            "explicit": False,
            "external_urls": {
                "spotify": "https://open.spotify.com/track/track123",
            },
        },
        "played_at": "2025-12-10T12:00:00Z",
        "context": {
            "type": "playlist",
            "uri": "spotify:playlist:playlist123",
        },
    }


@pytest.fixture
def mock_spotify_playlist() -> Dict[str, Any]:
    """Mock Spotify playlist."""
    return {
        "id": "playlist123",
        "name": "Test Playlist",
        "description": "A test playlist",
        "owner": {
            "display_name": "Test User",
        },
        "public": True,
        "collaborative": False,
        "tracks": {
            "total": 2,
        },
        "external_urls": {
            "spotify": "https://open.spotify.com/playlist/playlist123",
        },
        "full_tracks": [
            {
                "track": {
                    "name": "Track 1",
                    "artists": [{"name": "Artist 1"}],
                },
                "added_at": "2025-01-01T00:00:00Z",
            },
            {
                "track": {
                    "name": "Track 2",
                    "artists": [{"name": "Artist 2"}],
                },
                "added_at": "2025-01-02T00:00:00Z",
            },
        ],
    }


@pytest.fixture
def mock_unified_data():
    """Mock unified data model instance."""
    from egograph.models import UnifiedDataModel, DataSource, DataType, SensitivityLevel

    return UnifiedDataModel(
        source=DataSource.SPOTIFY,
        type=DataType.MUSIC,
        timestamp=datetime(2025, 12, 10, 12, 0, 0),
        raw_text="Test track by Test Artist",
        metadata={
            "track_id": "track123",
            "track_name": "Test Track",
            "artists": ["Test Artist"],
        },
        sensitivity=SensitivityLevel.LOW,
        nsfw=False,
    )
