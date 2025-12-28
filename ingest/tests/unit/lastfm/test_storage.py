"""Tests for Last.fm storage handler (R2)."""

import json
from unittest.mock import MagicMock, patch

import pytest
from botocore.exceptions import ClientError

from ingest.lastfm.storage import LastFmStorage


@pytest.fixture
def mock_s3_client():
    with patch("ingest.lastfm.storage.boto3.client") as mock:
        yield mock


@pytest.fixture
def storage(mock_s3_client):
    return LastFmStorage(
        endpoint_url="https://r2.example.com",
        access_key_id="key",
        secret_access_key="secret",
        bucket_name="test-bucket",
    )


def test_save_parquet_success(storage, mock_s3_client):
    """R2 への Parquet 保存成功をテストする。"""
    # Arrange: 保存するデータの準備
    data = [{"track_name": "T1", "artist_name": "A1"}]
    mock_s3 = mock_s3_client.return_value

    # Act: 保存を実行
    key = storage.save_parquet(data, 2023, 12, prefix="lastfm/tracks")

    # Assert: 保存結果と s3.put_object の呼び出しを検証
    assert key is not None
    assert "master/lastfm/tracks/year=2023/month=12/" in key
    assert key.endswith(".parquet")

    mock_s3.put_object.assert_called_once()
    _, kwargs = mock_s3.put_object.call_args
    assert kwargs["Bucket"] == "test-bucket"
    assert kwargs["Key"] == key
    assert kwargs["ContentType"] == "application/octet-stream"


def test_save_parquet_empty_data(storage, mock_s3_client):
    """空のデータリストでの save_parquet をテストする。"""
    # Act & Assert: 空データを渡したときに保存が行われず None が返ることを検証
    assert storage.save_parquet([], 2023, 12) is None
    mock_s3_client.return_value.put_object.assert_not_called()


def test_get_ingest_state_success(storage, mock_s3_client):
    """取り込み状態（ステート）の取得成功をテストする。"""
    # Arrange: ステートデータの準備とモックの設定
    state_data = {"latest_played_at": "2023-12-22T00:00:00Z"}
    mock_s3 = mock_s3_client.return_value
    mock_body = MagicMock()
    mock_body.read.return_value = json.dumps(state_data).encode("utf-8")
    mock_s3.get_object.return_value = {"Body": mock_body}

    # Act: ステートを取得
    state = storage.get_ingest_state()

    # Assert: 取得されたステートを検証
    assert state == state_data
    mock_s3.get_object.assert_called_with(
        Bucket="test-bucket", Key="state/lastfm_ingest_state.json"
    )


def test_get_ingest_state_not_found(storage, mock_s3_client):
    """ステートファイルが存在しない場合をテストする。"""
    # Arrange: NoSuchKey エラーをモック
    mock_s3 = mock_s3_client.return_value
    error_response = {"Error": {"Code": "NoSuchKey"}}
    mock_s3.get_object.side_effect = ClientError(error_response, "GetObject")

    # Act: ステートを取得
    state = storage.get_ingest_state()

    # Assert: 結果が None であることを検証
    assert state is None


def test_save_ingest_state(storage, mock_s3_client):
    """ステートの保存をテストする。"""
    # Arrange: 保存するステートを準備
    state = {"cursor": "xyz"}

    # Act: ステートの保存を実行
    storage.save_ingest_state(state)

    # Assert: 正しい引数で put_object が呼ばれたことを検証
    mock_s3_client.return_value.put_object.assert_called_once_with(
        Bucket="test-bucket",
        Key="state/lastfm_ingest_state.json",
        Body=json.dumps(state),
        ContentType="application/json",
    )


def test_list_parquet_files(storage, mock_s3_client):
    """Parquet ファイルのリストアップをテストする（ページネーション含む）。"""
    # Arrange: ページネーターのモック設定
    mock_s3 = mock_s3_client.return_value
    mock_paginator = MagicMock()
    mock_s3.get_paginator.return_value = mock_paginator

    mock_paginator.paginate.return_value = [
        {"Contents": [{"Key": "master/lastfm/tracks/year=2023/month=12/f1.parquet"}]},
        {"Contents": [{"Key": "master/lastfm/tracks/year=2023/month=12/f2.parquet"}]},
    ]

    # Act: ファイルリストを取得
    files = storage.list_parquet_files("lastfm/tracks")

    # Assert: 全てのファイル名が含まれていることを検証
    assert len(files) == 2
    assert "master/lastfm/tracks/year=2023/month=12/f1.parquet" in files
    assert "master/lastfm/tracks/year=2023/month=12/f2.parquet" in files
