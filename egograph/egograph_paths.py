"""Centralized runtime path definitions for the repository layout.

The deployment layout assumes:

<app-root>/
├── data/
└── repo/

This module lives under ``repo/egograph`` and resolves the sibling ``data/``
directory without hard-coding an absolute installation path.
"""

from pathlib import Path

PACKAGE_ROOT = Path(__file__).resolve().parent
REPO_ROOT = PACKAGE_ROOT.parent
APP_ROOT = REPO_ROOT.parent
DATA_ROOT = APP_ROOT / "data"

BACKEND_DATA_DIR = DATA_ROOT / "backend"
CHAT_SQLITE_PATH = BACKEND_DATA_DIR / "chat.sqlite"

PIPELINES_DATA_DIR = DATA_ROOT / "pipelines"
PIPELINES_STATE_DB_PATH = PIPELINES_DATA_DIR / "state.sqlite3"
PIPELINES_LOGS_DIR = PIPELINES_DATA_DIR / "logs"

PARQUET_DATA_DIR = DATA_ROOT / "parquet"

LEGACY_DATA_DIR = DATA_ROOT / "legacy"
LEGACY_CHAT_DUCKDB_PATH = LEGACY_DATA_DIR / "chat.duckdb"

ANALYTICS_DUCKDB_PATH = DATA_ROOT / "analytics.duckdb"
