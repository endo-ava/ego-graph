"""Last.fm 統合パッケージ。

Deprecated: Last.fm 連携は一時停止中。
"""

from ingest.lastfm.collector import LastFmCollector
from ingest.lastfm.storage import LastFmStorage
from ingest.lastfm.transform import transform_artist_info, transform_track_info

__all__ = [
    "LastFmCollector",
    "LastFmStorage",
    "transform_artist_info",
    "transform_track_info",
]
