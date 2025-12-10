"""Tests for Spotify data transformer."""

import pytest
from datetime import datetime

from src.ingest.spotify.transformer import SpotifyTransformer
from egograph.models import DataSource, DataType, SensitivityLevel


class TestSpotifyTransformer:
    """Tests for SpotifyTransformer class."""

    def test_transform_recently_played_single_track(self, mock_spotify_track):
        """Test transforming a single recently played track."""
        transformer = SpotifyTransformer()

        result = transformer.transform_recently_played([mock_spotify_track])

        assert len(result) == 1

        model = result[0]
        assert model.source == DataSource.SPOTIFY
        assert model.type == DataType.MUSIC
        assert model.sensitivity == SensitivityLevel.LOW
        assert model.nsfw == False

        # Check raw_text contains key information
        assert "Test Track" in model.raw_text
        assert "Test Artist 1" in model.raw_text
        assert "Test Album" in model.raw_text

        # Check metadata
        assert model.metadata["track_id"] == "track123"
        assert model.metadata["track_name"] == "Test Track"
        assert len(model.metadata["artists"]) == 2
        assert model.metadata["duration_ms"] == 180000
        assert "spotify_url" in model.metadata

    def test_transform_explicit_track(self, mock_spotify_track):
        """Test that explicit tracks are marked as NSFW."""
        mock_spotify_track["track"]["explicit"] = True

        transformer = SpotifyTransformer()
        result = transformer.transform_recently_played([mock_spotify_track])

        assert result[0].nsfw == True

    def test_transform_empty_list(self):
        """Test transforming empty list."""
        transformer = SpotifyTransformer()
        result = transformer.transform_recently_played([])

        assert result == []

    def test_transform_playlist(self, mock_spotify_playlist):
        """Test transforming a playlist."""
        transformer = SpotifyTransformer()

        result = transformer.transform_playlists([mock_spotify_playlist])

        assert len(result) == 1

        model = result[0]
        assert model.source == DataSource.SPOTIFY
        assert model.type == DataType.MUSIC
        assert model.sensitivity == SensitivityLevel.LOW

        # Check raw_text
        assert "Test Playlist" in model.raw_text
        assert "Test User" in model.raw_text
        assert "Track 1" in model.raw_text

        # Check metadata
        assert model.metadata["playlist_id"] == "playlist123"
        assert model.metadata["playlist_name"] == "Test Playlist"
        assert model.metadata["total_tracks"] == 2
        assert len(model.metadata["tracks"]) == 2
        assert model.metadata["public"] == True
        assert model.metadata["collaborative"] == False

    def test_transform_all(self, mock_spotify_track, mock_spotify_playlist):
        """Test transform_all combines tracks and playlists."""
        transformer = SpotifyTransformer()

        result = transformer.transform_all(
            [mock_spotify_track],
            [mock_spotify_playlist]
        )

        assert len(result) == 2

        # First should be track, second should be playlist
        assert result[0].metadata.get("track_id") is not None
        assert result[1].metadata.get("playlist_id") is not None
