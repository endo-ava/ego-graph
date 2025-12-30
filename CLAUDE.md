EgoGraph開発におけるガイドライン。

## プロジェクト概要

**EgoGraph** は、個人のデジタルライフログを統合する「Personal Data Warehouse」です。
データストアとして **DuckDB** を採用し、サーバーレス・ローカルファーストで動作します。

## 開発コマンド

```bash
# 依存関係の同期
uv sync

# データ収集の実行 (例)
uv run python -m ingest.spotify.main

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
    - `master/spotify/{tracks|artists}/**/*.parquet`: Spotifyマスターデータ
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

## CodeRabbitを使用したコードレビュー
- 各ステップに応じて、以下の方法でコードレビューを実施する

- Commit前:
  - `coderabbit --prompt-only -t uncommitted`

- PR作成前:
  - `coderabbit --prompt-only -t committed --base main`

- PR作成後
  - 自動でコードレビューが実行される
  - ghコマンドでレビューを受け取る:
  - `gh pr view <PR_NUMBER> --json reviews,comments > pr_data.json`

## その他

ユーザーの要望が曖昧で作業内容が確実に判断できない場合は、必ずまとめて質問すること。

## ルール（統一）

### SQL
- SQLは **プレースホルダ必須**。文字列結合でSQLを組み立てず、パラメータを渡す。
- 例外: DuckDBの `strftime` などフォーマット文字列が必要な場合のみ f-string を許可し、コメントで理由を明記する。

### Logging
- **遅延評価**: `logger.info("key=%s", value)` を使用（f-string禁止）。
- **機密情報**: APIキー/トークン/個人情報はログに出さない。
- **エラー**: 例外型とメッセージのみ出力し、DEBUGで詳細スタックを許可する。

### API エラーメッセージ
- **統一フォーマット**: `invalid_<field>: <reason>` を使用する。
- **範囲エラー**: `invalid_date_range: ...` のように意味が分かるキーを先頭に付ける。

### Docstring/コメント
- **言語**: Docstring/コメントは **日本語で統一**する。
