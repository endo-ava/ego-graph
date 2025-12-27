"""Last.fm API Client wrapper.

Deprecated: Last.fm 連携は一時停止中。
"""

import logging
import time
from typing import Any

import pylast
from tenacity import retry, stop_after_attempt, wait_exponential

logger = logging.getLogger(__name__)


class LastFmCollector:
    """トラックのメタデータを取得するための Last.fm API コレクター。"""

    def __init__(self, api_key: str, api_secret: str):
        """Last.fm クライアントを初期化します。

        Args:
            api_key: Last.fm API キー
            api_secret: Last.fm API シークレット
        """
        self.network = pylast.LastFMNetwork(api_key=api_key, api_secret=api_secret)
        # pylast のレート制限を有効化 (リクエスト間の待機を行う)
        self.network.enable_rate_limit()

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=2, max=10),
        reraise=True,
    )
    def get_track_info(
        self, artist_name: str, track_name: str
    ) -> dict[str, Any] | None:
        """単一トラックのメタデータを取得します。

        Args:
            artist_name: アーティスト名
            track_name: トラック名

        Returns:
            タグ、再生回数などを含む辞書。見つからない場合は None。
        """
        try:
            track = self.network.get_track(artist_name, track_name)

            # トラックの存在確認（シンプルな属性を取得）
            # pylast は遅延取得するため、リクエストを強制する必要があります
            playcount = track.get_playcount()

            # トップタグの取得
            # TopItem(item=Tag(name, ...), weight=...) のリストが返されます
            top_tags = track.get_top_tags(limit=5)
            tags = [t.item.get_name() for t in top_tags]

            # Get Duration
            duration = track.get_duration()

            # Get Listeners
            listeners = track.get_listener_count()

            # Get Album info if available
            album = track.get_album()
            album_name = album.get_name() if album else None

            return {
                "track_name": track.get_name(),
                "artist_name": track.get_artist().get_name(),
                "album_name": album_name,
                "playcount": playcount,
                "listeners": listeners,
                "duration_ms": duration,
                "tags": tags,
                "url": track.get_url(),
                "mbid": track.get_mbid(),  # MusicBrainz ID
            }

        except pylast.WSError as e:
            if e.status == "6":  # トラックが見つからない
                logger.debug(
                    f"Track not found directly: {artist_name} - {track_name}. Retrying with search..."
                )
                return self._search_and_get_track(artist_name, track_name)
            logger.warning(f"Error fetching track {artist_name} - {track_name}: {e}")
            raise

    def _search_and_get_track(
        self, artist_name: str, track_name: str
    ) -> dict[str, Any] | None:
        """検索APIを使用してトラックを探し、最も関連性の高い結果のメタデータを取得します。"""
        try:
            search = self.network.search_for_track(artist_name, track_name)
            results = search.get_next_page()

            if not results:
                logger.debug(
                    f"Search returned no results for: {artist_name} - {track_name}"
                )
                return None

            best_match = results[0]
            logger.info(
                f"Search found match: {best_match.get_artist().get_name()} - {best_match.get_name()}"
            )

            # 見つかったトラックオブジェクトから情報を取得
            # 既存のロジックと同じフィールドを取得したいので、共通化しても良いが
            # ここではシンプルに再取得（オブジェクトはすでに有効なので即座に取得できるはず）

            return {
                "track_name": best_match.get_name(),
                "artist_name": best_match.get_artist().get_name(),
                "album_name": best_match.get_album().get_name()
                if best_match.get_album()
                else None,
                "playcount": best_match.get_playcount(),
                "listeners": best_match.get_listener_count(),
                "duration_ms": best_match.get_duration(),
                "tags": [t.item.get_name() for t in best_match.get_top_tags(limit=5)],
                "url": best_match.get_url(),
                "mbid": best_match.get_mbid(),
            }
        except Exception as e:
            logger.warning(
                f"Error during search fallback for {artist_name} - {track_name}: {e}"
            )
            return None

    def get_artist_info(self, artist_name: str) -> dict[str, Any] | None:
        """アーティストのメタデータを取得します。

        Args:
            artist_name: アーティスト名

        Returns:
            タグ、再生回数、バイオグラフィなどを含む辞書。
        """
        try:
            artist = self.network.get_artist(artist_name)

            # Force fetch
            playcount = artist.get_playcount()

            top_tags = artist.get_top_tags(limit=5)
            tags = [t.item.get_name() for t in top_tags]

            # バイオグラフィのサマリー (Wiki)
            bio = artist.get_bio_summary()

            return {
                "artist_name": artist.get_name(),
                "playcount": playcount,
                "listeners": artist.get_listener_count(),
                "tags": tags,
                "bio_summary": bio,
                "url": artist.get_url(),
                "mbid": artist.get_mbid(),
            }
        except pylast.WSError as e:
            if e.status == "6":
                logger.debug(f"Artist not found: {artist_name}")
                return None
            logger.warning(f"Error fetching artist {artist_name}: {e}")
            raise
        except Exception:
            raise
