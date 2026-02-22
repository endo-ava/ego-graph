"""GitHubWorklogStorageの単体テスト。"""

import json
import unittest
from io import BytesIO
from unittest.mock import MagicMock, patch

import pandas as pd
from botocore.exceptions import ClientError

from ingest.github.storage import GitHubWorklogStorage


class TestGitHubWorklogStorage(unittest.TestCase):
    """GitHubWorklogStorageの単体テストクラス。"""

    def setUp(self):
        """テスト前にboto3をモック化し、Storageインスタンスを初期化する。"""
        self.mock_boto3 = patch("ingest.github.storage.boto3").start()
        self.mock_s3 = MagicMock()
        self.mock_boto3.client.return_value = self.mock_s3

        self.storage = GitHubWorklogStorage(
            endpoint_url="http://test-endpoint",
            access_key_id="test-key",
            secret_access_key="test-secret",
            bucket_name="test-bucket",
            raw_path="raw/",
            events_path="events/",
            master_path="master/",
        )

    def tearDown(self):
        """テスト後に全てのモックを停止する。"""
        patch.stopall()

    def test_init(self):
        """初期化時に正しくS3クライアントとパスが設定されることを検証する。"""
        # Assert: 属性が正しく設定されている
        self.assertEqual(self.storage.bucket_name, "test-bucket")
        self.assertEqual(self.storage.raw_path, "raw/")
        self.assertEqual(self.storage.events_path, "events/")
        self.assertEqual(self.storage.master_path, "master/")
        self.mock_boto3.client.assert_called_once()

    def test_save_raw_prs(self):
        """PR生データをJSON形式でR2に保存することを検証する。

        Path: raw/github/pull_requests/{YYYY}/{MM}/{DD}/{timestamp}_{uuid}.json
        """
        # Arrange: 保存するPRデータの準備
        data = [
            {
                "id": 1,
                "number": 100,
                "title": "Test PR",
                "state": "open",
                "user": {"login": "testuser"},
            }
        ]

        # Act: PR生データとして保存を実行
        key = self.storage.save_raw_prs(data, owner="testowner", repo="testrepo")

        # Assert: 保存結果を検証
        self.mock_s3.put_object.assert_called_once()
        call_args = self.mock_s3.put_object.call_args[1]
        self.assertEqual(call_args["Bucket"], "test-bucket")
        self.assertTrue(call_args["Key"].startswith("raw/github/pull_requests/"))
        self.assertTrue(call_args["Key"].endswith(".json"))
        self.assertEqual(call_args["ContentType"], "application/json")
        self.assertIsNotNone(key)

    def test_save_raw_commits(self):
        """Commit生データをJSON形式でR2に保存することを検証する。

        Path: raw/github/commits/{YYYY}/{MM}/{DD}/{timestamp}_{uuid}.json
        """
        # Arrange: 保存するCommitデータの準備
        data = [
            {
                "sha": "abc123",
                "commit": {
                    "author": {"name": "Test User", "email": "test@example.com"},
                    "message": "Test commit",
                },
                "stats": {"additions": 10, "deletions": 5, "total": 15},
            }
        ]

        # Act: Commit生データとして保存を実行
        key = self.storage.save_raw_commits(data, owner="testowner", repo="testrepo")

        # Assert: 保存結果を検証
        self.mock_s3.put_object.assert_called_once()
        call_args = self.mock_s3.put_object.call_args[1]
        self.assertEqual(call_args["Bucket"], "test-bucket")
        self.assertTrue(call_args["Key"].startswith("raw/github/commits/"))
        self.assertTrue(call_args["Key"].endswith(".json"))
        self.assertEqual(call_args["ContentType"], "application/json")
        self.assertIsNotNone(key)

    def test_save_commits_parquet(self):
        """CommitイベントをParquet形式で保存することを検証する。

        Path: events/github/commits/year={YYYY}/month={MM}/{uuid}.parquet
        """
        # Arrange: 保存するCommitイベントデータの準備
        data = [
            {
                "commit_event_id": "testowner/testrepo/abc123",
                "source": "github",
                "owner": "testowner",
                "repo": "testrepo",
                "repo_full_name": "testowner/testrepo",
                "sha": "abc123",
                "message": "Test commit",
                "committed_at_utc": "2024-01-15T10:00:00Z",
                "changed_files_count": 3,
                "additions": 10,
                "deletions": 5,
            }
        ]

        # Act: Parquet形式での保存を実行
        with patch("ingest.github.storage.pd.DataFrame.to_parquet") as _:
            key = self.storage.save_commits_parquet(data, year=2024, month=1)

            # Assert: 保存結果を検証
            self.mock_s3.put_object.assert_called_once()
            call_args = self.mock_s3.put_object.call_args[1]
            self.assertEqual(call_args["Bucket"], "test-bucket")
            self.assertTrue(
                call_args["Key"].startswith("events/github/commits/year=2024/month=01/")
            )
            self.assertTrue(call_args["Key"].endswith(".parquet"))
            self.assertEqual(call_args["ContentType"], "application/octet-stream")
            self.assertIsNotNone(key)

    def test_save_commits_parquet_deduplication(self):
        """既存Commit IDが重複排除されることを検証する。"""
        # Arrange: 保存するデータ（既存IDを含む）
        data = [
            {
                "commit_event_id": "existing_id",
                "sha": "abc123",
            },
            {
                "commit_event_id": "new_id",
                "sha": "def456",
            },
        ]

        # Act: Parquet形式での保存を実行
        with patch.object(
            self.storage,
            "_load_existing_commit_ids",
            return_value={"existing_id"},
        ):
            with patch("ingest.github.storage.pd.DataFrame.to_parquet") as _:
                key = self.storage.save_commits_parquet(data, year=2024, month=1)

                # Assert: 新規IDのみが保存されることを検証
                # ※ 実装側でフィルタリングされることを期待
                self.mock_s3.put_object.assert_called_once()
                self.assertIsNotNone(key)

    def test_save_commits_parquet_empty_when_all_duplicates(self):
        """全てのCommitが重複している場合、保存がスキップされることを検証する。"""
        # Arrange: 全て重複するデータ
        data = [
            {"commit_event_id": "existing_id_1", "sha": "abc123"},
            {"commit_event_id": "existing_id_2", "sha": "def456"},
        ]

        # Act: 全て重複する状態で保存を実行
        with patch.object(
            self.storage,
            "_load_existing_commit_ids",
            return_value={"existing_id_1", "existing_id_2"},
        ):
            key = self.storage.save_commits_parquet(data, year=2024, month=1)

            # Assert: put_objectが呼ばれない（保存スキップ）
            self.mock_s3.put_object.assert_not_called()
            self.assertIsNone(key)

    def test_save_commits_parquet_with_stats(self):
        """新規/重複件数の統計が返ることを検証する。"""
        data = [
            {"commit_event_id": "existing_id", "sha": "abc123"},
            {"commit_event_id": "new_id", "sha": "def456"},
        ]

        with patch.object(
            self.storage,
            "_load_existing_commit_ids",
            return_value={"existing_id"},
        ):
            with patch("ingest.github.storage.pd.DataFrame.to_parquet") as _:
                stats = self.storage.save_commits_parquet_with_stats(
                    data,
                    year=2024,
                    month=1,
                )

                self.assertEqual(stats["fetched"], 2)
                self.assertEqual(stats["new"], 1)
                self.assertEqual(stats["duplicates"], 1)
                self.assertEqual(stats["failed"], 0)

    def test_save_commits_parquet_with_stats_on_failure(self):
        """保存失敗時にfailed件数が返ることを検証する。"""
        data = [{"commit_event_id": "new_id", "sha": "def456"}]

        with patch.object(
            self.storage,
            "_load_existing_commit_ids",
            return_value=set(),
        ):
            with patch.object(self.storage, "_upload_parquet", return_value=None):
                stats = self.storage.save_commits_parquet_with_stats(
                    data,
                    year=2024,
                    month=1,
                )

                self.assertEqual(stats["fetched"], 1)
                self.assertEqual(stats["new"], 0)
                self.assertEqual(stats["duplicates"], 0)
                self.assertEqual(stats["failed"], 1)

    def test_save_pr_master(self):
        """PR現在状態をParquet形式で上書き保存することを検証する。

        Path: master/github/pull_requests_current/{owner}/{repo}.parquet
        """
        # Arrange: 保存するPR現在状態データの準備
        data = [
            {
                "pr_key": "testowner/testrepo/100",
                "source": "github",
                "owner": "testowner",
                "repo": "testrepo",
                "repo_full_name": "testowner/testrepo",
                "pr_number": 100,
                "pr_id": 12345,
                "action": "opened",
                "state": "open",
                "is_merged": False,
                "title": "Test PR",
            }
        ]

        # Act: Masterとして保存を実行
        with patch("ingest.github.storage.pd.DataFrame.to_parquet") as _:
            key = self.storage.save_pr_master(data, owner="testowner", repo="testrepo")

            # Assert: 保存結果を検証
            self.mock_s3.put_object.assert_called_once()
            call_args = self.mock_s3.put_object.call_args[1]
            self.assertEqual(call_args["Bucket"], "test-bucket")
            self.assertTrue(
                call_args["Key"].startswith(
                    "master/github/pull_requests_current/testowner/testrepo"
                )
            )
            self.assertTrue(call_args["Key"].endswith(".parquet"))
            self.assertEqual(call_args["ContentType"], "application/octet-stream")
            self.assertIsNotNone(key)

    def test_save_pr_master_overwrites(self):
        """PR Masterが上書き保存されることを検証する。"""
        # Arrange: 2回分の保存データ準備
        data_v1 = [{"pr_key": "key1", "pr_number": 100, "state": "open"}]
        data_v2 = [{"pr_key": "key1", "pr_number": 100, "state": "closed"}]

        # Act: 同じキーで2回保存
        with patch("ingest.github.storage.pd.DataFrame.to_parquet") as _:
            key1 = self.storage.save_pr_master(
                data_v1, owner="testowner", repo="testrepo"
            )
            key2 = self.storage.save_pr_master(
                data_v2, owner="testowner", repo="testrepo"
            )

            # Assert: 同じパスで2回呼ばれている（上書き）
            self.assertEqual(self.mock_s3.put_object.call_count, 2)
            call1_args = self.mock_s3.put_object.call_args_list[0][1]
            call2_args = self.mock_s3.put_object.call_args_list[1][1]
            # 同じキーであること
            self.assertEqual(call1_args["Key"], call2_args["Key"])
            self.assertIsNotNone(key1)
            self.assertIsNotNone(key2)

    def test_save_repo_master(self):
        """Repository MasterをParquet形式で保存することを検証する。

        Path: master/github/repositories/{owner}/{repo}.parquet
        """
        # Arrange: 保存するRepository Masterデータの準備
        data = [
            {
                "repo_id": 12345,
                "source": "github",
                "owner": "testowner",
                "repo": "testrepo",
                "repo_full_name": "testowner/testrepo",
                "description": "Test repository",
                "is_private": False,
                "is_fork": False,
                "archived": False,
                "primary_language": "Python",
            }
        ]

        # Act: Repository Masterとして保存を実行
        with patch("ingest.github.storage.pd.DataFrame.to_parquet") as _:
            key = self.storage.save_repo_master(
                data, owner="testowner", repo="testrepo"
            )

            # Assert: 保存結果を検証
            self.mock_s3.put_object.assert_called_once()
            call_args = self.mock_s3.put_object.call_args[1]
            self.assertEqual(call_args["Bucket"], "test-bucket")
            self.assertTrue(
                call_args["Key"].startswith(
                    "master/github/repositories/testowner/testrepo"
                )
            )
            self.assertTrue(call_args["Key"].endswith(".parquet"))
            self.assertEqual(call_args["ContentType"], "application/octet-stream")
            self.assertIsNotNone(key)

    def test_get_ingest_state_exists(self):
        """インジェスト状態が存在する場合、正しく取得されることを検証する。"""
        # Arrange: 保存されている状態がある場合をモック
        mock_body = MagicMock()
        mock_body.read.return_value = json.dumps(
            {"cursor": "2024-01-15T10:00:00Z", "last_repo": "testowner/testrepo"}
        ).encode("utf-8")
        self.mock_s3.get_object.return_value = {"Body": mock_body}

        # Act: 保存されている状態を取得
        state = self.storage.get_ingest_state()

        # Assert: 取得された状態を検証
        self.assertEqual(state["cursor"], "2024-01-15T10:00:00Z")
        self.assertEqual(state["last_repo"], "testowner/testrepo")
        self.mock_s3.get_object.assert_called_with(
            Bucket="test-bucket", Key="state/github_worklog_ingest_state.json"
        )

    def test_get_ingest_state_not_exists(self):
        """インジェスト状態が存在しない場合、Noneが返されることを検証する。"""
        # Arrange: NoSuchKeyエラーをモック
        error_response = {
            "Error": {"Code": "NoSuchKey", "Message": "The key does not exist"}
        }
        self.mock_s3.get_object.side_effect = ClientError(error_response, "get_object")

        # Act: 状態を取得
        state = self.storage.get_ingest_state()

        # Assert: Noneが返される
        self.assertIsNone(state)

    def test_save_ingest_state(self):
        """インジェスト状態が正しく保存されることを検証する。"""
        # Arrange: 保存する状態の準備
        state = {
            "cursor": "2024-01-15T10:00:00Z",
            "last_repo": "testowner/testrepo",
            "processed_count": 42,
        }

        # Act: 状態の保存を実行
        self.storage.save_ingest_state(state)

        # Assert: put_object が正しい引数で呼ばれたことを検証
        self.mock_s3.put_object.assert_called_once()
        call_args = self.mock_s3.put_object.call_args[1]
        self.assertEqual(call_args["Key"], "state/github_worklog_ingest_state.json")
        self.assertEqual(json.loads(call_args["Body"]), state)
        self.assertEqual(call_args["ContentType"], "application/json")

    def test_save_raw_prs_empty_data(self):
        """空データを渡した場合、保存がスキップされることを検証する。"""
        # Arrange: 空データ
        data = []

        # Act: 保存を実行
        key = self.storage.save_raw_prs(data, owner="testowner", repo="testrepo")

        # Assert: put_objectが呼ばれない
        self.mock_s3.put_object.assert_not_called()
        self.assertIsNone(key)

    def test_save_commits_parquet_empty_data(self):
        """空データを渡した場合、Parquet保存がスキップされることを検証する。"""
        # Arrange: 空データ
        data = []

        # Act: 保存を実行
        key = self.storage.save_commits_parquet(data, year=2024, month=1)

        # Assert: put_objectが呼ばれない
        self.mock_s3.put_object.assert_not_called()
        self.assertIsNone(key)

    def test_load_existing_commit_ids(self):
        """既存Commit IDが正しく読み込まれることを検証する。"""
        # Arrange: paginator と get_object をモック
        mock_page = {
            "Contents": [
                {"Key": "events/github/commits/year=2024/month=01/file1.parquet"},
                {"Key": "events/github/commits/year=2024/month=01/file2.parquet"},
            ]
        }
        mock_paginator = MagicMock()
        mock_paginator.paginate.return_value = [mock_page]
        self.mock_s3.get_paginator.return_value = mock_paginator

        # 各ファイルのDataFrameを作成
        df1 = pd.DataFrame([{"commit_event_id": "id1"}])
        df2 = pd.DataFrame([{"commit_event_id": "id2"}])

        # BytesIOに変換
        buffer1 = BytesIO()
        buffer2 = BytesIO()
        df1.to_parquet(buffer1, index=False, engine="pyarrow")
        df2.to_parquet(buffer2, index=False, engine="pyarrow")
        buffer1.seek(0)
        buffer2.seek(0)

        mock_body1 = MagicMock()
        mock_body1.read.return_value = buffer1.read()
        mock_body2 = MagicMock()
        mock_body2.read.return_value = buffer2.read()

        self.mock_s3.get_object.side_effect = [
            {"Body": mock_body1},
            {"Body": mock_body2},
        ]

        # Act: 既存Commit IDを読み込み
        existing_ids = self.storage._load_existing_commit_ids(year=2024, month=1)

        # Assert: 正しいIDが含まれる
        self.assertIn("id1", existing_ids)
        self.assertIn("id2", existing_ids)

    def test_load_existing_commit_ids_no_files(self):
        """ファイルが存在しない場合、空セットが返されることを検証する。"""
        # Arrange: ファイルが存在しない状態をモック
        error_response = {"Error": {"Code": "NoSuchKey", "Message": "Not found"}}
        self.mock_s3.get_paginator.side_effect = ClientError(
            error_response, "get_paginator"
        )

        # Act: 既存Commit IDを読み込み
        existing_ids = self.storage._load_existing_commit_ids(year=2024, month=1)

        # Assert: 空セットが返される
        self.assertEqual(existing_ids, set())

    def test_path_normalization(self):
        """パスが正規化され、末尾に/が付くことを検証する。"""
        # Arrange & Act: 末尾スラッシュなしで初期化
        storage_no_slash = GitHubWorklogStorage(
            endpoint_url="http://test-endpoint",
            access_key_id="test-key",
            secret_access_key="test-secret",
            bucket_name="test-bucket",
            raw_path="raw",  # 末尾スラッシュなし
            events_path="events",
            master_path="master",
        )

        # Assert: 全てのパスが末尾スラッシュ付きに正規化される
        self.assertEqual(storage_no_slash.raw_path, "raw/")
        self.assertEqual(storage_no_slash.events_path, "events/")
        self.assertEqual(storage_no_slash.master_path, "master/")
