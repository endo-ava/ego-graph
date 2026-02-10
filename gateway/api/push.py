"""プッシュ通知関連のAPIエンドポイント。

FCMトークン登録とWebhook経由のプッシュ通知送信を提供します。
"""

import logging

from starlette.exceptions import HTTPException
from starlette.requests import Request
from starlette.responses import JSONResponse
from starlette.routing import Route

from gateway.dependencies import (
    get_config,
    verify_webhook_secret,
)
from gateway.dependencies import (
    verify_gateway_token as verify_gateway_request,
)
from gateway.domain.models import TokenRegistrationRequest, WebhookPayload
from gateway.infrastructure.repositories import PushTokenRepository
from gateway.services.fcm_service import FcmService

logger = logging.getLogger(__name__)

# グローバルインスタンス
_fcm_service: FcmService | None = None
_token_repository: PushTokenRepository | None = None


def get_fcm_service() -> FcmService:
    """FCMサービスの singleton インスタンスを取得します。

    Returns:
        FcmService: FCMサービスインスタンス
    """
    global _fcm_service, _token_repository

    if _fcm_service is None:
        config = get_config()

        if _token_repository is None:
            _token_repository = PushTokenRepository()

        _fcm_service = FcmService(
            token_repository=_token_repository,
            fcm_project_id=config.fcm_project_id,
        )

    return _fcm_service


async def register_token(request: Request) -> JSONResponse:
    """FCMトークンを登録するエンドポイント。

    PUT /v1/push/token

    Request Body:
        {
            "device_token": "string",
            "platform": "android" | "ios",
            "device_name": "string" (optional)
        }

    Returns:
        200: トークン登録成功
        {
            "id": 1,
            "user_id": "default_user",
            "device_token": "...",
            "platform": "android",
            "device_name": "My Device",
            "enabled": true,
            "last_seen_at": "2025-02-08T12:00:00",
            "created_at": "2025-02-08T12:00:00"
        }

    Raises:
        HTTPException 400: リクエストボディのバリデーションエラー
    """
    await verify_gateway_request(request)

    try:
        body = await request.json()
    except Exception:
        raise HTTPException(status_code=400, detail="invalid_body: Invalid JSON")

    # バリデーション
    try:
        data = TokenRegistrationRequest.model_validate(body)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"invalid_request: {e}")

    config = get_config()
    repository = PushTokenRepository()

    # トークンを保存
    device = repository.save_token(
        user_id=config.default_user_id,
        device_token=data.device_token,
        platform=data.platform,
        device_name=data.device_name,
    )

    logger.info(
        "Registered push token: user_id=%s, platform=%s",
        config.default_user_id,
        data.platform,
    )

    return JSONResponse(
        status_code=200,
        content={
            "id": device.id,
            "user_id": device.user_id,
            "device_token": device.device_token,
            "platform": device.platform,
            "device_name": device.device_name,
            "enabled": device.enabled,
            "last_seen_at": device.last_seen_at.isoformat(),
            "created_at": device.created_at.isoformat(),
        },
    )


async def send_webhook(request: Request) -> JSONResponse:
    """Webhook経由でプッシュ通知を送信するエンドポイント。

    POST /v1/push/webhook

    Headers:
        X-Webhook-Secret: Webhook シークレット

    Request Body:
        {
            "type": "task_completed",
            "session_id": "agent-0001",
            "title": "完了",
            "body": "タスク完了"
        }

    Returns:
        200: 通知送信成功
        {
            "success_count": 1,
            "failure_count": 0,
            "invalid_tokens": []
        }

    Raises:
        HTTPException 400: リクエストボディのバリデーションエラー
    """
    await verify_webhook_secret(request)

    try:
        body = await request.json()
    except Exception:
        raise HTTPException(status_code=400, detail="invalid_body: Invalid JSON")

    # バリデーション
    try:
        payload = WebhookPayload.model_validate(body)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"invalid_payload: {e}")

    config = get_config()
    fcm_service = get_fcm_service()

    # プッシュ通知を送信
    result = await fcm_service.handle_webhook(payload, config.default_user_id)

    logger.info(
        "Webhook processed: type=%s, session_id=%s, result=%s",
        payload.type,
        payload.session_id,
        result,
    )

    return JSONResponse(
        status_code=200,
        content={
            "success_count": result["success_count"],
            "failure_count": result["failure_count"],
            "invalid_tokens": result["invalid_tokens"],
        },
    )


# ルート定義
routes = [
    Route("/v1/push/token", register_token, methods=["PUT"]),
    Route("/v1/push/webhook", send_webhook, methods=["POST"]),
]
