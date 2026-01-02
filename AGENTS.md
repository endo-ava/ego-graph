# EgoGraph 開発ガイドライン

## プロジェクト概要

**EgoGraph** は、個人のデジタルライフログを統合する「Personal Data Warehouse」です。
DuckDB を採用し、サーバーレス・ローカルファーストで動作します。

**モノレポ構成**: Python (uv workspace) + Node.js (npm)

| コンポーネント | 役割 | 技術 | エントリポイント |
|--------------|------|------|----------------|
| **shared/** | 共有ライブラリ | Python 3.13, Pydantic | `__init__.py`（公開API） |
| **ingest/** | データ収集 | Spotipy, DuckDB, boto3 | `ingest.spotify.main:main` |
| **backend/** | Agent API | FastAPI, DuckDB, LLM | `backend.main:create_app()` |
| **frontend/** | チャット UI | React 19, Capacitor 8 | `npm run dev` |

---

## 開発コマンド

### 全体（Python Workspace）
```bash
uv sync                          # 依存関係同期
uv run pytest                     # 全テスト
uv run ruff check .               # Lint
uv run ruff format .              # Format
```

### Ingest（データ収集）
```bash
uv run python -m ingest.spotify.main   # Spotify 収集
uv run pytest ingest/tests --cov=ingest
```

### Backend（API サーバー）
```bash
uv run uvicorn backend.main:app --reload
uv run pytest backend/tests --cov=backend
open http://localhost:8000/docs
```

### Frontend（モバイル/Web）
```bash
cd frontend
npm install && npm run dev        # Web 開発
npm run test:run                  # テスト
npm run android:sync              # モバイル同期
```

---

## アーキテクチャ

### データフロー
External APIs → GitHub Actions (Ingest) → R2 (Parquet) → Backend (DuckDB) → Frontend (HTTPS)

### データ配置
- **R2**: 正本（Parquet/Raw JSON、年月パーティショニング）
- **DuckDB**: View レイヤー（`:memory:` で R2 直接クエリ）
- **Qdrant**: 意味検索インデックス

### コンポーネント依存
```
shared/ ← 基盤ライブラリ
  ↑
  ├─ ingest/   (workspace依存)
  └─ backend/  (workspace依存)

frontend/  (独立、Backend API のみ利用)
```

詳細: [システムアーキテクチャ](./docs/10.architecture/1001_system_architecture.md)

---

## 実装ルール

### 1. Parquet 中心のデータ収集
- GitHub Actions が R2 に Parquet 書き出し
- DuckDB が `read_parquet('s3://...')` で直接読取（ステートレス）

### 2. Mobile First API
- FastAPI で REST API 提供（Basic Auth/API Key）
- ステートレス設計

### 3. Python パッケージング
- **shared/**: `__init__.py` で公開 API 再エクスポート、`__all__` で範囲明示
- **ingest/, backend/**: 最小限の `__init__.py`（docstring のみ）
- **バージョン**: `pyproject.toml` を単一ソース
- **workspace 依存**: `shared @ {workspace = true}`

---

## CI/CD 規約

### ワークフロー命名

| プレフィックス | 用途 | 例 |
|--------------|------|---|
| `ci-*.yml` | テスト・Lint（定常 CI） | `ci-backend.yml` |
| `job-*.yml` | 定期実行・手動ジョブ | `job-ingest-spotify.yml` |
| `deploy-*.yml` | デプロイ | `deploy-web-app.yml` |

### CI 構成

| ファイル | トリガー | 備考 |
|---------|---------|------|
| `ci-backend.yml` | `backend/**`, `shared/**` | Coverage → Codecov |
| `ci-ingest.yml` | `ingest/**`, `shared/**` | Coverage → Codecov |
| `ci-frontend.yml` | `frontend/**` | Vitest |
| `job-ingest-spotify.yml` | Cron: `0 2,14 * * *` | 1日2回実行 |

---

## Git & PR 規約

### ブランチ・コミット
- **戦略**: GitHub Flow（`main` への直接コミット禁止）
- **ブランチ命名**: `<type>/<short-description>`（例: `feat/add-sound-playback`）
- **コミット規約**: Conventional Commits（`<type>: <subject>`）
- **コミット言語**: 英語

### PR
- **単位**: 1 PR = 1 関心事
- **テンプレート**: `.github/PULL_REQUEST_TEMPLATE.md` 使用
- **レビュー**: 対応要否を自己判断、不要なら理由を説明

### 言語
- **日本語**: コードコメント、PR/Issue、レビュー
- **英語**: コミットメッセージ

---

## コーディング規約

### SQL
- **プレースホルダ必須**: `execute(query, (param,))`
- **例外**: DuckDB の `strftime` のみ f-string 許可（コメント明記）

```python
# 良い例
cursor.execute("SELECT * FROM events WHERE user_id = ?", (user_id,))

# 悪い例
cursor.execute(f"SELECT * FROM events WHERE user_id = {user_id}")  # ❌
```

### Logging
- **遅延評価**: `logger.info("key=%s", value)`（f-string 禁止）
- **機密情報**: API キー/トークン/個人情報を出力しない
- **エラー**: 例外型とメッセージのみ（DEBUG で詳細スタック許可）

```python
# 良い例
logger.info("Processing user_id=%s", user_id)

# 悪い例
logger.info(f"API Key: {api_key}")  # ❌ 機密情報漏洩
```

### API エラーメッセージ
- **統一フォーマット**: `invalid_<field>: <reason>`
- **例**: `raise ValueError("invalid_date_range: start_date must be before end_date")`

### Docstring/コメント
- **言語**: 日本語統一

```python
def fetch_plays(user_id: str) -> list[SpotifyPlayEvent]:
    """指定されたユーザーの再生履歴を取得する。

    Args:
        user_id: Spotify ユーザーID

    Returns:
        再生履歴のリスト
    """
    ...
```

---

## テスト

### Python
- **フレームワーク**: pytest + pytest-cov
- **実行**: `uv run pytest`
- **統一設定**: ルート `pyproject.toml` で管理

### Frontend
- **フレームワーク**: Vitest
- **実行**: `npm run test:run` / `npm run test:coverage`

---

## CodeRabbit レビュー

```bash
# Commit 前
coderabbit --prompt-only -t uncommitted

# PR 作成前
coderabbit --prompt-only -t committed --base main

# PR 作成後（自動実行）
gh pr view <PR_NUMBER> --json reviews,comments > pr_data.json
```

---

## その他

ユーザーの要望が曖昧な場合は、必ずまとめて質問すること。
