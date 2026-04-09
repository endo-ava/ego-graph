"""Workflow schedule state persistence."""

from __future__ import annotations

from datetime import datetime

from pipelines.domain.schedule import TriggerSpecType, WorkflowScheduleState
from pipelines.infrastructure.db._shared import SQLiteRepository, dt_to_text, text_to_dt


class ScheduleStateRepository(SQLiteRepository):
    """workflow schedule 状態の永続化を担う。"""

    def get_schedule_states(self) -> list[WorkflowScheduleState]:
        """全 workflow schedule の状態を返す。"""
        with self._mutex:
            rows = self._conn.execute(
                """
                SELECT
                    schedule_id,
                    workflow_id,
                    trigger_type,
                    trigger_expr,
                    timezone,
                    next_run_at,
                    last_scheduled_at
                FROM workflow_schedules
                ORDER BY schedule_id
                """
            ).fetchall()
        return [
            WorkflowScheduleState(
                schedule_id=row["schedule_id"],
                workflow_id=row["workflow_id"],
                trigger_type=TriggerSpecType(row["trigger_type"]),
                trigger_expr=row["trigger_expr"],
                timezone=row["timezone"],
                next_run_at=text_to_dt(row["next_run_at"]),
                last_scheduled_at=text_to_dt(row["last_scheduled_at"]),
            )
            for row in rows
        ]

    def update_schedule_state(
        self,
        *,
        schedule_id: str,
        next_run_at: datetime | None,
        last_scheduled_at: datetime | None,
    ) -> None:
        """schedule の次回予定・最終発火時刻を更新する。"""
        with self._mutex, self._conn:
            self._conn.execute(
                """
                UPDATE workflow_schedules
                SET next_run_at = ?,
                    last_scheduled_at = ?
                WHERE schedule_id = ?
                """,
                (
                    dt_to_text(next_run_at),
                    dt_to_text(last_scheduled_at),
                    schedule_id,
                ),
            )
