"""SQLite repository shared helpers."""

from __future__ import annotations

import json
import sqlite3
import threading
from datetime import UTC, datetime
from typing import Any

from pipelines.domain.workflow import (
    QueuedReason,
    StepRun,
    StepRunStatus,
    TriggerType,
    WorkflowRun,
    WorkflowRunStatus,
)


def utc_now() -> datetime:
    return datetime.now(tz=UTC)


def dt_to_text(value: datetime | None) -> str | None:
    return value.isoformat() if value else None


def text_to_dt(value: str | None) -> datetime | None:
    return datetime.fromisoformat(value) if value else None


def json_to_text(value: dict[str, Any] | None) -> str | None:
    if value is None:
        return None
    return json.dumps(value, ensure_ascii=False, sort_keys=True)


def text_to_json(value: str | None) -> dict[str, Any] | None:
    return json.loads(value) if value else None


def map_run(row: sqlite3.Row) -> WorkflowRun:
    return WorkflowRun(
        run_id=row["run_id"],
        workflow_id=row["workflow_id"],
        trigger_type=TriggerType(row["trigger_type"]),
        queued_reason=QueuedReason(row["queued_reason"]),
        status=WorkflowRunStatus(row["status"]),
        scheduled_at=text_to_dt(row["scheduled_at"]),
        queued_at=text_to_dt(row["queued_at"]) or utc_now(),
        started_at=text_to_dt(row["started_at"]),
        finished_at=text_to_dt(row["finished_at"]),
        last_error_message=row["last_error_message"],
        requested_by=row["requested_by"],
        parent_run_id=row["parent_run_id"],
        result_summary=text_to_json(row["result_summary_json"]),
    )


def map_step_run(row: sqlite3.Row) -> StepRun:
    return StepRun(
        step_run_id=row["step_run_id"],
        run_id=row["run_id"],
        step_id=row["step_id"],
        step_name=row["step_name"],
        sequence_no=row["sequence_no"],
        attempt_no=row["attempt_no"],
        command=row["command"],
        status=StepRunStatus(row["status"]),
        started_at=text_to_dt(row["started_at"]),
        finished_at=text_to_dt(row["finished_at"]),
        exit_code=row["exit_code"],
        stdout_tail=row["stdout_tail"],
        stderr_tail=row["stderr_tail"],
        log_path=row["log_path"],
        result_summary=text_to_json(row["result_summary_json"]),
    )


class SQLiteRepository:
    """Shared SQLite connection and mutex holder."""

    def __init__(
        self,
        conn: sqlite3.Connection,
        *,
        mutex: threading.RLock | None = None,
    ) -> None:
        self._conn = conn
        self._mutex = mutex or threading.RLock()
