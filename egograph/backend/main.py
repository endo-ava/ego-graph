"""EgoGraph Backend - FastAPI + MCP Server.

REST API と MCP (Model Context Protocol) を単一サーバーで提供する。
MCP エンドポイントは /mcp パスにマウントされる。
"""

import logging
import secrets

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.gzip import GZipMiddleware
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import JSONResponse

from backend.api import browser_history_data, data, github, health
from backend.config import BackendConfig
from backend.mcp_server import create_mcp_server

logger = logging.getLogger(__name__)


def create_app(config: BackendConfig | None = None) -> FastAPI:
    """FastAPIアプリケーションを作成します。

    Args:
        config: Backend設定（テスト用にオーバーライド可能）

    Returns:
        FastAPI: 設定済みのFastAPIアプリ
    """
    if config is None:
        config = BackendConfig.from_env()

    app = FastAPI(
        title="EgoGraph Backend API",
        description="Direct Data Access REST API",
        version="0.1.0",
        docs_url="/docs",
        redoc_url="/redoc",
    )

    app.add_middleware(
        GZipMiddleware,
        minimum_size=1000,
        compresslevel=6,
    )

    # CORS設定（環境変数から読み取り）
    origins = [
        origin.strip() for origin in config.cors_origins.split(",") if origin.strip()
    ]

    # ワイルドカードまたは空のオリジンリストの場合は警告を出力
    if "*" in origins:
        logger.warning(
            "CORS: ワイルドカード '*' が設定されています。開発環境用です。"
            "本番環境では具体的なオリジンを指定してください。"
        )
        origins = ["*"]
    elif not origins:
        logger.warning(
            "CORS origins が設定されていません。"
            "CORSミドルウェアは空のオリジンリストで動作します。"
        )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=origins,
        allow_credentials=False,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # ルーターの登録
    app.include_router(health.router)
    app.include_router(data.router)
    app.include_router(browser_history_data.router)
    app.include_router(github.router)

    # MCP Server を /mcp パスにマウント（API Key認証付き）
    mcp = create_mcp_server(config)
    mcp_app = mcp.streamable_http_app()

    if config.api_key is not None:

        class _MCPApiKeyAuth(BaseHTTPMiddleware):
            async def dispatch(self, request: Request, call_next):
                api_key = request.headers.get("x-api-key")
                if not api_key or not secrets.compare_digest(
                    api_key, str(config.api_key.get_secret_value())
                ):
                    return JSONResponse(
                        status_code=401, content={"detail": "Invalid API key"}
                    )
                return await call_next(request)

        mcp_app.add_middleware(_MCPApiKeyAuth)

    app.mount("/mcp", mcp_app)

    logger.info("EgoGraph Backend initialized (REST + MCP)")

    return app


if __name__ == "__main__":
    import sys

    import uvicorn

    try:
        config = BackendConfig.from_env()
    except ValueError as e:
        logger.error("Configuration error: %s", e)
        logger.error(
            "Please check your .env file. Required settings:\n"
            "  - R2_ENDPOINT_URL\n"
            "  - R2_ACCESS_KEY_ID\n"
            "  - R2_SECRET_ACCESS_KEY\n"
            "  - R2_BUCKET_NAME"
        )
        sys.exit(1)

    logger.info("Starting EgoGraph Backend on %s:%s", config.host, config.port)

    if config.reload:
        uvicorn.run(
            "backend.main:create_app",
            host=config.host,
            port=config.port,
            reload=True,
            factory=True,
        )
    else:
        uvicorn.run(
            create_app(config),
            host=config.host,
            port=config.port,
            reload=False,
        )
