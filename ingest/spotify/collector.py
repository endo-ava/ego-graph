"""Spotifyデータコレクター。

Spotify Web APIに接続し、以下を収集します:
- 最近再生したトラック(視聴履歴)
- ユーザーのプレイリストとトラック一覧
"""

import logging
from typing import Any, Dict, List

import spotipy
from spotipy.oauth2 import SpotifyOAuth
from tenacity import (
    retry,
    stop_after_attempt,
    wait_exponential,
    retry_if_exception_type,
)

from .config import (
    RECENTLY_PLAYED_LIMIT,
    PLAYLISTS_LIMIT,
    MAX_RETRIES,
    RETRY_BACKOFF_FACTOR,
)

logger = logging.getLogger(__name__)


class SpotifyCollector:
    """Spotify APIデータコレクター。

    Spotify Web APIからのOAuth認証とデータ収集を処理します。
    レート制限や一時的なエラーを処理するためのリトライロジックを実装しています。
    """

    def __init__(
        self,
        client_id: str,
        client_secret: str,
        refresh_token: str,
        redirect_uri: str = "http://localhost:8888/callback",
        scope: str = "user-read-recently-played playlist-read-private playlist-read-collaborative",
    ):
        """Spotifyコレクターを初期化します。

        Args:
            client_id: SpotifyアプリのクライアントID
            client_secret: Spotifyアプリのクライアントシークレット
            refresh_token: OAuthリフレッシュトークン
            redirect_uri: OAuthリダイレクトURI
            scope: OAuthスコープ(スペース区切り)
        """
        self.client_id = client_id
        self.client_secret = client_secret
        self.refresh_token = refresh_token

        # OAuthマネージャーの設定
        self.auth_manager = SpotifyOAuth(
            client_id=client_id,
            client_secret=client_secret,
            redirect_uri=redirect_uri,
            scope=scope,
            open_browser=False,  # ヘッドレス環境ではブラウザを開かない
        )

        # アクセストークンのリフレッシュ
        try:
            self.auth_manager.refresh_access_token(refresh_token)
            logger.info("Successfully refreshed Spotify access token")
        except Exception:
            logger.exception("Failed to refresh access token")
            raise

        # Spotifyクライアントの初期化
        self.sp = spotipy.Spotify(auth_manager=self.auth_manager)
        logger.info("Spotify collector initialized")

    @retry(
        retry=retry_if_exception_type((spotipy.SpotifyException,)),
        stop=stop_after_attempt(MAX_RETRIES),
        wait=wait_exponential(multiplier=RETRY_BACKOFF_FACTOR, min=2, max=10),
    )
    def get_recently_played(self, limit: int = RECENTLY_PLAYED_LIMIT) -> List[Dict[str, Any]]:
        """最近再生したトラックを取得します。

        Args:
            limit: 取得するトラックの最大数(最大50)

        Returns:
            メタデータを含むトラック辞書のリスト

        Raises:
            spotipy.SpotifyException: リトライ後にAPI呼び出しが失敗した場合
        """
        logger.info(f"Fetching recently played tracks (limit={limit})")

        try:
            results = self.sp.current_user_recently_played(limit=limit)
            items = results.get("items", [])
            logger.info(f"Successfully fetched {len(items)} recently played tracks")
            return items
        except spotipy.SpotifyException:
            logger.exception("Failed to fetch recently played tracks")
            raise

    @retry(
        retry=retry_if_exception_type((spotipy.SpotifyException,)),
        stop=stop_after_attempt(MAX_RETRIES),
        wait=wait_exponential(multiplier=RETRY_BACKOFF_FACTOR, min=2, max=10),
    )
    def get_user_playlists(self, limit: int = PLAYLISTS_LIMIT) -> List[Dict[str, Any]]:
        """ユーザーのプレイリストを取得します。

        Args:
            limit: 1ページあたりに取得するプレイリストの最大数

        Returns:
            メタデータを含むプレイリスト辞書のリスト

        Raises:
            spotipy.SpotifyException: リトライ後にAPI呼び出しが失敗した場合
        """
        logger.info(f"Fetching user playlists (limit={limit})")

        playlists = []
        offset = 0

        try:
            while True:
                results = self.sp.current_user_playlists(limit=limit, offset=offset)
                items = results.get("items", [])

                if not items:
                    break

                playlists.extend(items)
                offset += len(items)

                # さらにプレイリストがあるか確認
                if not results.get("next"):
                    break

            logger.info(f"Successfully fetched {len(playlists)} playlists")
            return playlists

        except spotipy.SpotifyException:
            logger.exception("Failed to fetch playlists")
            raise

    @retry(
        retry=retry_if_exception_type((spotipy.SpotifyException,)),
        stop=stop_after_attempt(MAX_RETRIES),
        wait=wait_exponential(multiplier=RETRY_BACKOFF_FACTOR, min=2, max=10),
    )
    def get_playlist_tracks(self, playlist_id: str) -> List[Dict[str, Any]]:
        """プレイリストから全トラックを取得します。

        Args:
            playlist_id: SpotifyプレイリストID

        Returns:
            メタデータを含むトラック辞書のリスト

        Raises:
            spotipy.SpotifyException: リトライ後にAPI呼び出しが失敗した場合
        """
        logger.debug(f"Fetching tracks for playlist {playlist_id}")

        tracks = []
        offset = 0
        limit = 100  # Spotifyによる最大許容数

        try:
            while True:
                results = self.sp.playlist_tracks(
                    playlist_id,
                    limit=limit,
                    offset=offset
                )
                items = results.get("items", [])

                if not items:
                    break

                tracks.extend(items)
                offset += len(items)

                # さらにトラックがあるか確認
                if not results.get("next"):
                    break

            logger.debug(f"Fetched {len(tracks)} tracks from playlist {playlist_id}")
            return tracks

        except spotipy.SpotifyException:
            logger.exception("Failed to fetch playlist tracks")
            raise

    def get_playlists_with_tracks(
        self,
        limit: int = PLAYLISTS_LIMIT
    ) -> List[Dict[str, Any]]:
        """ユーザーのプレイリストと全トラックを取得します。

        これは、完全なプレイリストデータを取得するために
        get_user_playlists() と get_playlist_tracks() を組み合わせた便利なメソッドです。

        Args:
            limit: 取得するプレイリストの最大数

        Returns:
            'tracks' フィールドが入力されたプレイリスト辞書のリスト

        Raises:
            spotipy.SpotifyException: API呼び出しが失敗した場合
        """
        playlists = self.get_user_playlists(limit=limit)

        enriched_playlists = []
        for playlist in playlists:
            playlist_id = playlist.get("id")
            if not playlist_id:
                logger.warning(f"Skipping playlist without ID: {playlist.get('name')}")
                continue

            try:
                tracks = self.get_playlist_tracks(playlist_id)
                playlist["full_tracks"] = tracks
                enriched_playlists.append(playlist)
            except Exception as e:
                logger.warning(
                    f"Failed to fetch tracks for playlist {playlist.get('name')}: {e}"
                )
                # トラックなしでプレイリストを含める
                enriched_playlists.append(playlist)

        logger.info(
            f"Successfully enriched {len(enriched_playlists)} playlists with tracks"
        )
        return enriched_playlists
