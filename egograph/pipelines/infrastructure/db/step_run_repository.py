"""Step run persistence."""

from __future__ import annotations

import uuid
from typing import Any

from pipelines.domain.workflow import StepRun, StepRunStatus
from pipelines.infrastructure.db._shared import (
    SQLiteRepository,
    dt_to_text,
    json_to_text,
    map_step_run,
    utc_now,
)


class StepRunRepository(SQLiteRepository):
    """step run の永続化を担う。"""

    def insert_step_run(
        self,
        *,
        run_id: str,
        step_id: str,
        step_name: str,
        sequence_no: int,
        attempt_no: int,
        command: str,
        status: StepRunStatus = StepRunStatus.QUEUED,
    ) -> StepRun:
        """step run を作成する。"""
        step_run_id = str(uuid.uuid4())
        with self._mutex, self._conn:
            self._conn.execute(
                """
                INSERT INTO step_runs (
                    step_run_id,
                    run_id,
                    step_id,
                    step_name,
                    sequence_no,
                    attempt_no,
                    command,
                    status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    step_run_id,
                    run_id,
                    step_id,
                    step_name,
                    sequence_no,
                    attempt_no,
                    command,
                    status.value,
                ),
            )
            row = self._conn.execute(
                "SELECT * FROM step_runs WHERE step_run_id = ?",
                (step_run_id,),
            ).fetchone()
        return map_step_run(row)

    def set_step_running(self, step_run_id: str) -> None:
        """step run を running に遷移させる。"""
        with self._mutex, self._conn:
            self._conn.execute(
                """
                UPDATE step_runs
                SET status = ?,
                    started_at = ?
                WHERE step_run_id = ?
                """,
                (
                    StepRunStatus.RUNNING.value,
                    dt_to_text(utc_now()),
                    step_run_id,
                ),
            )

    def update_step_result(
        self,
        *,
        step_run_id: str,
        status: StepRunStatus,
        exit_code: int | None = None,
        stdout_tail: str | None = None,
        stderr_tail: str | None = None,
        log_path: str | None = None,
        result_summary: dict[str, Any] | None = None,
    ) -> StepRun:
        """step run の終了状態を保存する。"""
        with self._mutex, self._conn:
            self._conn.execute(
                """
                UPDATE step_runs
                SET status = ?,
                    finished_at = ?,
                    exit_code = ?,
                    stdout_tail = ?,
                    stderr_tail = ?,
                    log_path = ?,
                    result_summary_json = ?
                WHERE step_run_id = ?
                """,
                (
                    status.value,
                    dt_to_text(utc_now()),
                    exit_code,
                    stdout_tail,
                    stderr_tail,
                    log_path,
                    json_to_text(result_summary),
                    step_run_id,
                ),
            )
            row = self._conn.execute(
                "SELECT * FROM step_runs WHERE step_run_id = ?",
                (step_run_id,),
            ).fetchone()
        return map_step_run(row)

    def list_step_runs(self, run_id: str) -> list[StepRun]:
        """run に紐づく step run を順序付きで返す。"""
        with self._mutex:
            rows = self._conn.execute(
                """
                SELECT *
                FROM step_runs
                WHERE run_id = ?
                ORDER BY sequence_no ASC, attempt_no ASC
                """,
                (run_id,),
            ).fetchall()
        return [map_step_run(row) for row in rows]
