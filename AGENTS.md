# EgoGraph 開発ガイドライン

## 概要

**EgoGraph**: Personal AI Agent and Personal Data Warehouse（DuckDB, サーバーレス・ローカルファースト）

**構成**: Python (uv workspace) + Kotlin Multiplatform / Compose Multiplatform

| コンポーネント | 役割           | 技術                                         | エントリポイント                     |
| -------------- | -------------- | -------------------------------------------- | ------------------------------------ |
| **shared/**    | 共有ライブラリ | Python 3.13, Pydantic                        | `__init__.py`                        |
| **ingest/**    | データ収集     | Spotipy, DuckDB, boto3                       | `ingest.spotify.main:main`           |
| **backend/**   | Agent API      | FastAPI, DuckDB, LLM                         | `backend.main:create_app()`          |
| **frontend/**  | チャット UI    | Kotlin 2.3, Compose Multiplatform, MVIKotlin | `./gradlew :androidApp:installDebug` |

## 開発コマンド

```bash
# === Python Workspace ===
uv sync                           # 依存関係同期
uv run pytest                     # 全テスト
uv run ruff check .               # Lint
uv run ruff check . --fix         # Lint & Fix
uv run ruff format .              # Format

# === Ingest ===
uv run python -m ingest.spotify.main
uv run pytest ingest/tests --cov=ingest

# === Backend ===
uv run python -m backend.main                 # http://localhost:8000/docs
uv run pytest backend/tests --cov=backend
uv run python -m backend.dev_tools.chat_cli   # デバッグ用CLIツール

# === Frontend (cd frontend) ===
cd frontend # PJルートからはgradlewは使えないことに注意
./gradlew :androidApp:assembleDebug      # ビルド
./gradlew :androidApp:installDebug      # インストール
./gradlew :shared:testDebugUnitTest     # テスト
./gradlew ktlintCheck                   # Lint
./gradlew ktlintFormat                  # Format
./gradlew detekt                        # 静的解析
# NOTE: ktlintFormat/ktlintCheck は同一コマンドで連続実行せず、先に ktlintFormat 単体で実行する（同一Gradle実行内だと ktlintCheck が先に走って失敗することがあるため）

# === E2E Test (Maestro) ===
maestro test maestro/flows/           # 全テスト一括実行
```

## アーキテクチャ

```
External APIs → GitHub Actions (Ingest) → R2 (Parquet) → Backend (DuckDB) → Frontend
```

| ストレージ | 役割                                     |
| ---------- | ---------------------------------------- |
| R2         | 正本（Parquet/JSON、年月パーティション） |
| DuckDB     | View（`:memory:` で R2 直接クエリ）      |
| Qdrant     | 意味検索インデックス                     |

## 規約

### Git / CI

- **GitHub Flow**: `main` 直接コミット禁止、ブランチ `<type>/<desc>`
- **コミット**: Conventional Commits（英語）
- **ワークフロー**: `ci-*.yml`(テスト), `job-*.yml`(定期), `deploy-*.yml`, `release-*.yml`

### コーディング

| 項目      | ルール                                             |
| --------- | -------------------------------------------------- |
| SQL       | プレースホルダ必須: `execute(query, (param,))`     |
| Logging   | 遅延評価 `logger.info("k=%s", v)`, 機密情報禁止    |
| APIエラー | 統一フォーマット `invalid_<field>: <reason>`       |
| Docstring | 日本語                                             |
| テスト    | AAA パターン必須、Python: pytest、Frontend: Kotest |

## デバッグ

### スキル選択

| シナリオ             | 使用スキル                             | 説明                                           |
| -------------------- | -------------------------------------- | ---------------------------------------------- |
| **APIのみ**          | `tmux-api-debug`                       | Backend APIの動作確認・デバッグ                |
| **UI + API（E2E）**  | `android-adb-debug` + `tmux-api-debug` | フロントエンドからバックエンドまでの統合テスト |
| **接続トラブル**     | `adb-connection-troubleshoot`          | ADB接続問題の診断・解決                        |
| **LLM ToolCall検証** | `agent-tool-test`                      | 各LLMモデルの全ツール使用可否テスト            |

### 環境構成

```
Linux ─ Backend (tmux) + ADB Client
    ↓ Tailscale:100.x.x.x:5559
Windows ─ netsh (0.0.0.0:5559→127.0.0.1:5555) ─ Android Emulator (:5555)
```

※ 5559を外部公開する理由: エミュレータの:5555とのポート競合回避

### Frontend開発フロー

1. Windows側でエミュ起動（ユーザー作業、要確認）
2. Linux から ADB 接続

   ```bash
   adb connect <WINDOWS_IP>:5559
   adb devices
   ```

   - `<WINDOWS_IP>` は `frontend/.env.local` の `WINDOWS_IP`

3. Backend を起動

   ```bash
   uv run python -m backend.main

   - tmuxを使ってもよい
   ```

4. adb コマンドで現在の挙動を確認しながら実装
5. ビルド & インストール
   ```bash
   cd frontend && ./gradlew :androidApp:installDebug
   ```
6. adb コマンドでビルド内容の確認

## CodeRabbit

```bash
coderabbit --prompt-only -t uncommitted              # Commit前
coderabbit --prompt-only -t committed --base main    # PR作成前
```

## その他

- 質問は `AskUserQuestion` 等を活用
- サブエージェント活用でコンテキストをクリーンに（`delegate_task` を使用、`task` は使わない）
- コード変更後はテスト確認必須
