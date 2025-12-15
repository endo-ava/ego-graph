"""Spotifyデータコレクター。

Spotify Web APIに接続し、以下を収集します:
- 最近再生したトラック(視聴履歴)
- ユーザーのプレイリストとトラック一覧
"""

import logging
from typing import Any

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
        redirect_uri: str = "http://127.0.0.1:8888/callback",
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
    def get_recently_played(
        self,
        limit: int = RECENTLY_PLAYED_LIMIT,
        after: int | None = None
    ) -> list[dict[str, Any]]:
        """最近再生したトラックを取得します。

        Args:
            limit: 取得するトラックの最大数(最大50)
            after: この時刻以降の再生履歴のみ取得するUnixミリ秒タイムスタンプ。
                   Noneの場合は全件取得（デフォルト）。

        Returns:
            メタデータを含むトラック辞書のリスト

        Raises:
            spotipy.SpotifyException: リトライ後にAPI呼び出しが失敗した場合

        Note:
            afterパラメータを使用すると、指定したタイムスタンプより後の
            再生履歴のみが返されます（タイムスタンプ自体は含まれません）。
        """
        # ログ出力: 増分取得か全件取得かを明示
        if after is not None:
            logger.info(
                f"Fetching recently played tracks incrementally "
                f"(limit={limit}, after={after} ms)"
            )
        else:
            logger.info(f"Fetching recently played tracks (limit={limit})")

        try:
            # afterパラメータを条件付きで渡す
            api_params = {"limit": limit}
            if after is not None:
                api_params["after"] = after

            results = self.sp.current_user_recently_played(**api_params)
            items = results.get("items", [])

            # 結果の詳細ログ
            if after is not None and len(items) == 0:
                logger.info(
                    "No new tracks found since last fetch. Database is up to date."
                )
            else:
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
    def get_user_playlists(self, limit: int = PLAYLISTS_LIMIT) -> list[dict[str, Any]]:
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
    def get_playlist_tracks(self, playlist_id: str) -> list[dict[str, Any]]:
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
    ) -> list[dict[str, Any]]:
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

    @retry(
        retry=retry_if_exception_type((spotipy.SpotifyException,)),
        stop=stop_after_attempt(MAX_RETRIES),
        wait=wait_exponential(multiplier=RETRY_BACKOFF_FACTOR, min=2, max=10),
    )
    def get_audio_features(self, track_ids: list[str]) -> list[dict[str, Any]]:
        """複数のトラックのAudio Features(特徴量)を取得します。

        Args:
            track_ids: SpotifyトラックIDのリスト(最大100個)

        Returns:
            Audio Featuresオブジェクトのリスト。
            IDに対応する特徴量が見つからない場合はNoneが含まれる可能性があります。
            
        Note:
            取得できる特徴量:
            - danceability: 踊りやすさ
            - energy: エネルギッシュさ
            - valence: ポジティブ度(感情)
            - tempo: テンポ(BPM)
            - acousticness: アコースティック感
            etc.
        """
        if not track_ids:
            return []

        logger.debug(f"Fetching audio features for {len(track_ids)} tracks")
        
        # Spotify APIは一度に最大100IDまで
        chunk_size = 100
        all_features = []

        try:
            for i in range(0, len(track_ids), chunk_size):
                chunk = track_ids[i : i + chunk_size]
                features = self.sp.audio_features(tracks=chunk)
                if features:
                    all_features.extend(features)
            
            logger.info(f"Successfully fetched audio features for {len(all_features)} tracks")
            return all_features

        except spotipy.SpotifyException as e:
            if e.http_status == 403:
                logger.warning("Audio Features endpoint is restricted (403). This feature is deprecated for new Spotify Apps created after Nov 2024.")
                return []
            
            logger.exception("Failed to fetch audio features")
            raise

