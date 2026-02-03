"""ツールレジストリの構築ヘルパー。"""

from backend.domain.tools.spotify.stats import GetListeningStatsTool, GetTopTracksTool
from backend.domain.tools.youtube.stats import (
    GetTopChannelsTool,
    GetWatchHistoryTool,
    GetWatchingStatsTool,
)
from backend.infrastructure.repositories import SpotifyRepository, YouTubeRepository
from backend.usecases.tools.registry import ToolRegistry
from shared.config import R2Config


def build_tool_registry(r2_config: R2Config | None) -> ToolRegistry:
    """R2設定に応じたツールレジストリを構築する。"""
    tool_registry = ToolRegistry()

    if not r2_config:
        return tool_registry

    spotify_repository = SpotifyRepository(r2_config)
    tool_registry.register(GetTopTracksTool(spotify_repository))
    tool_registry.register(GetListeningStatsTool(spotify_repository))

    youtube_repository = YouTubeRepository(r2_config)
    tool_registry.register(GetWatchHistoryTool(youtube_repository))
    tool_registry.register(GetWatchingStatsTool(youtube_repository))
    tool_registry.register(GetTopChannelsTool(youtube_repository))

    return tool_registry
