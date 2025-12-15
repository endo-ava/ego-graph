"""共有ユーティリティ関数のテスト。"""

import pytest
from datetime import datetime, timezone
from shared.utils import iso8601_to_unix_ms


def test_iso8601_to_unix_ms_standard_format():
    """標準的なISO 8601形式の変換をテストする。"""
    iso_ts = "2025-12-14T02:30:00.000Z"
    result = iso8601_to_unix_ms(iso_ts)
    expected = 1765679400000
    assert result == expected


def test_iso8601_to_unix_ms_different_timestamps():
    """異なるタイムスタンプの変換をテストする。"""
    test_cases = [
        ("2025-01-01T00:00:00.000Z", 1735689600000),
        ("2025-12-31T23:59:59.000Z", 1767225599000),
    ]

    for iso_ts, expected_ms in test_cases:
        result = iso8601_to_unix_ms(iso_ts)
        assert result == expected_ms


def test_iso8601_to_unix_ms_with_timezone_offset():
    """タイムゾーンオフセット付きISO 8601形式をテストする。"""
    iso_ts = "2025-12-14T02:30:00.000+00:00"
    result = iso8601_to_unix_ms(iso_ts)
    expected = 1765679400000
    assert result == expected


def test_iso8601_to_unix_ms_with_datetime_object():
    """datetimeオブジェクトの変換をテストする。"""
    dt = datetime(2025, 12, 14, 2, 30, tzinfo=timezone.utc)
    result = iso8601_to_unix_ms(dt)
    expected = 1765679400000
    assert result == expected


def test_iso8601_to_unix_ms_invalid_format():
    """無効なフォーマットでValueErrorが発生することをテストする。"""
    invalid_timestamps = [
        "not-a-timestamp",
        "2025-13-01T00:00:00.000Z",
        "",
    ]

    for invalid_ts in invalid_timestamps:
        with pytest.raises(ValueError):
            iso8601_to_unix_ms(invalid_ts)
