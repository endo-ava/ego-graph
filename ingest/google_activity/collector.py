"""MyActivityスクレイパー。

Google MyActivityからYouTube視聴履歴を収集します。
Playwrightを使用してスクレイピングを行い、クッキー認証をサポートします。
"""

import logging
from datetime import datetime, timezone
from typing import Any
from urllib.parse import parse_qs, urlparse

from playwright.async_api import (
    Browser,
    BrowserContext,
    Page,
    async_playwright,
)
from tenacity import (
    retry,
    retry_if_not_exception_type,
    stop_after_attempt,
    wait_exponential,
)

from .config import MAX_RETRIES, MYACTIVITY_URL, RETRY_BACKOFF_FACTOR

logger = logging.getLogger(__name__)


class AuthenticationError(Exception):
    """クッキーの期限切れまたは認証エラー。"""

    pass


# 共通リトライデコレータ (AuthenticationErrorはリトライ対象外)
collector_retry = retry(
    retry=retry_if_not_exception_type(AuthenticationError),
    stop=stop_after_attempt(MAX_RETRIES),
    wait=wait_exponential(multiplier=RETRY_BACKOFF_FACTOR, min=2, max=10),
)


def _extract_video_id(video_url: str) -> str | None:
    """YouTube URLからvideo_idを抽出する。

    Args:
        video_url: YouTube動画のURL

    Returns:
        video_id（抽出できない場合はNone）
    """
    if not video_url:
        return None

    # URLパース
    parsed = urlparse(video_url)

    # クエリパラメータからvを抽出
    if parsed.hostname in ["www.youtube.com", "youtube.com", "m.youtube.com"]:
        query_params = parse_qs(parsed.query)
        return query_params.get("v", [None])[0]

    # 短縮URL形式
    if parsed.hostname == "youtu.be":
        return parsed.path.lstrip("/")

    return None


def _parse_watched_at(timestamp_str: str) -> datetime:
    """視聴日時文字列をdatetimeオブジェクトに変換する。

    Args:
        timestamp_str: 日時文字列（ISO8601形式など）

    Returns:
        datetimeオブジェクト（UTC）
    """
    # 各種形式を試す
    for fmt in [
        "%Y-%m-%dT%H:%M:%S.%fZ",  # ISO8601 with microseconds
        "%Y-%m-%dT%H:%M:%SZ",  # ISO8601 without microseconds
        "%Y-%m-%d %H:%M:%S",  # 簡易形式
        "%Y年%m月%d日 %H:%M",  # 日本語形式
    ]:
        try:
            parsed = datetime.strptime(timestamp_str, fmt)
            # タイムゾーンがない場合はUTCと仮定
            if parsed.tzinfo is None:
                parsed = parsed.replace(tzinfo=timezone.utc)
            return parsed
        except ValueError:
            continue

    # パースに失敗した場合は現在時刻を返す（エラーハンドリング用）
    logger.warning("Failed to parse timestamp: %s", timestamp_str)
    return datetime.now(timezone.utc)


class MyActivityCollector:
    """MyActivityデータコレクター。

    Google MyActivityからYouTube視聴履歴をスクレイピングします。
    Playwrightを使用し、クッキー認証とリトライロジックをサポートします。

    Attributes:
        cookies: Google認証用のクッキーデータ
        browser: Playwrightブラウザインスタンス（遅延初期化）
    """

    def __init__(self, cookies: list[dict[str, Any]]):
        """MyActivityコレクターを初期化します。

        Args:
            cookies: Google認証用クッキー [{"name": "...", "value": "..."}]
        """
        self.cookies = cookies
        self.browser: Browser | None = None
        self.context: BrowserContext | None = None
        self.page: Page | None = None
        self._playwright = async_playwright()
        logger.info("MyActivity collector initialized with %d cookies", len(cookies))

    async def _initialize_browser(self) -> None:
        """Playwrightブラウザとコンテキストを初期化します。"""
        if self.browser is None or not self.browser.is_connected():
            # self._playwrightがNoneの場合は新しいインスタンスを作成
            if self._playwright is None:
                self._playwright = async_playwright()
            playwright_instance = await self._playwright.start()
            self.browser = await playwright_instance.chromium.launch(headless=True)
            self.context = await self.browser.new_context()

            # クッキーを設定
            await self.context.add_cookies(self.cookies)

            self.page = await self.context.new_page()
            logger.info("Browser initialized with cookies")

    async def _cleanup_browser(self) -> None:
        """ブラウザリソースをクリーンアップします。"""
        if self.context:
            await self.context.close()
        if self.browser and self.browser.is_connected():
            await self.browser.close()
        if self._playwright:
            await self._playwright.stop()

        self.context = None
        self.page = None
        self.browser = None
        logger.info("Browser resources cleaned up")

    @collector_retry
    async def collect_watch_history(
        self,
        after_timestamp: datetime,
        max_items: int | None = None,
    ) -> list[dict[str, Any]]:
        """YouTube視聴履歴を収集します。

        Args:
            after_timestamp: この時刻以降の視聴履歴のみ収集
            max_items: 収集する最大アイテム数（Noneの場合は無制限）

        Returns:
            視聴履歴のリスト。各アイテムは以下のキーを含む:
            - video_id: YouTube動画ID
            - title: 動画タイトル
            - channel_name: チャンネル名
            - watched_at: 視聴日時（datetimeオブジェクト）
            - video_url: 動画URL

        Raises:
            AuthenticationError: クッキーの期限切れまたは認証エラー
        """
        logger.info(
            "Collecting watch history (after=%s, max_items=%s)",
            after_timestamp.isoformat(),
            max_items,
        )

        try:
            await self._initialize_browser()

            # MyActivityページにアクセス
            logger.info("Navigating to MyActivity page: %s", MYACTIVITY_URL)
            response = await self.page.goto(MYACTIVITY_URL, wait_until="networkidle")

            # 認証エラーのチェック
            if await self._is_authentication_failed(response):
                raise AuthenticationError(
                    "Authentication failed. Cookies may be expired or invalid."
                )

            # 視聴履歴アイテムをスクレイピング
            items = await self._scrape_watch_items(after_timestamp, max_items)

            logger.info("Successfully collected %d watch history items", len(items))
            return items

        except AuthenticationError:
            raise
        except Exception as e:
            logger.exception("Failed to collect watch history: %s", e)
            raise
        finally:
            await self._cleanup_browser()

    async def _is_authentication_failed(self, response) -> bool:
        """認証が失敗したかどうかを判定する。

        Args:
            response: ページレスポンスオブジェクト

        Returns:
            認証失敗の場合はTrue
        """
        if response is None:
            return True

        # URLまたはステータスコードで判定
        status_code = response.status
        if status_code in [401, 403]:
            return True

        # リダイレクト先で判定（ログインページへリダイレクトされた場合）
        current_url = self.page.url if self.page else ""
        if "accounts.google.com" in current_url:
            return True

        # ページの内容で判定
        page_content = await self.page.content() if self.page else ""
        if "Sign in" in page_content and "Google Account" in page_content:
            return True

        return False

    async def _scrape_watch_items(
        self,
        after_timestamp: datetime,
        max_items: int | None,
    ) -> list[dict[str, Any]]:
        """視聴履歴アイテムをスクレイピングする。

        Args:
            after_timestamp: この時刻以降の視聴履歴のみ収集
            max_items: 収集する最大アイテム数

        Returns:
            スクレイピングした視聴履歴アイテムのリスト
        """
        items: list[dict[str, Any]] = []
        scroll_count = 0
        max_scrolls = 50  # 無限ループ防止

        while scroll_count < max_scrolls:
            # 現在のページのアイテムを抽出
            page_items = await self._extract_items_from_page(after_timestamp)
            new_items = [item for item in page_items if item not in items]
            items.extend(new_items)

            logger.debug(
                "Scroll %d: Found %d new items (total: %d)",
                scroll_count + 1,
                len(new_items),
                len(items),
            )

            # 最大アイテム数に達したら終了
            if max_items is not None and len(items) >= max_items:
                items = items[:max_items]
                break

            # 新しいアイテムが見つからなければ終了
            if len(new_items) == 0:
                logger.info(
                    "No new items found on scroll %d, stopping", scroll_count + 1
                )
                break

            # スクロールしてさらにアイテムを読み込む
            await self.page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
            await self.page.wait_for_timeout(2000)  # スクロール待機

            scroll_count += 1

        logger.info("Scraped %d items after %d scrolls", len(items), scroll_count)
        return items

    async def _extract_items_from_page(
        self, after_timestamp: datetime
    ) -> list[dict[str, Any]]:
        """現在のページから視聴履歴アイテムを抽出する。

        Args:
            after_timestamp: この時刻以降のアイテムのみ抽出

        Returns:
            抽出したアイテムのリスト
        """
        # 実際のセレクタはMyActivityのHTML構造に依存
        # 注: これは実装例であり、実際のセレクタは検証が必要

        items = []

        # 視聴履歴アイテムのセレクタ（実装例）
        # 実際の構造に基づいて調整が必要
        item_elements = await self.page.query_selector_all(
            '[data-contain="watch-history-item"], .activity-item, .ytd-watch-card'
        )

        for element in item_elements:
            try:
                item = await self._parse_item_element(element, after_timestamp)
                if item:
                    items.append(item)
            except Exception as e:
                logger.warning("Failed to parse item element: %s", e)
                continue

        return items

    async def _parse_item_element(
        self, element, after_timestamp: datetime
    ) -> dict[str, Any] | None:
        """アイテム要素からデータをパースする。

        Args:
            element: Playwright ElementHandle
            after_timestamp: この時刻以降のアイテムのみ抽出

        Returns:
            パースしたアイテムデータ（条件に合わない場合はNone）
        """
        # タイトルの取得
        title_element = await element.query_selector(
            "[data-title], .title, h3, a[title]"
        )
        title = None
        if title_element:
            title = await title_element.get_attribute("title")
            if not title:
                title = await title_element.inner_text()

        # チャンネル名の取得
        channel_element = await element.query_selector(".channel-name, .byline")
        channel_name = (
            await channel_element.inner_text() if channel_element else "Unknown"
        )

        # 動画URLの取得
        link_element = await element.query_selector("a[href*='youtube.com/watch']")
        video_url = await link_element.get_attribute("href") if link_element else None

        # video_idの抽出
        video_id = _extract_video_id(video_url) if video_url else None

        # 視聴日時の取得
        time_element = await element.query_selector(".timestamp, .time, .date")
        time_text = await time_element.inner_text() if time_element else None
        watched_at = (
            _parse_watched_at(time_text) if time_text else datetime.now(timezone.utc)
        )

        # after_timestampによるフィルタリング
        if watched_at < after_timestamp:
            return None

        # 必須フィールドのチェック
        if not all([video_id, title, channel_name, watched_at, video_url]):
            logger.warning(
                "Item missing required fields: video_id=%s, title=%s",
                video_id,
                title,
            )
            return None

        return {
            "video_id": video_id,
            "title": title,
            "channel_name": channel_name,
            "watched_at": watched_at,
            "video_url": video_url,
        }
