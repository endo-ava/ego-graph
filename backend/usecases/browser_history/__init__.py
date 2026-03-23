"""Browser history usecases."""

from backend.usecases.browser_history.ingest_browser_history import (
    BrowserHistoryUseCaseError,
    ingest_browser_history,
)

__all__ = ["BrowserHistoryUseCaseError", "ingest_browser_history"]
