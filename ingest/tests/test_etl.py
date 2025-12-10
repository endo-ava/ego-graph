"""Tests for ETL pipeline."""

import pytest
from datetime import datetime

from pipeline.etl import ETLPipeline
from egograph.models import UnifiedDataModel, DataSource, DataType


class TestETLPipeline:
    """Tests for ETLPipeline class."""

    def test_process_empty_list(self):
        """Test processing empty list."""
        etl = ETLPipeline()
        nodes = etl.process_documents([])

        assert nodes == []

    def test_process_single_document(self, mock_unified_data):
        """Test processing a single document."""
        etl = ETLPipeline(chunk_size=512, chunk_overlap=50)

        nodes = etl.process_documents([mock_unified_data])

        assert len(nodes) >= 1

        # Check first node
        node = nodes[0]
        assert node.text is not None
        assert len(node.text) > 0
        assert node.metadata["source"] == "spotify"
        assert node.metadata["type"] == "music"
        assert "track_id" in node.metadata

    def test_process_multiple_documents(self, mock_unified_data):
        """Test processing multiple documents."""
        etl = ETLPipeline()

        # Create multiple documents
        docs = [mock_unified_data for _ in range(5)]

        nodes = etl.process_documents(docs)

        assert len(nodes) >= 5

    def test_get_stats(self, mock_unified_data):
        """Test getting statistics about nodes."""
        etl = ETLPipeline()
        nodes = etl.process_documents([mock_unified_data])

        stats = etl.get_stats(nodes)

        assert stats["total_nodes"] == len(nodes)
        assert stats["avg_text_length"] > 0
        assert stats["total_characters"] > 0

    def test_get_stats_empty(self):
        """Test getting statistics for empty list."""
        etl = ETLPipeline()

        stats = etl.get_stats([])

        assert stats["total_nodes"] == 0
        assert stats["avg_text_length"] == 0

    def test_chunking_long_text(self):
        """Test that long text gets chunked."""
        etl = ETLPipeline(chunk_size=50, chunk_overlap=10)

        # Create document with long text
        long_text = " ".join(["word"] * 200)  # Very long text

        model = UnifiedDataModel(
            source=DataSource.SPOTIFY,
            type=DataType.MUSIC,
            timestamp=datetime.now(),
            raw_text=long_text,
        )

        nodes = etl.process_documents([model])

        # Should create multiple chunks
        assert len(nodes) > 1
