"""GitHub パイプラインの E2E 結合テスト。

MemoryS3 + responses モックを使用し、Collector → Transform → Storage → Compaction
の全データフローを検証する。
"""

import responses
from pydantic import SecretStr

from pipelines.sources.common.config import (
    Config,
    DuckDBConfig,
    GitHubWorklogConfig,
    R2Config,
)
from pipelines.sources.github.pipeline import (
    run_github_compact,
    run_github_ingest,
)
from pipelines.tests.e2e.test_browser_history_ingest import (
    _MemoryS3Server,
)
from pipelines.tests.fixtures.github_responses import (
    MOCK_COMMIT_DETAIL_RESPONSE,
    MOCK_PR_COMMITS_RESPONSE,
    MOCK_PR_REVIEWS_RESPONSE,
    MOCK_PULL_REQUEST_RESPONSE,
    MOCK_REPOSITORY_COMMITS_RESPONSE,
    MOCK_REPOSITORY_RESPONSE,
    MOCK_USER_REPOSITORIES_RESPONSE,
)


def _build_config(memory_s3) -> Config:
    """GitHub pipeline 用の設定を構築する。"""
    r2 = R2Config(
        endpoint_url=memory_s3.endpoint_url,
        access_key_id="test-access-key",
        secret_access_key=SecretStr("test-secret-key"),
        bucket_name="test-bucket",
    )
    return Config(
        github_worklog=GitHubWorklogConfig(
            token=SecretStr("test-github-token"),
            github_login="test-user",
            target_repos=["test-user/test-repo"],
            backfill_days=30,
            fetch_commit_details=True,
            max_commit_detail_requests_per_repo=10,
        ),
        duckdb=DuckDBConfig(r2=r2),
    )


def _mock_github_api():
    """GitHub API の必要エンドポイントをモックする。"""
    base = "https://api.github.com"

    # user/repos
    responses.add(
        responses.GET,
        f"{base}/user/repos",
        json=MOCK_USER_REPOSITORIES_RESPONSE,
        status=200,
    )

    # repos/test-user/test-repo
    responses.add(
        responses.GET,
        f"{base}/repos/test-user/test-repo",
        json=MOCK_REPOSITORY_RESPONSE,
        status=200,
    )

    # repos/test-user/test-repo/pulls — 現在月の日付で上書き
    current_pr = {
        **MOCK_PULL_REQUEST_RESPONSE,
        "created_at": "2026-04-01T00:00:00Z",
        "updated_at": "2026-04-01T01:00:00Z",
        "head": {
            "ref": "feature-branch",
            "repo": {
                "full_name": "test-user/test-repo",
                "owner": {"login": "test-user"},
            },
        },
    }
    responses.add(
        responses.GET,
        f"{base}/repos/test-user/test-repo/pulls",
        json=[current_pr],
        status=200,
    )

    # repos/test-user/test-repo/pulls/1/reviews
    responses.add(
        responses.GET,
        f"{base}/repos/test-user/test-repo/pulls/1/reviews",
        json=MOCK_PR_REVIEWS_RESPONSE,
        status=200,
    )

    # repos/test-user/test-repo/commits — 現在月の日付で上書き
    current_commits = [
        {
            "sha": "abc123def456",
            "commit": {
                "author": {
                    "name": "Test User",
                    "email": "test@example.com",
                    "date": "2026-04-01T00:00:00Z",
                },
                "message": "Direct commit to main",
            },
            "author": {
                "login": "test-user",
                "id": 12345,
            },
        },
    ]
    responses.add(
        responses.GET,
        f"{base}/repos/test-user/test-repo/commits",
        json=current_commits,
        status=200,
    )

    # repos/test-user/test-repo/commits/abc123def456 (commit detail)
    # 日付は現在月に合わせて上書き
    commit_detail = {
        **MOCK_COMMIT_DETAIL_RESPONSE,
        "commit": {
            **MOCK_COMMIT_DETAIL_RESPONSE["commit"],
            "author": {
                **MOCK_COMMIT_DETAIL_RESPONSE["commit"]["author"],
                "date": "2026-04-01T00:00:00Z",
            },
        },
    }
    responses.add(
        responses.GET,
        f"{base}/repos/test-user/test-repo/commits/abc123def456",
        json=commit_detail,
        status=200,
    )


@responses.activate
def test_github_ingest_to_compact_end_to_end():
    """GitHub 収集から compaction までの全フローが MemoryS3 上で完結する。"""
    # Arrange
    with _MemoryS3Server() as memory_s3:
        config = _build_config(memory_s3)
        _mock_github_api()

        # Act 1: ingest 実行 (Collector → Transform → Storage)
        ingest_result = run_github_ingest(config=config)

        # Assert 1: ingest 結果を検証
        assert ingest_result["provider"] == "github"
        assert ingest_result["operation"] == "ingest"
        assert ingest_result["status"] == "succeeded"

        # Act 2: compaction 実行 (S3読込 → 重複排除 → 書込)
        compact_result = run_github_compact(config=config)

        # Assert 2: compaction 結果を検証
        assert compact_result["provider"] == "github"
        assert compact_result["operation"] == "compact"
        assert len(compact_result["compacted_keys"]) > 0

        # Assert 3: MemoryS3 に期待されるオブジェクトが保存されている
        object_keys = {key for _, key in memory_s3.objects}

        # raw データ (PRs, commits)
        assert any(k.startswith("raw/github/pull_requests/") for k in object_keys), (
            "raw PR data not found"
        )
        assert any(k.startswith("raw/github/commits/") for k in object_keys), (
            "raw commit data not found"
        )

        # events (commits, pull_requests)
        assert any(k.startswith("events/github/commits/year=") for k in object_keys), (
            "commit events not found"
        )
        assert any(
            k.startswith("events/github/pull_requests/year=") for k in object_keys
        ), "PR events not found"

        # master (repos)
        assert any(k.startswith("master/github/repositories/") for k in object_keys), (
            "repo master not found"
        )

        # compacted data
        assert any(
            k.startswith("compacted/events/github/commits/year=") for k in object_keys
        ), "compacted commits not found"
        assert any(
            k.startswith("compacted/events/github/pull_requests/year=")
            for k in object_keys
        ), "compacted PRs not found"

        # state
        assert any(
            "state/github_worklog_ingest_state.json" in k for k in object_keys
        ), "ingest state not found"
