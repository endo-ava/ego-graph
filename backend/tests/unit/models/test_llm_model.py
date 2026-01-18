"""backend/models/llm_model.pyのユニットテスト。"""

import pytest

from backend.api.schemas import DEFAULT_MODEL, LLMModel, get_all_models, get_model
from backend.configs.llm_models import MODELS_CONFIG


class TestLLMModel:
    """LLMModelクラスのテスト。"""

    def test_llm_model_creation(self):
        """LLMModelが正しく作成される。"""
        # Arrange & Act
        model = LLMModel(
            id="test-model",
            name="Test Model",
            provider="test",
            input_cost_per_1m=0.5,
            output_cost_per_1m=1.0,
            is_free=False,
        )

        # Assert
        assert model.id == "test-model"
        assert model.name == "Test Model"
        assert model.provider == "test"
        assert model.input_cost_per_1m == 0.5
        assert model.output_cost_per_1m == 1.0
        assert model.is_free is False

    def test_llm_model_free_model(self):
        """無料モデルのis_freeがTrueになる。"""
        # Arrange & Act
        model = LLMModel(
            id="free-model",
            name="Free Model",
            provider="openrouter",
            input_cost_per_1m=0.0,
            output_cost_per_1m=0.0,
            is_free=True,
        )

        # Assert
        assert model.is_free is True
        assert model.input_cost_per_1m == 0.0
        assert model.output_cost_per_1m == 0.0


class TestGetModel:
    """get_model関数のテスト。"""

    def test_get_model_with_valid_id(self):
        """有効なモデルIDでモデルが返される。"""
        # Arrange
        model_id = "xiaomi/mimo-v2-flash:free"

        # Act
        model = get_model(model_id)

        # Assert
        assert isinstance(model, LLMModel)
        assert model.id == model_id
        assert model.name == "MIMO v2 Flash"
        assert model.provider == "openrouter"
        assert model.is_free is True

    def test_get_model_with_all_preset_models(self):
        """すべてのプリセットモデルが取得できる。"""
        # Arrange: MODELS_CONFIGの全モデルIDをテスト
        for model_id in MODELS_CONFIG.keys():
            # Act
            model = get_model(model_id)

            # Assert
            assert isinstance(model, LLMModel)
            assert model.id == model_id
            assert model == MODELS_CONFIG[model_id]

    def test_get_model_with_invalid_id_raises_value_error(self):
        """無効なモデルIDでValueErrorが発生する。"""
        # Arrange
        invalid_model_id = "nonexistent-model"

        # Act & Assert
        with pytest.raises(ValueError) as exc_info:
            get_model(invalid_model_id)

        # エラーメッセージの検証
        assert "invalid_model_name" in str(exc_info.value)
        assert invalid_model_id in str(exc_info.value)

    def test_get_model_returns_correct_model_details(self):
        """get_modelが正しいモデル詳細を返す。"""
        # Arrange
        model_id = "deepseek/deepseek-v3.2"

        # Act
        model = get_model(model_id)

        # Assert
        assert model.id == "deepseek/deepseek-v3.2"
        assert model.name == "DeepSeek v3.2"
        assert model.provider == "openrouter"
        assert model.input_cost_per_1m == 0.25
        assert model.output_cost_per_1m == 0.38
        assert model.is_free is False


class TestGetAllModels:
    """get_all_models関数のテスト。"""

    def test_get_all_models_returns_list(self):
        """get_all_modelsがリストを返す。"""
        # Act
        models = get_all_models()

        # Assert
        assert isinstance(models, list)

    def test_get_all_models_contains_all_models(self):
        """get_all_modelsがすべてのモデルを含む。"""
        # Act
        models = get_all_models()

        # Assert
        assert len(models) == len(MODELS_CONFIG)
        for model in models:
            assert isinstance(model, LLMModel)
            assert model.id in MODELS_CONFIG

    def test_get_all_models_includes_default_model(self):
        """get_all_modelsがデフォルトモデルを含む。"""
        # Act
        models = get_all_models()
        model_ids = [model.id for model in models]

        # Assert
        assert DEFAULT_MODEL in model_ids

    def test_get_all_models_returns_correct_types(self):
        """get_all_modelsが正しい型のオブジェクトを返す。"""
        # Act
        models = get_all_models()

        # Assert
        for model in models:
            assert isinstance(model, LLMModel)
            assert isinstance(model.id, str)
            assert isinstance(model.name, str)
            assert isinstance(model.provider, str)
            assert isinstance(model.input_cost_per_1m, float)
            assert isinstance(model.output_cost_per_1m, float)
            assert isinstance(model.is_free, bool)


class TestModelsConfig:
    """MODELS_CONFIGの整合性テスト。"""

    def test_default_model_exists_in_config(self):
        """デフォルトモデルがMODELS_CONFIGに存在する。"""
        # Assert
        assert DEFAULT_MODEL in MODELS_CONFIG

    def test_all_models_have_required_fields(self):
        """すべてのモデルが必須フィールドを持つ。"""
        # Act & Assert
        for model_id, model in MODELS_CONFIG.items():
            assert model.id == model_id
            assert len(model.name) > 0
            assert len(model.provider) > 0
            assert model.input_cost_per_1m >= 0.0
            assert model.output_cost_per_1m >= 0.0
            assert isinstance(model.is_free, bool)

    def test_free_models_have_zero_cost(self):
        """無料モデルのコストが0である。"""
        # Act & Assert
        for model in MODELS_CONFIG.values():
            if model.is_free:
                assert model.input_cost_per_1m == 0.0
                assert model.output_cost_per_1m == 0.0

    def test_paid_models_have_positive_cost(self):
        """有料モデルのコストが正の値である。"""
        # Act & Assert
        for model in MODELS_CONFIG.values():
            if not model.is_free:
                assert model.input_cost_per_1m > 0.0 or model.output_cost_per_1m > 0.0
