"""EgoGraphの統一データモデル。

異なるソース（Spotify、YouTubeなど）からの全てのデータは、
処理・保存される前にこの統一スキーマに変換されます。
"""

from datetime import datetime
from enum import Enum
from typing import Any
from uuid import UUID, uuid4

from pydantic import BaseModel, ConfigDict, Field


class DataSource(str, Enum):
    """データソースの種類。"""

    SPOTIFY = "spotify"
    YOUTUBE = "youtube"
    BROWSER = "browser"
    BANK = "bank"
    AMAZON = "amazon"
    GMAIL = "gmail"
    ANDROID = "android"
    PC = "pc"
    STEAM = "steam"
    SWITCH = "switch"
    NOTE = "note"
    TWITTER = "twitter"
    ADULT = "adult"
    MAPS = "maps"
    CALENDAR = "calendar"


class DataType(str, Enum):
    """データコンテンツの種類。"""

    MUSIC = "music"
    VIDEO = "video"
    PURCHASE = "purchase"
    EMAIL = "email"
    APP_USAGE = "app_usage"
    LOCATION = "location"
    EVENT = "event"
    NOTE = "note"
    TWEET = "tweet"
    TRANSACTION = "transaction"


class SensitivityLevel(str, Enum):
    """データの機密レベル分類。"""

    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"


class UnifiedDataModel(BaseModel):
    """全データソースのための統一データモデル。

    このモデルは、全てのデータソース間での一貫性を保証し、
    処理、埋め込み、保存のための共通インターフェースを提供します。

    Attributes:
        id: このデータレコードの一意な識別子
        source: データソース（例: spotify, youtube）
        type: データコンテンツの種類（例: music, video）
        timestamp: データが作成/収集された日時
        raw_text: データの検索可能なテキスト表現
        metadata: ソース固有のメタデータ（柔軟な構造）
        embedding: ベクトル埋め込み（ruri-v3-310mによって入力される）
        sensitivity: プライバシー機密レベル
        nsfw: コンテンツがNSFW/不適切かどうかのフラグ
    """

    model_config = ConfigDict(use_enum_values=True)

    id: UUID = Field(default_factory=uuid4, description="一意な識別子")
    source: DataSource = Field(..., description="データソース")
    type: DataType = Field(..., description="データコンテンツの種類")
    timestamp: datetime = Field(..., description="データの日時 (ISO8601)")
    raw_text: str = Field(..., description="検索可能なテキストコンテンツ")
    metadata: dict[str, Any] = Field(
        default_factory=dict, description="ソース固有のメタデータ"
    )
    embedding: list[float] | None = Field(
        None, description="ベクトル埋め込み (ruri-v3用768次元)"
    )
    sensitivity: SensitivityLevel = Field(
        SensitivityLevel.LOW, description="プライバシー機密レベル"
    )
    nsfw: bool = Field(False, description="NSFW/不適切なコンテンツフラグ")

    def to_dict(self) -> dict[str, Any]:
        """辞書表現に変換します。

        Returns:
            JSONシリアル化可能な全フィールドを含む辞書
        """
        return self.model_dump(mode="json")

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "UnifiedDataModel":
        """辞書からインスタンスを作成します。

        Args:
            data: 辞書表現

        Returns:
            UnifiedDataModelインスタンス
        """
        return cls(**data)
