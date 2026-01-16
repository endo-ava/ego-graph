"""LLMモデル情報の管理。"""

from pydantic import BaseModel


class LLMModel(BaseModel):
    """LLMモデル情報。"""

    id: str
    name: str
    provider: str
    input_cost_per_1m: float
    output_cost_per_1m: float
    is_free: bool


MODELS_CONFIG = {
    "tngtech/deepseek-r1t2-chimera:free": LLMModel(
        id="tngtech/deepseek-r1t2-chimera:free",
        name="DeepSeek R1T2 Chimera",
        provider="openrouter",
        input_cost_per_1m=0.0,
        output_cost_per_1m=0.0,
        is_free=True,
    ),
    "xiaomi/mimo-v2-flash:free": LLMModel(
        id="xiaomi/mimo-v2-flash:free",
        name="MIMO v2 Flash",
        provider="openrouter",
        input_cost_per_1m=0.0,
        output_cost_per_1m=0.0,
        is_free=True,
    ),
    "x-ai/grok-4.1-fast": LLMModel(
        id="x-ai/grok-4.1-fast",
        name="Grok 4.1 Fast",
        provider="openrouter",
        input_cost_per_1m=0.20,
        output_cost_per_1m=0.50,
        is_free=False,
    ),
    "deepseek/deepseek-v3.2": LLMModel(
        id="deepseek/deepseek-v3.2",
        name="DeepSeek v3.2",
        provider="openrouter",
        input_cost_per_1m=0.25,
        output_cost_per_1m=0.38,
        is_free=False,
    ),
}

DEFAULT_MODEL = "tngtech/deepseek-r1t2-chimera:free"


def get_model(model_id: str) -> LLMModel:
    """モデルIDからモデル情報を取得する。

    Args:
        model_id: モデルID

    Returns:
        LLMModel: モデル情報

    Raises:
        ValueError: モデルIDがプリセットに含まれない場合
    """
    if model_id not in MODELS_CONFIG:
        raise ValueError(f"invalid_model_name: unknown model '{model_id}'")
    return MODELS_CONFIG[model_id]


def get_all_models() -> list[LLMModel]:
    """全モデルを取得する。

    Returns:
        モデル情報のリスト
    """
    return list(MODELS_CONFIG.values())
