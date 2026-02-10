"""認証関連の機能。"""

import logging
import os
from typing import Final

logger = logging.getLogger(__name__)

# 環境変数名
ENV_API_KEY: Final = "GATEWAY_API_KEY"
ENV_LEGACY_BEARER_TOKEN: Final = "GATEWAY_BEARER_TOKEN"


class TokenVerifier:
    """Gateway API keyの検証を行うクラス。

    環境変数からトークンを読み込み、リクエストのトークンを検証する。
    """

    def __init__(self) -> None:
        """TokenVerifierを初期化する。

        Raises:
            ValueError: 認証キー環境変数が設定されていない場合
        """
        self._token = os.getenv(ENV_API_KEY) or os.getenv(ENV_LEGACY_BEARER_TOKEN)
        if not self._token:
            required_vars = f"{ENV_API_KEY} (or {ENV_LEGACY_BEARER_TOKEN})"
            raise ValueError(f"{required_vars} environment variable is required")
        if len(self._token) < 32:
            logger.warning(
                "Gateway API key is shorter than 32 bytes, this is not recommended"
            )

    def verify(self, token: str | None) -> bool:
        """トークンを検証する。

        Args:
            token: 検証するトークン文字列

        Returns:
            トークンが有効な場合はTrue、無効な場合はFalse
        """
        if not token:
            return False

        # 後方互換: "Bearer " プレフィックス付き入力を許容
        if token.startswith("Bearer "):
            token = token[7:]

        return token == self._token


# グローバルインスタンス
_verifier: TokenVerifier | None = None


def get_token_verifier() -> TokenVerifier:
    """TokenVerifierのシングルトンインスタンスを取得する。

    Returns:
        TokenVerifierインスタンス
    """
    global _verifier
    if _verifier is None:
        _verifier = TokenVerifier()
    return _verifier


def verify_gateway_token(api_key_or_authorization: str | None) -> bool:
    """Gateway APIキーを検証する。

    Args:
        api_key_or_authorization: X-API-Key または Authorization ヘッダーの値

    Returns:
        トークンが有効な場合はTrue、無効な場合はFalse
    """
    verifier = get_token_verifier()
    return verifier.verify(api_key_or_authorization)
