"""EgoGraph Backend - FastAPI application entry point.

ハイブリッドBackend: LLMエージェント + 汎用データアクセスREST API
"""

import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from backend.api import chat, data, health
from backend.config import BackendConfig

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
        description="Hybrid Backend: LLM Agent + Direct Data Access REST API",
        version="0.1.0",
        docs_url="/docs",
        redoc_url="/redoc",
    )

    # CORS設定（環境変数から読み取り）
    # CORS_ORIGINS="https://example.com,https://app.example.com" のように設定
    origins = [origin.strip() for origin in config.cors_origins.split(",")]
    app.add_middleware(
        CORSMiddleware,
        allow_origins=origins,
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


# モジュールレベルでのアプリインスタンス（プロダクション用）
# インポート時に環境変数が必要（テスト時はcreate_app(config)を使う）
try:
    app = create_app()
except (ValueError, Exception):
    # テスト環境など、環境変数がない場合は後で設定する
    app = None  # type: ignore


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
