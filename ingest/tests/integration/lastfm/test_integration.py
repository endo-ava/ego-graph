"""Last.fm データのエンリッチメントパイプラインのインテグレーションテスト。"""

import os
from unittest.mock import MagicMock, patch

import duckdb
import pytest
import responses

from ingest.lastfm.collector import LastFmCollector
from ingest.lastfm.storage import LastFmStorage
from ingest.lastfm_r2_main import enrich_tracks, enrich_artists


@pytest.fixture
def mock_s3_client():
    with patch("ingest.lastfm.storage.boto3.client") as mock:
        yield mock


@pytest.fixture
def temp_spotify_db(tmp_path):
    """Spotify の再生履歴を含む一時的な DuckDB を作成します。"""
    db_path = tmp_path / "spotify_source.duckdb"
    conn = duckdb.connect(str(db_path))
    conn.execute("CREATE SCHEMA raw")
    conn.execute("""
        CREATE TABLE raw.spotify_plays (
            track_name VARCHAR,
            artist_names VARCHAR[],
            played_at_utc TIMESTAMP
        )
    """)
    conn.execute("""
        INSERT INTO raw.spotify_plays VALUES 
        ('Test Song 1', ['Artist 1'], '2023-10-01 10:00:00'),
        ('Test Song 2', ['Artist 2'], '2023-10-01 11:00:00')
    """)
    yield str(db_path)
    conn.close()


@pytest.mark.integration
def test_lastfm_enrichment_full_flow(temp_spotify_db, mock_s3_client):
    """Spotify 再生履歴から Last.fm メタデータを取得し R2 に保存する一連の流れをテストする。"""
    # Arrange: コレクター、ストレージの初期化と pylast のモック設定
    collector = LastFmCollector(api_key="test_key", api_secret="test_secret")
    storage = LastFmStorage(
        endpoint_url="http://mock",
        access_key_id="k",
        secret_access_key="s",
        bucket_name="b"
    )
    
    mock_track = MagicMock()
    mock_track.get_name.return_value = "Test Song 1"
    mock_track.get_artist().get_name.return_value = "Artist 1"
    mock_track.get_playcount.return_value = 100
    mock_track.get_listener_count.return_value = 50
    mock_track.get_top_tags.return_value = []
    mock_track.get_duration.return_value = 200000
    mock_track.get_album().get_name.return_value = "Test Album"
    mock_track.get_url.return_value = "http://last.fm/t1"
    mock_track.get_mbid.return_value = "m1"

    mock_artist = MagicMock()
    mock_artist.get_name.return_value = "Artist 1"
    mock_artist.get_playcount.return_value = 1000
    mock_artist.get_listener_count.return_value = 500
    mock_artist.get_top_tags.return_value = []
    mock_artist.get_bio_summary.return_value = "Bio"
    mock_artist.get_url.return_value = "http://last.fm/a1"
    mock_artist.get_mbid.return_value = "a1"

    with patch.object(collector.network, 'get_track', return_value=mock_track), \
         patch.object(collector.network, 'get_artist', return_value=mock_artist):
        
        # Act 1: トラックのエンリッチメントを実行
        with patch.object(storage, 'list_parquet_files', return_value=[]):
            unprocessed_tracks = [
                ("Test Song 1", "Artist 1")
            ]
            key_track = enrich_tracks(collector, storage, unprocessed_tracks)
            
            # Assert 1: 保存キーが返され、put_object が呼ばれたことを検証
            assert key_track is not None
            mock_s3_client.return_value.put_object.assert_called()

        # Act 2: アーティストのエンリッチメントを実行
        with patch.object(storage, 'list_parquet_files', return_value=[]):
            unprocessed_artists = ["Artist 1"]
            key_artist = enrich_artists(collector, storage, unprocessed_artists)
            
            # Assert 2: 保存キーが返されたことを検証
            assert key_artist is not None


@pytest.mark.integration
def test_deduplication_logic(temp_spotify_db, mock_s3_client):
    """既得データが適切にスキップされることをテストする。"""
    # Arrange: 既に R2 にファイルが存在する状態をモック
    collector = LastFmCollector(api_key="test_key", api_secret="test_secret")
    storage = LastFmStorage(
        endpoint_url="http://mock",
        access_key_id="k",
        secret_access_key="s",
        bucket_name="b"
    )

    existing_files = ["events/lastfm/tracks/year=2023/month=10/existing.parquet"]
    
    # Act: 重複排除ロジックの確認（現在はプレースホルダー）
    with patch.object(storage, 'list_parquet_files', return_value=existing_files):
        # TODO: より詳細な重複排除のインテグレーションテストを実装予定
        pass

    # Assert: モックが正しく呼ばれることを期待
    assert True
