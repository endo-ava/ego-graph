"""Backend input validation helpers."""

from datetime import date
from typing import Any


def parse_date(value: date | str, field_name: str) -> date:
    """ISO日付またはdateを正規化する。"""
    if isinstance(value, date):
        return value
    try:
        return date.fromisoformat(value)
    except ValueError as e:
        raise ValueError(f"Invalid date format for {field_name}: {e}") from e


def validate_date_range(
    start_date: date | str, end_date: date | str
) -> tuple[date, date]:
    """日付範囲を正規化し、範囲の整合性を検証する。"""
    start = parse_date(start_date, "start_date")
    end = parse_date(end_date, "end_date")
    if start > end:
        raise ValueError("start_date must be on or before end_date")
    return start, end


def validate_limit(limit: Any, *, min_value: int = 1, max_value: int = 100) -> int:
    """limitの範囲を検証する。"""
    if not isinstance(limit, int):
        raise ValueError("limit must be a positive integer")
    if limit < min_value or limit > max_value:
        raise ValueError(
            f"limit must be between {min_value} and {max_value}"
        )
    return limit


def validate_granularity(granularity: str) -> str:
    """集計粒度を検証する。"""
    allowed = {"day", "week", "month"}
    if granularity not in allowed:
        raise ValueError("granularity must be one of: day, week, month")
    return granularity
