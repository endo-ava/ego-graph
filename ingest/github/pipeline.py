"""GitHub作業ログ取り込みパイプラインのオーケストレーション。"""

import logging
from collections import defaultdict
from datetime import datetime, timedelta, timezone
from typing import Any

from ingest.config import Config
from ingest.github.collector import GitHubWorklogCollector
from ingest.github.storage import GitHubWorklogStorage
from ingest.github.transform import (
    transform_commits_to_events,
    transform_prs_to_master,
    transform_repository,
)

logger = logging.getLogger(__name__)


def _parse_iso_utc(value: str | None) -> datetime | None:
    """ISO8601文字列をUTC datetimeに変換する。"""
    if not value:
        return None
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00")).astimezone(
            timezone.utc
        )
    except ValueError:
        return None


def _resolve_since_iso(
    state: dict[str, Any] | None,
    backfill_days: int,
) -> str:
    """増分取得開始時刻を決定する。"""
    cursor = None
    if state:
        cursor = state.get("cursor_utc") or state.get("last_ingested_at")

    if cursor:
        logger.info("Using incremental cursor: %s", cursor)
        return cursor

    start = datetime.now(timezone.utc) - timedelta(days=backfill_days)
    since = start.isoformat()
    logger.info("No cursor found. Backfill mode since=%s", since)
    return since


def run_pipeline(config: Config) -> None:
    """GitHub作業ログインジェストの実行ロジック。

    Args:
        config: 設定情報（GitHubとR2を含む）

    Raises:
        ValueError: 設定が不足している場合
        RuntimeError: パイプラインの実行に失敗した場合
    """
    if not config.github_worklog:
        raise ValueError("GitHub worklog configuration is required")
    if not config.duckdb or not config.duckdb.r2:
        raise ValueError("R2 configuration is required for this pipeline")

    github_conf = config.github_worklog
    r2_conf = config.duckdb.r2

    logger.info("=" * 60)
    logger.info("GitHub Worklog Ingestion Pipeline")
    logger.info(f"GitHub User: {github_conf.github_login}")
    logger.info(f"Target Repos: {github_conf.target_repos or 'All personal repos'}")
    logger.info("=" * 60)

    # StorageとCollectorを初期化
    storage = GitHubWorklogStorage(
        endpoint_url=r2_conf.endpoint_url,
        access_key_id=r2_conf.access_key_id,
        secret_access_key=r2_conf.secret_access_key.get_secret_value(),
        bucket_name=r2_conf.bucket_name,
        raw_path=r2_conf.raw_path,
        events_path=r2_conf.events_path,
        master_path=r2_conf.master_path,
    )

    collector = GitHubWorklogCollector(
        token=github_conf.token.get_secret_value(),
        github_login=github_conf.github_login,
    )

    # 状態を取得し、増分取得開始時刻を決定
    state = storage.get_ingest_state()
    since_iso = _resolve_since_iso(state, github_conf.backfill_days)

    # ターゲットリポジトリを決定
    if github_conf.target_repos:
        # 指定されたリポジトリのみを処理
        target_repos = github_conf.target_repos
        logger.info(f"Processing {len(target_repos)} specified repositories")
    else:
        # ユーザーの全リポジトリを取得
        all_repos = collector.get_user_repositories()
        target_repos = [r["full_name"] for r in all_repos]
        logger.info(f"Found {len(target_repos)} personal repositories")

    if not target_repos:
        logger.warning("No repositories to process. Exiting.")
        return

    # 各リポジトリを処理
    total_prs = 0
    total_commits = 0
    total_new_commits = 0
    total_duplicate_commits = 0
    total_failed_records = 0
    total_failed_api_calls = 0
    total_failed_repos = 0

    all_commits_data = []
    max_cursor_candidate: datetime | None = None

    def update_cursor_candidate(value: str | None) -> None:
        nonlocal max_cursor_candidate
        dt = _parse_iso_utc(value)
        if dt is None:
            return
        if max_cursor_candidate is None or dt > max_cursor_candidate:
            max_cursor_candidate = dt

    for repo_full_name in target_repos:
        try:
            owner, repo = repo_full_name.split("/", 1)
            logger.info(f"Processing repository: {repo_full_name}")

            # Repository情報を取得
            repo_info = collector.get_repository(owner, repo)
            repo_transformed = transform_repository(repo_info, github_conf.github_login)
            if repo_transformed:
                repo_saved = storage.save_repo_master([repo_transformed], owner, repo)
                if repo_saved is None:
                    logger.error(
                        "Failed to save repository master for %s", repo_full_name
                    )
                    total_failed_records += 1
            else:
                logger.info(f"Skipping non-personal repo: {repo_full_name}")
                continue

            # PR一覧を取得
            prs = collector.get_pull_requests(owner, repo, since=since_iso)
            logger.info(f"Found {len(prs)} PRs in {repo_full_name}")

            # 各PRのレビュー数を取得
            for pr in prs:
                pr_number = pr.get("number")
                if pr_number:
                    try:
                        reviews = collector.get_pr_reviews(owner, repo, pr_number)
                        pr["reviews_count"] = len(reviews)
                    except Exception as e:
                        pr_number_str = str(pr_number)
                        logger.warning(
                            "Failed to fetch reviews for PR #%s: %s",
                            pr_number_str,
                            e,
                        )
                        pr["reviews_count"] = 0
                        total_failed_api_calls += 1

            for pr in prs:
                update_cursor_candidate(pr.get("updated_at"))

            total_prs += len(prs)

            # PR Masterを保存
            if prs:
                prs_transformed = transform_prs_to_master(prs, github_conf.github_login)
                total_failed_records += len(prs) - len(prs_transformed)
                if prs_transformed:
                    pr_master_saved = storage.save_pr_master(
                        prs_transformed, owner, repo
                    )
                    if pr_master_saved is None:
                        logger.error("Failed to save PR master for %s", repo_full_name)
                        total_failed_records += len(prs_transformed)

                # PR生データを保存
                raw_pr_saved = storage.save_raw_prs(prs, owner, repo)
                if raw_pr_saved is None:
                    logger.error("Failed to save raw PRs for %s", repo_full_name)
                    total_failed_records += len(prs)

            # Repository Commitsを取得
            commits = collector.get_repository_commits(owner, repo, since=since_iso)
            logger.info(f"Found {len(commits)} commits in {repo_full_name}")

            # 各Commitの詳細を取得（変更量メタデータ用）
            enriched_commits = []
            for commit in commits:
                sha = commit.get("sha")
                if sha:
                    try:
                        detail = collector.get_commit_detail(owner, repo, sha)
                        # 詳細情報をマージ
                        commit_with_detail = {**commit, **detail}
                        enriched_commits.append(commit_with_detail)
                    except Exception as e:
                        logger.warning(f"Failed to fetch detail for commit {sha}: {e}")
                        total_failed_api_calls += 1
                        enriched_commits.append(commit)
                else:
                    enriched_commits.append(commit)

            # Commitsを変換
            commits_transformed = transform_commits_to_events(
                enriched_commits, repo_full_name
            )
            total_failed_records += len(enriched_commits) - len(commits_transformed)
            all_commits_data.extend(commits_transformed)
            total_commits += len(commits_transformed)

            for commit in commits_transformed:
                update_cursor_candidate(commit.get("committed_at_utc"))

            # Commit生データを保存
            if commits:
                raw_commits_saved = storage.save_raw_commits(commits, owner, repo)
                if raw_commits_saved is None:
                    logger.error("Failed to save raw commits for %s", repo_full_name)
                    total_failed_records += len(commits)

        except Exception as e:
            logger.error(f"Failed to process repository {repo_full_name}: {e}")
            total_failed_repos += 1
            continue

    logger.info(f"Total collected: {total_prs} PRs, {total_commits} commits")

    # Commitイベントを年月でグループ化して保存
    commits_by_month = _group_commits_by_month(all_commits_data)

    all_saved = True
    for (year, month), commits in commits_by_month.items():
        stats = storage.save_commits_parquet_with_stats(commits, year, month)
        if stats["failed"] > 0:
            logger.error(f"Failed to save commits Parquet for {year}-{month:02d}")
            all_saved = False
        else:
            logger.info(
                "Saved commits for %d-%02d (fetched=%d new=%d duplicates=%d)",
                year,
                month,
                stats["fetched"],
                stats["new"],
                stats["duplicates"],
            )
        total_new_commits += stats["new"]
        total_duplicate_commits += stats["duplicates"]
        total_failed_records += stats["failed"]

    logger.info(
        "Ingest stats: prs=%d commits_fetched=%d commits_new=%d duplicates=%d failed_records=%d failed_api=%d failed_repos=%d",
        total_prs,
        total_commits,
        total_new_commits,
        total_duplicate_commits,
        total_failed_records,
        total_failed_api_calls,
        total_failed_repos,
    )

    # 状態を更新
    if all_saved and total_failed_api_calls == 0 and total_failed_repos == 0:
        cursor = (
            max_cursor_candidate.isoformat()
            if max_cursor_candidate is not None
            else datetime.now(timezone.utc).isoformat()
        )
        new_state = {
            "cursor_utc": cursor,
            "total_repos": len(target_repos),
            "updated_at": datetime.now(timezone.utc).isoformat(),
        }
        storage.save_ingest_state(new_state)
        logger.info("Pipeline completed successfully!")
    else:
        logger.warning("Pipeline had failures. State not updated.")


def _group_commits_by_month(
    commits: list[dict[str, Any]],
) -> dict[tuple[int, int], list[dict[str, Any]]]:
    """コミットイベントを年月でグループ化する。

    Args:
        commits: コミットイベントのリスト

    Returns:
        年月をキーとしたコミットリストの辞書
    """
    grouped: dict[tuple[int, int], list[dict[str, Any]]] = defaultdict(list)

    for commit in commits:
        committed_date = commit.get("committed_at_utc")
        if committed_date:
            try:
                # ISO 8601形式を解析
                dt = datetime.fromisoformat(committed_date.replace("Z", "+00:00"))
                grouped[(dt.year, dt.month)].append(commit)
            except (ValueError, AttributeError) as e:
                logger.warning("Failed to parse date %s: %s", committed_date, e)
        else:
            # 日付がない場合、現在の月に含める
            now = datetime.now(timezone.utc)
            grouped[(now.year, now.month)].append(commit)

    return grouped
