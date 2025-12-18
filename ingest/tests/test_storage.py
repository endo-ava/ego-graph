import json
import unittest
from unittest.mock import MagicMock, patch

from ingest.spotify.storage import SpotifyStorage


class TestSpotifyStorage(unittest.TestCase):
    def setUp(self):
        self.mock_boto3 = patch("ingest.spotify.storage.boto3").start()
        self.mock_s3 = MagicMock()
        self.mock_boto3.client.return_value = self.mock_s3

        self.storage = SpotifyStorage(
            endpoint_url="http://test-endpoint",
            access_key_id="test-key",
            secret_access_key="test-secret",
            bucket_name="test-bucket",
            raw_path="raw/",
            events_path="events/",
        )

    def tearDown(self):
        patch.stopall()

    def test_save_raw_json(self):
        # Arrange
        data = [{"id": "1", "name": "test"}]

        # Act
        key = self.storage.save_raw_json(data, prefix="test_prefix")

        # Assert
        self.mock_s3.put_object.assert_called_once()
        call_args = self.mock_s3.put_object.call_args[1]
        self.assertEqual(call_args["Bucket"], "test-bucket")
        self.assertTrue(call_args["Key"].startswith("raw/test_prefix/"))
        self.assertTrue(call_args["Key"].endswith(".json"))
        self.assertEqual(call_args["ContentType"], "application/json")
        self.assertIsNotNone(key)

    def test_save_parquet(self):
        # Arrange
        data = [{"col1": 1, "col2": "a"}, {"col1": 2, "col2": "b"}]

        # Act
        with patch("ingest.spotify.storage.pd.DataFrame.to_parquet") as _:
            # We mock to_parquet to avoid needing pyarrow installed in test env
            # assuming pyarrow is installed (we added it), let's let it run naturally,
            # or mock the bytes writing.

            # Let's mock the internal BytesIO write to verify it calls put_object
            key = self.storage.save_parquet(
                data, year=2023, month=10, prefix="test_events"
            )

            # Assert
            self.mock_s3.put_object.assert_called_once()
            call_args = self.mock_s3.put_object.call_args[1]
            self.assertEqual(call_args["Bucket"], "test-bucket")
            self.assertTrue(
                call_args["Key"].startswith("events/test_events/year=2023/month=10/")
            )
            self.assertTrue(call_args["Key"].endswith(".parquet"))
            self.assertEqual(call_args["ContentType"], "application/octet-stream")
            self.assertIsNotNone(key)

    def test_get_ingest_state_exists(self):
        # Arrange
        mock_body = MagicMock()
        mock_body.read.return_value = json.dumps({"cursor": 123}).encode("utf-8")
        self.mock_s3.get_object.return_value = {"Body": mock_body}

        # Act
        state = self.storage.get_ingest_state()

        # Assert
        self.assertEqual(state, {"cursor": 123})
        self.mock_s3.get_object.assert_called_with(
            Bucket="test-bucket", Key="state/ingest_state.json"
        )

    def test_save_ingest_state(self):
        # Arrange
        state = {"cursor": 456}

        # Act
        self.storage.save_ingest_state(state)

        # Assert
        self.mock_s3.put_object.assert_called()
        call_args = self.mock_s3.put_object.call_args[1]
        self.assertEqual(call_args["Key"], "state/ingest_state.json")
        self.assertEqual(json.loads(call_args["Body"]), state)
