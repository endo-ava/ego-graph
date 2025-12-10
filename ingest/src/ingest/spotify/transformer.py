"""Spotifyデータトランスフォーマー。

生のSpotify APIレスポンスを統一データモデルに変換します。
"""

import logging
from datetime import datetime
from typing import Any, Dict, List

from dateutil import parser as date_parser

from egograph.models import (
    UnifiedDataModel,
    DataSource,
    DataType,
    SensitivityLevel,
)
from egograph.utils import safe_get, format_duration_ms

logger = logging.getLogger(__name__)


class SpotifyTransformer:
    """Spotify APIデータを統一スキーマに変換します。"""

    def transform_recently_played(
        self,
        items: List[Dict[str, Any]]
    ) -> List[UnifiedDataModel]:
        """最近再生したトラックを統一モデルに変換します。

        Args:
            items: Spotify APIからの最近再生したトラックのリスト

        Returns:
            UnifiedDataModelインスタンスのリスト
        """
        logger.info(f"Transforming {len(items)} recently played tracks")

        models = []
        for item in items:
            try:
                model = self._transform_track_item(item)
                models.append(model)
            except Exception as e:
                logger.warning(f"Failed to transform track item: {e}")
                continue

        logger.info(
            f"Successfully transformed {len(models)}/{len(items)} tracks"
        )
        return models

    def _transform_track_item(
        self,
        item: Dict[str, Any]
    ) -> UnifiedDataModel:
        """単一の最近再生したトラックアイテムを変換します。

        Args:
            item: Spotify APIからの最近再生したトラックアイテム

        Returns:
            UnifiedDataModelインスタンス
        """
        track = item.get("track", {})
        played_at_str = item.get("played_at", "")
        context = item.get("context", {})

        # タイムスタンプのパース
        played_at = date_parser.isoparse(played_at_str)

        # トラック情報の抽出
        track_id = track.get("id", "")
        track_name = track.get("name", "Unknown Track")
        artists = [artist.get("name", "") for artist in track.get("artists", [])]
        artist_names = ", ".join(artists) if artists else "Unknown Artist"
        album_name = safe_get(track, "album", "name", default="Unknown Album")
        duration_ms = track.get("duration_ms", 0)
        explicit = track.get("explicit", False)

        # コンテキスト情報（プレイリスト、アルバム、アーティスト）
        context_type = safe_get(context, "type", default="unknown")
        context_uri = safe_get(context, "uri", default="")

        # 検索可能なテキストを生成
        raw_text = (
            f"Listening to '{track_name}' by {artist_names} "
            f"from album '{album_name}' at {played_at.isoformat()}"
        )

        # メタデータの構築
        metadata = {
            "track_id": track_id,
            "track_name": track_name,
            "artists": artists,
            "album": album_name,
            "duration_ms": duration_ms,
            "duration_formatted": format_duration_ms(duration_ms),
            "explicit": explicit,
            "played_at": played_at.isoformat(),
            "context_type": context_type,
            "context_uri": context_uri,
        }

        # 利用可能な場合、オーディオ特徴量を追加（URLなど）
        if "external_urls" in track:
            metadata["spotify_url"] = safe_get(
                track, "external_urls", "spotify", default=""
            )

        return UnifiedDataModel(
            source=DataSource.SPOTIFY,
            type=DataType.MUSIC,
            timestamp=played_at,
            raw_text=raw_text,
            metadata=metadata,
            sensitivity=SensitivityLevel.LOW,  # Spotifyデータは低機密
            nsfw=explicit,  # ExplicitコンテンツをNSFWとしてマーク
        )

    def transform_playlists(
        self,
        playlists: List[Dict[str, Any]]
    ) -> List[UnifiedDataModel]:
        """Transform playlists to unified model.

        Args:
            playlists: List of playlist dictionaries from Spotify API

        Returns:
            List of UnifiedDataModel instances
        """
        logger.info(f"Transforming {len(playlists)} playlists")

        models = []
        for playlist in playlists:
            try:
                model = self._transform_playlist_item(playlist)
                models.append(model)
            except Exception as e:
                logger.warning(
                    f"Failed to transform playlist "
                    f"{playlist.get('name', 'unknown')}: {e}"
                )
                continue

        logger.info(
            f"Successfully transformed {len(models)}/{len(playlists)} playlists"
        )
        return models

    def transform_playlists(
        self,
        playlists: List[Dict[str, Any]]
    ) -> List[UnifiedDataModel]:
        """プレイリストを統一モデルに変換します。

        Args:
            playlists: Spotify APIからのプレイリスト辞書のリスト

        Returns:
            UnifiedDataModelインスタンスのリスト
        """
        logger.info(f"Transforming {len(playlists)} playlists")

        models = []
        for playlist in playlists:
            try:
                model = self._transform_playlist_item(playlist)
                models.append(model)
            except Exception as e:
                logger.warning(
                    f"Failed to transform playlist "
                    f"{playlist.get('name', 'unknown')}: {e}"
                )
                continue

        logger.info(
            f"Successfully transformed {len(models)}/{len(playlists)} playlists"
        )
        return models

    def _transform_playlist_item(
        self,
        playlist: Dict[str, Any]
    ) -> UnifiedDataModel:
        """単一のプレイリストアイテムを変換します。

        Args:
            playlist: Spotify APIからのプレイリスト辞書

        Returns:
            UnifiedDataModelインスタンス
        """
        # プレイリスト情報の抽出
        playlist_id = playlist.get("id", "")
        playlist_name = playlist.get("name", "Unnamed Playlist")
        description = playlist.get("description", "")
        owner = safe_get(playlist, "owner", "display_name", default="Unknown")
        public = playlist.get("public", False)
        collaborative = playlist.get("collaborative", False)
        total_tracks = safe_get(playlist, "tracks", "total", default=0)

        # 利用可能な場合、全トラックリストを取得
        full_tracks = playlist.get("full_tracks", [])
        track_list = []

        for item in full_tracks:
            track = item.get("track")
            if not track:
                continue

            track_name = track.get("name", "Unknown Track")
            artists = [
                artist.get("name", "")
                for artist in track.get("artists", [])
            ]
            added_at = item.get("added_at", "")

            track_list.append({
                "track_name": track_name,
                "artists": artists,
                "added_at": added_at,
            })

        # 検索可能なテキストを生成
        track_names_preview = ", ".join([
            f"'{t['track_name']}' by {', '.join(t['artists'])}"
            for t in track_list[:10]  # 最初の10トラック
        ])

        if len(track_list) > 10:
            track_names_preview += f", and {len(track_list) - 10} more tracks"

        raw_text = (
            f"Playlist '{playlist_name}' by {owner} with {total_tracks} tracks"
        )
        if track_names_preview:
            raw_text += f" including {track_names_preview}"
        if description:
            raw_text += f". Description: {description}"

        # メタデータの構築
        metadata = {
            "playlist_id": playlist_id,
            "playlist_name": playlist_name,
            "description": description,
            "owner": owner,
            "public": public,
            "collaborative": collaborative,
            "total_tracks": total_tracks,
            "tracks": track_list,  # 全トラックリスト
        }

        # 利用可能な場合、プレイリストURLを追加
        if "external_urls" in playlist:
            metadata["spotify_url"] = safe_get(
                playlist, "external_urls", "spotify", default=""
            )

        # 現在時刻をタイムスタンプとして使用（収集時刻）
        timestamp = datetime.utcnow()

        return UnifiedDataModel(
            source=DataSource.SPOTIFY,
            type=DataType.MUSIC,
            timestamp=timestamp,
            raw_text=raw_text,
            metadata=metadata,
            sensitivity=SensitivityLevel.LOW,
            nsfw=False,  # プレイリストはNSFWとしてマークしない
        )

    def transform_all(
        self,
        recently_played: List[Dict[str, Any]],
        playlists: List[Dict[str, Any]]
    ) -> List[UnifiedDataModel]:
        """全てのSpotifyデータを統一モデルに変換します。

        最近再生したトラックとプレイリストの両方を変換する便利なメソッドです。

        Args:
            recently_played: Spotify APIからの最近再生したトラック
            playlists: Spotify APIからのプレイリスト

        Returns:
            UnifiedDataModelインスタンスの結合リスト
        """
        logger.info("Transforming all Spotify data")

        tracks_models = self.transform_recently_played(recently_played)
        playlist_models = self.transform_playlists(playlists)

        all_models = tracks_models + playlist_models

        logger.info(
            f"Transformed total of {len(all_models)} items "
            f"({len(tracks_models)} tracks, {len(playlist_models)} playlists)"
        )

        return all_models
