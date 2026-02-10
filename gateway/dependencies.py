"""Gateway 依存関数。

認証、設定取得などの依存関数を提供します。
"""

import logging
import secrets

from starlette.datastructures import Headers
from starlette.exceptions import HTTPException
from starlette.requests import Request

from gateway.config import GatewayConfig

logger = logging.getLogger(__name__)

# グローバル設定（1回だけロード）
_config: GatewayConfig | None = None


def get_config() -> GatewayConfig:
    """Gateway設定を取得します。

    初回呼び出し時に環境変数から設定をロードし、キャッシュします。

    Returns:
        GatewayConfig

    Raises:
        ValueError: 必須設定が不足している場合
    """
    global _config
    if _config is None:
        logger.info("Loading gateway configuration")
        _config = GatewayConfig.from_env()
    return _config


async def verify_gateway_token(
    request: Request,
) -> None:
    """Gateway APIキー認証。

    X-API-Key ヘッダーを検証します。
    timing attack 対策として secrets.compare_digest を使用します。

    Args:
        request: Starlette リクエストオブジェクト

    Raises:
        HTTPException: 認証に失敗した場合（401）
    """
    config = get_config()
    headers: Headers = request.headers
    x_api_key = headers.get("X-API-Key")

    if not x_api_key:
        logger.warning("Missing X-API-Key header")
        raise HTTPException(status_code=401, detail="Missing API key")

    # トークンの検証（timing attack 対策）
    expected_token = config.api_key.get_secret_value()

    if not secrets.compare_digest(x_api_key.strip(), expected_token):
        logger.warning("Invalid API key")
        raise HTTPException(status_code=401, detail="Invalid API key")


async def verify_webhook_secret(
    request: Request,
) -> None:
    """Webhook シークレット検証。

    X-Webhook-Secret ヘッダーを検証します。
    timing attack 対策として secrets.compare_digest を使用します。

    Args:
        request: Starlette リクエストオブジェクト

    Raises:
        HTTPException: 認証に失敗した場合（401）
    """
    config = get_config()
    headers: Headers = request.headers
    webhook_secret = headers.get("X-Webhook-Secret")

    # シークレットが存在しない場合はエラー
    if not webhook_secret:
        logger.warning("Missing X-Webhook-Secret header")
        raise HTTPException(status_code=401, detail="Missing webhook secret")

    # シークレットの検証（timing attack 対策）
    expected_secret = config.webhook_secret.get_secret_value()

    if not secrets.compare_digest(webhook_secret, expected_secret):
        logger.warning("Invalid webhook secret")
        raise HTTPException(status_code=401, detail="Invalid webhook secret")
