"""Tests for unified data models."""

import pytest
from datetime import datetime
from uuid import UUID

from egograph.models import (
    UnifiedDataModel,
    DataSource,
    DataType,
    SensitivityLevel,
)


class TestUnifiedDataModel:
    """Tests for UnifiedDataModel."""

    def test_create_minimal_model(self):
        """Test creating model with minimal required fields."""
        model = UnifiedDataModel(
            source=DataSource.SPOTIFY,
            type=DataType.MUSIC,
            timestamp=datetime(2025, 12, 10),
            raw_text="Test content",
        )

        assert model.source == DataSource.SPOTIFY
        assert model.type == DataType.MUSIC
        assert isinstance(model.id, UUID)
        assert model.sensitivity == SensitivityLevel.LOW  # default
        assert model.nsfw == False  # default
        assert model.embedding is None  # default

    def test_to_dict(self):
        """Test converting model to dictionary."""
        model = UnifiedDataModel(
            source=DataSource.SPOTIFY,
            type=DataType.MUSIC,
            timestamp=datetime(2025, 12, 10, 12, 0, 0),
            raw_text="Test content",
            metadata={"key": "value"},
        )

        data = model.to_dict()

        assert isinstance(data, dict)
        assert data["source"] == "spotify"
        assert data["type"] == "music"
        assert isinstance(data["id"], str)  # UUID serialized to string
        assert isinstance(data["timestamp"], str)  # datetime serialized to ISO
        assert data["metadata"]["key"] == "value"

    def test_from_dict(self):
        """Test creating model from dictionary."""
        data = {
            "source": "spotify",
            "type": "music",
            "timestamp": "2025-12-10T12:00:00",
            "raw_text": "Test content",
            "metadata": {"key": "value"},
            "sensitivity": "low",
            "nsfw": False,
        }

        model = UnifiedDataModel.from_dict(data)

        assert model.source == DataSource.SPOTIFY
        assert model.type == DataType.MUSIC
        assert model.metadata["key"] == "value"

    def test_enum_values(self):
        """Test that enum values are serialized correctly."""
        model = UnifiedDataModel(
            source=DataSource.SPOTIFY,
            type=DataType.MUSIC,
            timestamp=datetime.now(),
            raw_text="Test",
            sensitivity=SensitivityLevel.HIGH,
        )

        data = model.to_dict()
        assert data["source"] == "spotify"  # not "DataSource.SPOTIFY"
        assert data["type"] == "music"
        assert data["sensitivity"] == "high"
