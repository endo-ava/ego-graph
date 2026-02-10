"""認証モジュールの単体テスト。"""

from unittest.mock import patch

import pytest

import gateway.infrastructure.auth as auth_module
from gateway.infrastructure.auth import (
    TokenVerifier,
    get_token_verifier,
    verify_gateway_token,
)

# ============================================================================
# TokenVerifierテスト
# ============================================================================


class TestTokenVerifier:
    """TokenVerifierクラスのテスト。"""

    def test_init_with_valid_token(self):
        """有効なトークンで初期化できることを確認する。"""
        # Arrange
        test_token = "valid_token_32_bytes_or_more"

        # Act
        with patch("gateway.infrastructure.auth.os.getenv", return_value=test_token):
            verifier = TokenVerifier()

            # Assert
            assert verifier._token == test_token

    def test_init_with_missing_environment_variable(self):
        """環境変数が未設定の場合はエラーになることを確認する。"""
        # Arrange
        with patch("gateway.infrastructure.auth.os.getenv", return_value=None):
            # Act & Assert
            with pytest.raises(ValueError, match="GATEWAY_API_KEY"):
                TokenVerifier()

    def test_init_with_short_token_warns(self):
        """短すぎるトークンで警告が発生することを確認する。"""
        # Arrange
        short_token = "short"

        # Act
        with patch("gateway.infrastructure.auth.os.getenv", return_value=short_token):
            # Warningはログに出力されるため、エラーにはならない
            verifier = TokenVerifier()

            # Assert
            assert verifier._token == short_token


# ============================================================================
# verifyメソッドテスト
# ============================================================================


class TestTokenVerifierVerify:
    """TokenVerifier.verifyメソッドのテスト。"""

    @pytest.fixture
    def verifier(self):
        """テスト用TokenVerifier。"""
        test_token = "valid_token_32_bytes_or_more"
        with patch("gateway.infrastructure.auth.os.getenv", return_value=test_token):
            return TokenVerifier()

    def test_verify_with_valid_token(self, verifier):
        """有効なトークンで検証が成功することを確認する。"""
        # Arrange
        verifier._token = "test_token"

        # Act
        result = verifier.verify("test_token")

        # Assert
        assert result is True

    def test_verify_with_bearer_prefix(self, verifier):
        """Bearerプレフィックス付きのトークンで検証が成功することを確認する。"""
        # Arrange
        verifier._token = "test_token"

        # Act
        result = verifier.verify("Bearer test_token")

        # Assert
        assert result is True

    def test_verify_with_invalid_token(self, verifier):
        """無効なトークンで検証が失敗することを確認する。"""
        # Arrange
        verifier._token = "correct_token"

        # Act
        result = verifier.verify("wrong_token")

        # Assert
        assert result is False

    def test_verify_with_none_token(self, verifier):
        """Noneトークンで検証が失敗することを確認する。"""
        # Act
        result = verifier.verify(None)

        # Assert
        assert result is False

    def test_verify_with_empty_string(self, verifier):
        """空文字列で検証が失敗することを確認する。"""
        # Act
        result = verifier.verify("")

        # Assert
        assert result is False


# ============================================================================
# グローバル関数テスト
# ============================================================================


class TestGlobalFunctions:
    """グローバル関数のテスト。"""

    def test_get_token_verifier_singleton(self):
        """get_token_verifierがシングルトンを返すことを確認する。"""
        # Arrange
        test_token = "test_token_32_bytes_or_more"

        # Act
        with patch("gateway.infrastructure.auth.os.getenv", return_value=test_token):
            verifier1 = get_token_verifier()
            verifier2 = get_token_verifier()

        # Assert
        assert verifier1 is verifier2

    def test_verify_gateway_token_valid(self):
        """有効なトークンでverify_gateway_tokenが成功することを確認する。"""
        # Arrange
        test_token = "valid_token_32_bytes_or_more"

        # Act
        with patch("gateway.infrastructure.auth.os.getenv", return_value=test_token):
            # グローバルverifierをリセット
            auth_module._verifier = None
            result = verify_gateway_token(test_token)

        # Assert
        assert result is True

    def test_verify_gateway_token_invalid(self):
        """無効なトークンでverify_gateway_tokenが失敗することを確認する。"""
        # Arrange
        correct_token = "correct_token_32_bytes_or_more"
        wrong_token = "wrong_token_32_bytes_or_more"

        # Act
        with patch("gateway.infrastructure.auth.os.getenv", return_value=correct_token):
            # グローバルverifierをリセット
            auth_module._verifier = None
            result = verify_gateway_token(wrong_token)

        # Assert
        assert result is False
