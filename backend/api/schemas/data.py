"""データ API スキーマ。

Spotify データ API のレスポンスモデルを定義します。
"""

from pydantic import BaseModel


class TopTrackResponse(BaseModel):
    """トップトラック API レスポンス。

    Attributes:
        track_name: 曲名
        artist: アーティスト名
        play_count: 再生回数
        total_minutes: 総再生時間（分）
    """

    track_name: str
    artist: str
    play_count: int
    total_minutes: float


class ListeningStatsResponse(BaseModel):
    """視聴統計 API レスポンス。

    Attributes:
        period: 期間（日付文字列）
        total_ms: 総再生時間（ミリ秒）
        track_count: 再生トラック数
        unique_tracks: ユニーク曲数
    """

    period: str
    total_ms: int
    track_count: int
    unique_tracks: int
