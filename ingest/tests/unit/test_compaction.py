"""Compaction helper tests."""

import pandas as pd

from ingest.compaction import build_compacted_key, compact_records


class TestBuildCompactedKey:
    """build_compacted_key tests."""

    def test_builds_events_key(self):
        key = build_compacted_key(
            compacted_path="compacted/",
            data_domain="events",
            dataset_path="spotify/plays",
            year=2024,
            month=1,
        )

        assert key == "compacted/events/spotify/plays/year=2024/month=01/data.parquet"

    def test_builds_master_key(self):
        key = build_compacted_key(
            compacted_path="compacted/",
            data_domain="master",
            dataset_path="spotify/tracks",
            year=2024,
            month=2,
        )

        assert key == "compacted/master/spotify/tracks/year=2024/month=02/data.parquet"


class TestCompactRecords:
    """compact_records tests."""

    def test_deduplicates_by_key_keeping_latest(self):
        records = [
            {
                "track_id": "track-1",
                "name": "Song A",
                "updated_at": "2024-01-01T00:00:00Z",
            },
            {
                "track_id": "track-1",
                "name": "Song A+",
                "updated_at": "2024-01-02T00:00:00Z",
            },
            {
                "track_id": "track-2",
                "name": "Song B",
                "updated_at": "2024-01-01T00:00:00Z",
            },
        ]

        df = compact_records(
            records,
            dedupe_key="track_id",
            sort_by="updated_at",
        )

        assert isinstance(df, pd.DataFrame)
        assert len(df) == 2
        assert df.loc[df["track_id"] == "track-1", "name"].item() == "Song A+"

    def test_returns_empty_dataframe_for_empty_records(self):
        df = compact_records([], dedupe_key="track_id")

        assert df.empty
