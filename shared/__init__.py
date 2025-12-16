from importlib.metadata import PackageNotFoundError, version

from .config import Config, EmbeddingConfig, QdrantConfig, SpotifyConfig
from .models import DataSource, DataType, SensitivityLevel, UnifiedDataModel
from .utils import (
    batch_items,
    format_duration_ms,
    log_execution_time,
    safe_get,
    serialize_for_json,
)

try:
    __version__ = version("egograph")
except PackageNotFoundError:
    __version__ = "0.0.0.dev"

__all__ = [
    "Config",
    "DataSource",
    "DataType",
    "EmbeddingConfig",
    "QdrantConfig",
    "SensitivityLevel",
    "SpotifyConfig",
    "UnifiedDataModel",
    "batch_items",
    "format_duration_ms",
    "log_execution_time",
    "safe_get",
    "serialize_for_json",
]
