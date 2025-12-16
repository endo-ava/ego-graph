from unittest.mock import Mock, patch

import pytest
from botocore.exceptions import ClientError

from ingest.spotify.r2_sync import R2Sync


@pytest.fixture
def mock_boto_client():
    with patch("boto3.client") as mock:
        yield mock


@pytest.fixture
def r2_sync(mock_boto_client):
    # Setup mock client instance
    mock_s3 = Mock()
    mock_boto_client.return_value = mock_s3

    sync = R2Sync(
        endpoint_url="http://test-endpoint",
        access_key_id="test-key",
        secret_access_key="test-secret",
        bucket_name="test-bucket",
    )
    return sync


def test_init(mock_boto_client):
    R2Sync(
        endpoint_url="http://test-endpoint",
        access_key_id="test-key",
        secret_access_key="test-secret",
        bucket_name="test-bucket",
    )

    mock_boto_client.assert_called_with(
        "s3",
        endpoint_url="http://test-endpoint",
        aws_access_key_id="test-key",
        aws_secret_access_key="test-secret",
    )


def test_download_db_success(r2_sync, tmp_path):
    local_path = tmp_path / "local.db"

    # Mock download_file to allow successful execution
    r2_sync.s3.download_file.return_value = None

    result = r2_sync.download_db(str(local_path))

    assert result is True
    r2_sync.s3.download_file.assert_called_with(
        "test-bucket", "duckdb/analytics.duckdb", str(local_path)
    )


def test_download_db_custom_name(r2_sync, tmp_path):
    local_path = tmp_path / "custom.db"

    result = r2_sync.download_db(str(local_path), db_name="custom.duckdb")

    assert result is True
    r2_sync.s3.download_file.assert_called_with(
        "test-bucket", "duckdb/custom.duckdb", str(local_path)
    )


def test_download_db_not_found(r2_sync, tmp_path):
    local_path = tmp_path / "local.db"

    # Simulate 404 error
    error_response = {"Error": {"Code": "404"}}
    r2_sync.s3.download_file.side_effect = ClientError(error_response, "DownloadFile")

    result = r2_sync.download_db(str(local_path))

    assert result is False


def test_download_db_other_error(r2_sync, tmp_path):
    local_path = tmp_path / "local.db"

    # Simulate 500 or other error
    error_response = {"Error": {"Code": "500"}}
    r2_sync.s3.download_file.side_effect = ClientError(error_response, "DownloadFile")

    with pytest.raises(ClientError):
        r2_sync.download_db(str(local_path))


def test_upload_db(r2_sync, tmp_path):
    local_path = tmp_path / "local.db"
    # Create a dummy file
    local_path.write_text("dummy data")

    r2_sync.upload_db(str(local_path))

    r2_sync.s3.upload_file.assert_called_with(
        str(local_path),
        "test-bucket",
        "duckdb/analytics.duckdb",
        ExtraArgs={"ContentType": "application/octet-stream"},
    )


def test_get_db_metadata_success(r2_sync):
    expected_meta = {
        "ContentLength": 1024,
        "LastModified": "2025-01-01T00:00:00Z",
        "ETag": "some-hash",
    }
    r2_sync.s3.head_object.return_value = expected_meta

    meta = r2_sync.get_db_metadata()

    assert meta["size_bytes"] == 1024
    assert meta["last_modified"] == "2025-01-01T00:00:00Z"
    assert meta["etag"] == "some-hash"

    r2_sync.s3.head_object.assert_called_with(
        Bucket="test-bucket", Key="duckdb/analytics.duckdb"
    )


def test_get_db_metadata_not_found(r2_sync):
    error_response = {"Error": {"Code": "404"}}
    r2_sync.s3.head_object.side_effect = ClientError(error_response, "HeadObject")

    meta = r2_sync.get_db_metadata()

    assert meta is None


def test_get_db_metadata_other_error(r2_sync):
    error_response = {"Error": {"Code": "403"}}
    r2_sync.s3.head_object.side_effect = ClientError(error_response, "HeadObject")

    with pytest.raises(ClientError):
        r2_sync.get_db_metadata()
