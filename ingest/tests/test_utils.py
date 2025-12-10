"""Tests for utility functions."""

import pytest
from datetime import datetime
from uuid import uuid4

from egograph.utils import (
    serialize_for_json,
    batch_items,
    format_duration_ms,
    safe_get,
)


class TestUtils:
    """Tests for utility functions."""

    def test_serialize_uuid(self):
        """Test serializing UUID."""
        uid = uuid4()
        result = serialize_for_json(uid)

        assert isinstance(result, str)
        assert str(uid) == result

    def test_serialize_datetime(self):
        """Test serializing datetime."""
        dt = datetime(2025, 12, 10, 12, 0, 0)
        result = serialize_for_json(dt)

        assert isinstance(result, str)
        assert "2025-12-10" in result

    def test_serialize_dict(self):
        """Test serializing dictionary with special types."""
        data = {
            "id": uuid4(),
            "timestamp": datetime(2025, 12, 10),
            "name": "test",
        }

        result = serialize_for_json(data)

        assert isinstance(result["id"], str)
        assert isinstance(result["timestamp"], str)
        assert result["name"] == "test"

    def test_batch_items(self):
        """Test batching items."""
        items = list(range(10))
        batches = batch_items(items, batch_size=3)

        assert len(batches) == 4  # 3 + 3 + 3 + 1
        assert batches[0] == [0, 1, 2]
        assert batches[1] == [3, 4, 5]
        assert batches[3] == [9]

    def test_batch_items_exact_size(self):
        """Test batching when items divide evenly."""
        items = list(range(9))
        batches = batch_items(items, batch_size=3)

        assert len(batches) == 3
        assert all(len(batch) == 3 for batch in batches)

    def test_format_duration_ms(self):
        """Test formatting duration."""
        assert format_duration_ms(0) == "0:00"
        assert format_duration_ms(1000) == "0:01"
        assert format_duration_ms(60000) == "1:00"
        assert format_duration_ms(125000) == "2:05"
        assert format_duration_ms(180000) == "3:00"

    def test_safe_get_simple(self):
        """Test safe_get with simple keys."""
        data = {"a": {"b": {"c": "value"}}}

        assert safe_get(data, "a", "b", "c") == "value"

    def test_safe_get_missing_key(self):
        """Test safe_get with missing key."""
        data = {"a": {"b": "value"}}

        assert safe_get(data, "a", "x", "y") is None
        assert safe_get(data, "a", "x", "y", default="default") == "default"

    def test_safe_get_none_value(self):
        """Test safe_get with None in path."""
        data = {"a": None}

        assert safe_get(data, "a", "b") is None
