"""テスト共通設定。"""

import pytest


@pytest.fixture
def mock_env_token():
    """テスト用環境変数トークン。"""
    return "test_token_32_bytes_or_more_for_testing"
