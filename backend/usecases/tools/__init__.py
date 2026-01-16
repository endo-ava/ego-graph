"""Backend tools layer.

MCP風のツール設計を採用し、LLMエージェントがDuckDBデータに
アクセスできるようにします。
"""

from backend.domain.tools import Tool
from backend.usecases.tools.base import ToolBase
from backend.usecases.tools.registry import ToolRegistry
from backend.usecases.tools.spotify import GetListeningStatsTool, GetTopTracksTool

__all__ = [
    "Tool",
    "ToolBase",
    "ToolRegistry",
    "GetTopTracksTool",
    "GetListeningStatsTool",
]
