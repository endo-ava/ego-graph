"""ツールのドメインモデル。

LLMに提供するツールのスキーマ定義です。
"""

from typing import Any

from pydantic import BaseModel


class Tool(BaseModel):
    """ツールスキーマ(ドメインエンティティ)。

    LLMプロバイダーに渡すためのツール定義です。
    プロバイダーに依存しない抽象的なツールの概念を表現します。

    Attributes:
        name: ツール名
        description: ツールの説明(LLMが読む)
        inputSchema: 入力パラメータのJSON Schema
    """

    name: str
    description: str
    inputSchema: dict[str, Any]  # JSON Schema
