from datetime import datetime, timezone
from unittest.mock import patch

import pytest

from backend.usecases.browser_history.ingest_browser_history import (
    BrowserHistoryUseCaseError,
    build_browser_history_storage,
    ingest_browser_history,
)
from ingest.browser_history.pipeline import BrowserHistoryPipelineResult
from ingest.browser_history.schema import BrowserHistoryPayload


def _payload() -> BrowserHistoryPayload:
    return BrowserHistoryPayload.model_validate(
        {
            "sync_id": "2f4377e4-8c80-4ef4-a6bb-7f9350dbd6cf",
            "source_device": "device-1",
            "browser": "edge",
            "profile": "Default",
            "synced_at": "2026-03-22T12:00:00Z",
            "items": [],
        }
    )


def test_build_storage_from_r2_config(mock_r2_config):
    storage = build_browser_history_storage(mock_r2_config)

    assert storage.bucket_name == "test-bucket"
    assert storage.events_path == "events/"


def test_ingest_calls_pipeline_once(mock_r2_config):
    expected = BrowserHistoryPipelineResult(
        sync_id="2f4377e4-8c80-4ef4-a6bb-7f9350dbd6cf",
        accepted=0,
        raw_saved=False,
        events_saved=False,
        received_at=datetime(2026, 3, 22, 12, 0, tzinfo=timezone.utc),
    )

    with patch(
        "backend.usecases.browser_history.ingest_browser_history.run_browser_history_pipeline",
        return_value=expected,
    ) as mock_pipeline:
        result = ingest_browser_history(_payload(), mock_r2_config)

    assert result == expected
    mock_pipeline.assert_called_once()


def test_ingest_wraps_pipeline_exception(mock_r2_config):
    with patch(
        "backend.usecases.browser_history.ingest_browser_history.run_browser_history_pipeline",
        side_effect=RuntimeError("boom"),
    ):
        with pytest.raises(BrowserHistoryUseCaseError, match="boom"):
            ingest_browser_history(_payload(), mock_r2_config)
