"""Spotify compacted parquet generation."""

import argparse
import logging
from datetime import datetime, timezone

from ingest.settings import IngestSettings
from ingest.spotify.storage import SpotifyStorage

logger = logging.getLogger(__name__)


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--year", type=int, default=None)
    parser.add_argument("--month", type=int, default=None)
    return parser.parse_args()


def main() -> None:
    """Generate monthly compacted parquet files for Spotify datasets."""
    args = _parse_args()
    now = datetime.now(timezone.utc)
    year = args.year or now.year
    month = args.month or now.month

    config = IngestSettings.load()
    if not config.duckdb or not config.duckdb.r2:
        raise ValueError("R2 configuration is required for compaction")

    r2_conf = config.duckdb.r2
    storage = SpotifyStorage(
        endpoint_url=r2_conf.endpoint_url,
        access_key_id=r2_conf.access_key_id,
        secret_access_key=r2_conf.secret_access_key.get_secret_value(),
        bucket_name=r2_conf.bucket_name,
        raw_path=r2_conf.raw_path,
        events_path=r2_conf.events_path,
        master_path=r2_conf.master_path,
        compacted_path=r2_conf.compacted_path,
    )

    storage.compact_month(
        data_domain="events",
        dataset_path="spotify/plays",
        year=year,
        month=month,
        dedupe_key="play_id",
        sort_by="played_at_utc",
    )
    storage.compact_month(
        data_domain="master",
        dataset_path="spotify/tracks",
        year=year,
        month=month,
        dedupe_key="track_id",
        sort_by="updated_at",
    )
    storage.compact_month(
        data_domain="master",
        dataset_path="spotify/artists",
        year=year,
        month=month,
        dedupe_key="artist_id",
        sort_by="updated_at",
    )
    logger.info("Spotify compaction finished for %d-%02d", year, month)


if __name__ == "__main__":
    main()
