# EgoGraph 開発ガイドライン

## プロジェクト概要

**EgoGraph** は、個人のデジタルライフログを統合する「Personal AI Agent and Personal Data Warehouse」です。
DuckDB を採用し、サーバーレス・ローカルファーストで動作します。

**モノレポ構成**: Python (uv workspace) + Kotlin Multiplatform / Compose Multiplatform

| コンポーネント | 役割           | 技術                                         | エントリポイント                     |
| -------------- | -------------- | -------------------------------------------- | ------------------------------------ |
| **shared/**    | 共有ライブラリ | Python 3.13, Pydantic                        | `__init__.py`（公開API）             |
| **ingest/**    | データ収集     | Spotipy, DuckDB, boto3                       | `ingest.spotify.main:main`           |
| **backend/**   | Agent API      | FastAPI, DuckDB, LLM                         | `backend.main:create_app()`          |
| **frontend/**  | チャット UI    | Kotlin 2.3, Compose Multiplatform, MVIKotlin | `./gradlew :androidApp:installDebug` |

---

## 開発コマンド

```bash
# === 全体（Python Workspace）===
uv sync                          # 依存関係同步
uv run pytest                     # 全テスト
uv run ruff check .               # Lint
uv run ruff check . --fix .       # Lint & Fix
uv run ruff format .              # Format

# === Ingest（データ収集）===
uv run python -m ingest.spotify.main   # Spotify 収集
uv run pytest ingest/tests --cov=ingest

# === Backend（API サーバー）===
uv run python -m backend.main
uv run pytest backend/tests --cov=backend
open http://localhost:8000/docs
uv run python -m backend.dev_tools.chat_cli # デバッグ用LLM CLIツール

# === Frontend（モバイル/Web）===
cd frontend
./gradlew :androidApp:installDebug    # デバッグ実行
./gradlew :shared:testDebugUnitTest   # テスト
./gradlew ktlintCheck                 # Lint チェック
./gradlew ktlintFormat                # Format
```

---

## アーキテクチャ

### データフロー

External APIs → GitHub Actions (Ingest) → R2 (Parquet) → Backend (DuckDB) → Frontend (HTTPS)

### データ配置

- **R2**: 正本（Parquet/Raw JSON、年月パーティショニング）
- **DuckDB**: View レイヤー（`:memory:` で R2 直接クエリ）
- **Qdrant**: 意味検索インデックス

---

## CI/CD 規約

### ワークフロー命名

| プレフィックス  | 用途                    | 例                       |
| --------------- | ----------------------- | ------------------------ |
| `ci-*.yml`      | テスト・Lint（定常 CI） | `ci-backend.yml`         |
| `job-*.yml`     | 定期実行・手動ジョブ    | `job-ingest-spotify.yml` |
| `deploy-*.yml`  | デプロイ                | `deploy-web-app.yml`     |
| `release-*.yml` | リリース                | `release-v1.0.0.yml`     |

---

## Git & PR 規約

### ブランチ・コミット

- **戦略**: GitHub Flow（`main` への直接コミット禁止）
- **ブランチ命名**: `<type>/<short-description>`（例: `feat/add-sound-playback`）
- **コミット規約**: Conventional Commits（`<type>: <subject>`）
- **コミット言語**: 英語

---

## コーディング規約

### SQL

- **プレースホルダ必須**: `execute(query, (param,))`

```python
# 例
cursor.execute("SELECT * FROM events WHERE user_id = ?", (user_id,))
```

### Logging

- **遅延評価**: `logger.info("key=%s", value)`（f-string 禁止）
- **機密情報**: API キー/トークン/個人情報を出力しない
- **エラー**: 例外型とメッセージのみ（DEBUG で詳細スタック許可）

### API エラーメッセージ

- **統一フォーマット**: `invalid_<field>: <reason>`
- **例**: `raise ValueError("invalid_date_range: start_date must be before end_date")`

### Docstring/コメント

- **言語**: 日本語統一

---

## テスト

### Python

- **フレームワーク**: pytest + pytest-cov
- **実行**: `uv run pytest`
- **統一設定**: ルート `pyproject.toml` で管理

### Frontend

- **フレームワーク**: Kotest + JUnit
- **実行**: `cd frontend && ./gradlew :shared:testDebugUnitTest`

### 共通

- AAA パターンで記述すること

---

## デバッグ・テスト方針

### スキル選択ガイド

| シナリオ             | 使用スキル                             | 説明                                           |
| -------------------- | -------------------------------------- | ---------------------------------------------- |
| **APIのみ**          | `tmux-api-debug`                       | Backend APIの動作確認・デバッグ                |
| **UI + API（E2E）**  | `android-adb-debug` + `tmux-api-debug` | フロントエンドからバックエンドまでの統合テスト |
| **接続トラブル**     | `adb-connection-troubleshoot`          | ADB接続問題の診断・解決                        |
| **LLM ToolCall検証** | `agent-tool-test`                      | 各LLMモデルの全ツール使用可否テスト            |

### 環境構成

```
Linux (開発環境)
  ├─ Backend (tmux session)     ← tmux-api-debug
  └─ ADB Client                 ← android-adb-debug
       ↓ (Tailscale: 100.x.x.x:5559)
Windows (エミュレータホスト)
  └─ Android Emulator
```

### クイックコマンド

```bash
# APIのみのデバッグ
# → tmux-api-debug スキルをロード

# UI + APIの統合デバッグ
# 1. Backend起動（tmux）
# 2. エミュレータ接続 & アプリインストール
./.claude/skills/android-adb-debug/scripts/linux_connect_and_install.sh
```

---

## CodeRabbit レビュー

CodeRabbitは、コードレビューのためのAIツールです。以下のコマンドで使用できます。

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

- ユーザーに質問をする場合は`AskUserQuestion`（またはそれに類するツール）を活用すること。

- 積極的にサブエージェントを活用し、メインコンテキストをクリーンに保つこと。

- コードを変更した後は、テストが通ることを確認すること。

- OpenCodeでサブエージェントを呼ぶ場合は、必ず`delegate_task`を活用すること。`task`は使わない。
