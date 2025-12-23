"""EgoGraph Backend - FastAPI application entry point.

ハイブリッドBackend: LLMエージェント + 汎用データアクセスREST API
"""

import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from backend.api import chat, data, health
from backend.config import BackendConfig

logger = logging.getLogger(__name__)


def create_app() -> FastAPI:
    """FastAPIアプリケーションを作成します。

    Returns:
        FastAPI: 設定済みのFastAPIアプリ
    """
    app = FastAPI(
        title="EgoGraph Backend API",
        description="Hybrid Backend: LLM Agent + Direct Data Access REST API",
        version="0.1.0",
        docs_url="/docs",
        redoc_url="/redoc",
    )

    # CORS設定（クライアントアプリ対応）
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],  # TODO: 本番環境では特定のオリジンのみ許可
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # ルーターの登録
    app.include_router(health.router)
    app.include_router(data.router)
    app.include_router(chat.router)

    logger.info("EgoGraph Backend initialized successfully")

    return app


app = create_app()


if __name__ == "__main__":
    import uvicorn

    config = BackendConfig.from_env()

    logger.info(f"Starting EgoGraph Backend on {config.host}:{config.port}")

    uvicorn.run(
        "backend.main:app",
        host=config.host,
        port=config.port,
        reload=config.reload,
    )
