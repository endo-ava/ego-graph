"""サンプルParquetデータ生成ユーティリティ。"""

import pandas as pd
from datetime import datetime, timedelta
from io import BytesIO


def generate_sample_spotify_plays(num_tracks: int = 100) -> pd.DataFrame:
    """サンプルSpotify再生履歴データを生成。

    Args:
        num_tracks: 生成するトラック数

    Returns:
        サンプル再生履歴のDataFrame
    """
    base_time = datetime(2024, 1, 1, 10, 0, 0)

    data = []
    for i in range(num_tracks):
        data.append(
            {
                "played_at_utc": base_time + timedelta(minutes=i * 3),
                "track_id": f"track_{i % 20}",  # 20種類のトラックを繰り返し
                "track_name": f"Song {i % 20}",
                "artist_names": [f"Artist {i % 10}"],
                "album_name": f"Album {i % 5}",
                "ms_played": 180000 + (i * 1000),
            }
        )

    return pd.DataFrame(data)


def save_parquet_to_bytes(df: pd.DataFrame) -> bytes:
    """DataFrameをParquetバイト列に変換。

    Args:
        df: 変換するDataFrame

    Returns:
        Parquet形式のバイト列
    """
    buffer = BytesIO()
    df.to_parquet(buffer, engine="pyarrow")
    buffer.seek(0)
    return buffer.read()
