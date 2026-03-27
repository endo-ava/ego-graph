# GitHub データソース

## 1. 概要

- **データの性質**: 構造化ログ
- **粒度**: Atomic (イベント単位)
- **更新頻度**: 日次
- **センシティビティレベル**: Medium (公開リポジトリは Low)

---

## 2. 対象データ

| データタイプ | 説明 | 取得方法 |
|-------------|------|----------|
| commits | コミット履歴 | GitHub API (`GET /repos/{owner}/{repo}/commits`) |
| issues | Issue 作成・更新 | GitHub API (`GET /repos/{owner}/{repo}/issues`) |
| pull_requests | PR 作成・更新 | GitHub API (`GET /repos/{owner}/{repo}/pulls`) |
| reviews | PR レビュー | GitHub API (`GET /repos/{owner}/{repo}/pulls/{pull_number}/reviews`) |
| workflow_runs | GitHub Actions 実行履歴 | GitHub API (`GET /repos/{owner}/{repo}/actions/runs`) |

---

## 3. スキーマ定義

### 3.1 Commit イベントスキーマ

| フィールド | 型 | 説明 |
|-----------|---|------|
| `commit_event_id` | string | 一意識別子 (`{repo_full_name}:{sha}`) |
| `source` | string | データソース (`github`) |
| `owner` | string | リポジトリオーナー |
| `repo` | string | リポジトリ名 |
| `repo_full_name` | string | リポジトリフルネーム (`owner/repo`) |
| `sha` | string | コミットハッシュ |
| `message` | string | コミットメッセージ |
| `committed_at_utc` | datetime | コミット時刻 (UTC) |
| `changed_files_count` | int | 変更ファイル数 |
| `additions` | int | 追加行数 |
| `deletions` | int | 削除行数 |
| `ingested_at_utc` | datetime | 取り込み時刻 (UTC) |

### 3.2 Pull Request イベントスキーマ

| フィールド | 型 | 説明 |
|-----------|---|------|
| `pr_event_id` | string | 一意識別子（ハッシュ値） |
| `pr_key` | string | PRユニークキー（ハッシュ値） |
| `source` | string | データソース (`github`) |
| `owner` | string | リポジトリオーナー |
| `repo` | string | リポジトリ名 |
| `repo_full_name` | string | リポジトリフルネーム (`owner/repo`) |
| `pr_number` | int | PR番号 |
| `pr_id` | int | GitHub PR ID |
| `action` | string | アクション種別 (`opened`, `updated`, `closed`, `merged`, `reopened`) |
| `state` | string | PR状態 (`open`, `closed`) |
| `is_merged` | bool | マージ済みフラグ |
| `title` | string | PRタイトル |
| `labels` | array | ラベル一覧 |
| `base_ref` | string | マージ先ブランチ |
| `head_ref` | string | マージ元ブランチ |
| `created_at_utc` | datetime | 作成時刻 (UTC) |
| `updated_at_utc` | datetime | 更新時刻 (UTC) |
| `closed_at_utc` | datetime | クローズ時刻 (UTC) |
| `merged_at_utc` | datetime | マージ時刻 (UTC) |
| `comments_count` | int | コメント数 |
| `review_comments_count` | int | レビューコメント数 |
| `reviews_count` | int | レビュー数 |
| `commits_count` | int | コミット数 |
| `additions` | int | 追加行数 |
| `deletions` | int | 削除行数 |
| `changed_files_count` | int | 変更ファイル数 |
| `ingested_at_utc` | datetime | 取り込み時刻 (UTC) |

### 3.3 共通フィールド

以下のフィールドは Commit / Pull Request 両イベントで共通：

| フィールド | 型 | 説明 |
|-----------|---|------|
| `source` | string | データソース (`github`) |
| `owner` | string | リポジトリオーナー |
| `repo` | string | リポジトリ名 |
| `repo_full_name` | string | リポジトリフルネーム (`owner/repo`) |
| `ingested_at_utc` | datetime | 取り込み時刻 (UTC) |

### 3.4 Parquet 保存先

```text
s3://ego-graph/events/github/
  ├── commits/
  │   ├── year=2024/
  │   │   ├── month=01/
  │   │   │   └── {uuid}.parquet
  │   │   └── month=02/
  │   │       └── {uuid}.parquet
  │   └── ...
  └── pull_requests/
      ├── year=2024/
      │   ├── month=01/
      │   │   └── {uuid}.parquet
      │   └── ...
      └── ...
```

---

## 4. ワークフロー

- **ワークフロー**: `job-ingest-github.yml`
- **実行タイミング**: Cron (1日1回: 15:00 UTC = 00:00 JST 深夜)
- **増分取り込み**: R2 内のカーソル (`state/github_worklog_ingest_state.json`) で管理

---

## 5. 実装詳細

### 5.1 ディレクトリ構成

```
ingest/github/
├── __init__.py
├── main.py           # エントリーポイント
├── collector.py      # GitHub API データ取得
├── transform.py      # データ変換・正規化
├── storage.py        # R2 アップロード
├── pipeline.py       # ETL パイプライン統合
└── compact.py        # Parquet 最適化
```

### 5.2 認証

- **認証方式**: GitHub Personal Access Token (PAT)
- **必要なスコープ**: `repo`, `read:user`
- **環境変数**: `GITHUB_PAT`, `GITHUB_LOGIN`

---

## 6. Semantification 戦略

### 6.1 自然言語化テンプレート

| イベントタイプ | テンプレート |
|--------------|-------------|
| commit | `{repository}で{message}をコミットした` |
| issue_open | `{repository}でIssue #{number}: {title}を作成した` |
| issue_close | `{repository}でIssue #{number}: {title}をクローズした` |
| pr_open | `{repository}でPR #{number}: {title}を作成した` |
| pr_merge | `{repository}でPR #{number}: {title}をマージした` |
| review | `{repository}のPR #{pr_number}をレビューした` |
| workflow_success | `{repository}でワークフロー{workflow_name}が成功した` |
| workflow_failure | `{repository}でワークフロー{workflow_name}が失敗した` |

---

## 7. 検索シナリオ例

| 質問 | 検索戦略 |
|-----|---------|
| 「先週どんなコードを書いた？」 | `commits` テーブル、`committed_at_utc` で期間フィルタ |
| 「最近マージしたPRは？」 | `pull_requests` テーブル、`is_merged=true`、`merged_at_utc` で降順ソート |
| 「特定リポジトリの活動は？」 | `repo_full_name` でフィルタ |

---

## 8. 参考

- [GitHub REST API Documentation](https://docs.github.com/rest)
- [Ingest 共通アーキテクチャ](./README.md)
- [データモデル](../01-overview/data-strategy.md)
