EgoGraph開発におけるガイドライン。

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

# Lint & Format (Ruff)
# チェックのみ
uv run ruff check .
# フォーマット実行
uv run ruff format .
# 自動修正可能なLintエラーの修正
uv run ruff check --fix .
```

## アーキテクチャとデータモデル

- **データ配置 (Data Lake)**:
  - **Cloudflare R2**: 正本。Parquetファイル、Raw JSON、State管理をすべてR2に保存。
    - `events/**/*.parquet`: 分析用の構造化データ（年月パーティショニング）
    - `raw/**/*.json`: APIレスポンスの正本（監査用）
    - `state/*.json`: 増分取り込み用のカーソル管理
  - **DuckDB**: Viewレイヤー。Backendで`:memory:`初期化し、R2のParquetを直接クエリ。
- **クライアント**:
  - `frontend/`: モバイル/Webアプリ (Capacitor) がHTTPSでAPIを叩く。

## 実装上の重要ルール

1. **Parquet中心のデータ収集**:
   - GitHub ActionsがCollectorを実行し、R2にParquetファイルを書き出す。
   - DuckDBはR2のParquetを`read_parquet()`で直接読み取る（ステートレス設計）。

2. **Mobile First API**:
   - Backend (FastAPI) は、モバイルアプリからの利用を前提としたREST APIを提供する。
   - 認証はシンプルに（Basic Auth / API Key）実装し、ステートレスにする。

3. **Pythonパッケージング (`__init__.py`)**:
   - **ライブラリ型**（`shared/`）: 公開APIを`__init__.py`で再エクスポート。`from shared import X`で使えるようにする。`__all__`で公開範囲を明示。
   - **アプリケーション型**（`ingest/`, `backend/`）: 最小限の`__init__.py`（docstring + `__version__`のみ）。直接インポートを前提。
   - **バージョン管理**: `pyproject.toml`を単一ソースとし、`importlib.metadata.version()`で動的取得。
   - **サブパッケージ**: 空の`__init__.py`でも配置し、相対インポート（`.`）を可能にする。

## ディレクトリ構成

- `ingest/`: データ収集ワーカー (Python)
- `backend/`: Agent API Server (FastAPI)
- `frontend/`: Mobile/Web App (Capacitor)
- `data/`: サーバー上の永続化データ (Git管理対象外)
- `docs/`: プロジェクトドキュメント

## CI/CD 規約

GitHub Actionsのワークフローファイル (`.github/workflows/`) は、役割を示すプレフィックスを付ける。

- **`ci-*.yml`**: テスト、Lint、ビルドチェックなど、コード品質を保証するための定常CI。
  - 例: `ci-ingest.yml`, `ci-backend.yml`
- **`job-*.yml`**: 定期実行 (Cron) や手動トリガーで動作する実処理ジョブ。
  - 例: `job-ingest-spotify.yml`, `job-db-backup.yml`
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

## その他

ユーザーの要望が曖昧で作業内容が確実に判断できない場合は、必ずまとめて質問すること。
