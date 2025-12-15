# CLAUDE.md

EgoGraph開発における、Claude Code (claude.ai/code) 向けのガイドライン。
**Architecture Pivot: DuckDB Centric**

## プロジェクト概要

**EgoGraph** は、個人のデジタルライフログを統合する「Personal Data Warehouse」です。
データストアとして **DuckDB** を採用し、サーバーレス・ローカルファーストで動作します。

## 開発コマンド

```bash
# 依存関係の同期
uv sync

# データ収集の実行 (例)
uv run python ingest/main.py

# テスト実行
uv run pytest

# Spotify接続テスト (Live Env)
python3 -m ingest.tests.test_live_collector
```

## アーキテクチャとデータモデル

- **データ配置 (Server-side)**:
  - `data/events/**/*.parquet`: データレイクとしてParquetファイルを保存。
  - `data/analytics.duckdb`: 永続化が必要なDuckDB本体。
- **クライアント**:
  - `frontend/`: モバイル/Webアプリ (Capacitor) がHTTPSでAPIを叩く。

## 実装上の重要ルール

1. **Parquet中心のデータ収集**:
   - Collectorは `data/` ディレクトリ（永続化ボリューム）にParquetファイルを書き出す。
   - DuckDBはこれを参照するが、書き込み競合を避けるため直接INSERTはしない。

2. **Mobile First API**:
   - Backend (FastAPI) は、モバイルアプリからの利用を前提としたREST APIを提供する。
   - 認証はシンプルに（Basic Auth / API Key）実装し、ステートレスにする。

## ディレクトリ構成

- `ingest/`: データ収集ワーカー (Python)
- `backend/`: Agent API Server (FastAPI)
- `frontend/`: Mobile/Web App (Capacitor)
- `data/`: サーバー上の永続化データ (Git管理対象外)
- `docs/`: プロジェクトドキュメント

## CI/CD 規約

GitHub Actionsのワークフローファイル (`.github/workflows/`) は、役割を示すプレフィックスを付ける。

- **`ci-*.yml`**: テスト、Lint、ビルドチェックなど、コード品質を保証するための定常CI。
  - 例: `ci-ingest-spotify.yml`, `ci-backend.yml`
- **`job-*.yml`**: 定期実行 (Cron) や手動トリガーで動作する実処理ジョブ。
  - 例: `job-spotify-ingest.yml`, `job-db-backup.yml`
- **`deploy-*.yml`**: アプリケーションのデプロイなど。
  - 例: `deploy-web-app.yml`

## Git & Pull Request 規約

- **ブランチ**:
  - **戦略**: GitHub Flow (`main`への直接コミット禁止)
  - **命名**: `<type>/<short-description>` (例: `feat/add-sound-playback`, `refactor/optimize-svg-rendering`)
- **コミット**:
  - **規約**: Conventional Commits (`<type>: <subject>`)
  - **主な `<type>`**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
  - **言語**: **英語**
- **Pull Request (PR)**:
  - **単位**: 1 PR = 1関心事。巨大化させない
  - **記述**: テンプレート (`.github/PULL_REQUEST_TEMPLATE.md`) を使用。Descriptionにはそのブランチの変更内容を網羅
- **レビュー**:
  - レビューは鵜呑みにせず、対応要否を自身で判断。不要な場合は理由を伝える
- **言語**:
  - **日本語**: コードコメント、PR/Issue、レビュー
  - **英語**: コミットメッセージ