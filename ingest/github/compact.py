"""GitHub compacted parquet generation."""

import argparse
import logging
from datetime import datetime, timezone

from ingest.github.storage import GitHubWorklogStorage
from ingest.settings import IngestSettings

logger = logging.getLogger(__name__)


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--year", type=int, default=None)
    parser.add_argument("--month", type=int, default=None)
    return parser.parse_args()


def main() -> None:
    """Generate monthly compacted parquet files for GitHub datasets."""
    args = _parse_args()
    now = datetime.now(timezone.utc)
    year = args.year or now.year
    month = args.month or now.month

    config = IngestSettings.load()
    if not config.duckdb or not config.duckdb.r2:
        raise ValueError("R2 configuration is required for compaction")

    r2_conf = config.duckdb.r2
    storage = GitHubWorklogStorage(
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
        dataset_path="github/commits",
        year=year,
        month=month,
        dedupe_key="commit_event_id",
        sort_by="committed_at_utc",
    )
    storage.compact_month(
        dataset_path="github/pull_requests",
        year=year,
        month=month,
        dedupe_key="pr_event_id",
        sort_by="updated_at_utc",
    )
    logger.info("GitHub compaction finished for %d-%02d", year, month)


if __name__ == "__main__":
    main()
