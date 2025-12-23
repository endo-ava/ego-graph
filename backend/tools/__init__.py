"""Backend tools layer.

MCP風のツール設計を採用し、LLMエージェントがDuckDBデータに
アクセスできるようにします。
"""

from backend.tools.base import Tool, ToolBase
from backend.tools.registry import ToolRegistry
from backend.tools.spotify import GetListeningStatsTool, GetTopTracksTool

__all__ = [
    "Tool",
    "ToolBase",
    "ToolRegistry",
    "GetTopTracksTool",
    "GetListeningStatsTool",
]
