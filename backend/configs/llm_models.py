"""LLM モデル設定。

利用可能な LLM モデルの定義とデフォルトモデルの設定を管理します。
"""

from backend.domain.models.llm_model import LLMModel

MODELS_CONFIG: dict[str, LLMModel] = {
    "openai/gpt-oss-120b:free": LLMModel(
        id="openai/gpt-oss-120b:free",
        name="GPT-OSS-120B",
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
    "mistralai/devstral-2512:free": LLMModel(
        id="mistralai/devstral-2512:free",
        name="DevStral-2512",
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

DEFAULT_MODEL = "xiaomi/mimo-v2-flash:free"
