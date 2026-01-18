"""LLM モデルのドメインモデル。

LLM モデルの情報を表現するエンティティです。
"""

from pydantic import BaseModel


class LLMModel(BaseModel):
    """LLM モデル情報エンティティ。

    Attributes:
        id: モデル ID（例: "openai/gpt-oss-120b:free"）
        name: モデルの表示名
        provider: プロバイダー名（例: "openrouter"）
        input_cost_per_1m: 入力 100万トークンあたりのコスト（USD）
        output_cost_per_1m: 出力 100万トークンあたりのコスト（USD）
        is_free: 無料モデルかどうか
    """

    id: str
    name: str
    provider: str
    input_cost_per_1m: float
    output_cost_per_1m: float
    is_free: bool
