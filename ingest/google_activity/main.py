"""YouTube視聴履歴 → R2 (Parquet Data Lake) データ取り込みパイプライン。"""

import asyncio
import json
import logging
import os
import sys
from datetime import datetime, timedelta, timezone

from ingest.google_activity import transform as google_transform
from ingest.google_activity.config import AccountConfig
from ingest.google_activity.pipeline import run_all_accounts_pipeline
from ingest.google_activity.storage import YouTubeStorage
from ingest.settings import IngestSettings
from shared import log_execution_time

logger = logging.getLogger(__name__)


@log_execution_time
def main():
    """メイン Ingestion パイプライン実行処理。"""
    logger.info("=" * 60)
    logger.info("EgoGraph YouTube Activity Ingestion Pipeline (Parquet)")
    logger.info(f"Started at: {datetime.now(timezone.utc).isoformat()}")
    logger.info("=" * 60)

    try:
        config = IngestSettings.load()

        # Google Activityアカウント設定の構築
        # TODO: 複数アカウント対応のために環境変数から読み込む
        accounts = _load_google_accounts()

        # R2 Storage初期化
        r2_config = config.duckdb.r2 if config.duckdb else None
        if not r2_config:
            raise ValueError("R2 config is required for YouTube Activity pipeline")

        storage = YouTubeStorage(
            endpoint_url=r2_config.endpoint_url,
            access_key_id=r2_config.access_key_id,
            secret_access_key=r2_config.secret_access_key,
            bucket_name=r2_config.bucket_name,
            raw_path=r2_config.raw_path,
            events_path=r2_config.events_path,
            master_path=r2_config.master_path,
        )

        # デフォルト設定
        # 過去1ヶ月分の視聴履歴を取得
        after_timestamp = datetime.now(timezone.utc) - timedelta(days=30)
        max_items = 1000

        # パイプライン実行
        results = asyncio.run(
            run_all_accounts_pipeline(
                accounts=accounts,
                storage=storage,
                transform=google_transform,
                after_timestamp=after_timestamp,
                max_items=max_items,
            )
        )

        # 結果の集計
        success_count = sum(1 for r in results.values() if r.success)
        total_count = len(results)

        if success_count == total_count:
            logger.info(
                "All accounts pipeline succeeded (%d/%d)", success_count, total_count
            )
        else:
            logger.warning(
                "Some accounts failed: %d/%d succeeded", success_count, total_count
            )

        # 失敗したアカウントがある場合はエラー終了
        if success_count < total_count:
            sys.exit(1)

    except Exception:
        logger.exception("Pipeline failed")
        sys.exit(1)


def _load_google_accounts() -> list[AccountConfig]:
    """環境変数からGoogleアカウント設定を読み込む。

    Returns:
        AccountConfigのリスト

    環境変数:
        GOOGLE_ACCOUNT_ID_1: アカウントID (例: account1)
        GOOGLE_COOKIES_1: Cookie文字列 (JSON形式またはkey=value,key=value形式)
        GOOGLE_API_KEY_1: YouTube APIキー

    Note:
        現在は単一アカウントのサンプル実装
        将来的には複数アカウントに対応可能
    """
    accounts = []

    # アカウント1（必須）
    account_id = os.getenv("GOOGLE_ACCOUNT_ID_1")
    if not account_id:
        raise ValueError("GOOGLE_ACCOUNT_ID_1 is required")

    cookies_str = os.getenv("GOOGLE_COOKIES_1")
    if not cookies_str:
        raise ValueError("GOOGLE_COOKIES_1 is required")

    # Cookieパース
    try:
        cookies = json.loads(cookies_str) if cookies_str.startswith("{") else {}
        if not cookies:
            # key=value,key=value形式のパース
            for pair in cookies_str.split(","):
                if "=" in pair:
                    key, value = pair.strip().split("=", 1)
                    cookies[key.strip()] = value.strip()
    except Exception as e:
        raise ValueError(f"Failed to parse GOOGLE_COOKIES_1: {e}") from e

    if not cookies:
        raise ValueError("GOOGLE_COOKIES_1 must contain valid cookies")

    youtube_api_key = os.getenv("GOOGLE_API_KEY_1")
    if not youtube_api_key:
        raise ValueError("GOOGLE_API_KEY_1 is required")

    account = AccountConfig(
        account_id=account_id,
        cookies=cookies,
        youtube_api_key=youtube_api_key,
    )
    accounts.append(account)

    logger.info("Loaded %d Google account(s)", len(accounts))

    return accounts


if __name__ == "__main__":
    main()
