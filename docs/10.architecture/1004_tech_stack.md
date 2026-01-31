# 技術スタック

モノレポ構成における各コンポーネントの技術選定とその理由。

---

## モノレポ構成

| コンポーネント | 言語/FW     | パッケージマネージャー | 主要ライブラリ                   |
| -------------- | ----------- | ---------------------- | -------------------------------- |
| **shared/**    | Python 3.13 | uv                     | Pydantic, python-dotenv          |
| **ingest/**    | Python 3.13 | uv                     | Spotipy, DuckDB, boto3, pyarrow  |
| **backend/**   | Python 3.13 | uv                     | FastAPI, Uvicorn, DuckDB         |
| **frontend/**  | Kotlin 2.3  | Gradle                 | Compose Multiplatform, MVIKotlin |

- **Python Workspace**: uv で shared, ingest, backend を一元管理
- **Frontend**: Kotlin Multiplatform (Gradle)

---

## 1. Data Storage

### DuckDB (OLAP 分析エンジン)

- **用途**: SQL 分析、集計、台帳管理
- **Extension**:
  - `parquet`: Parquet ファイルに対する高速クエリ
  - `httpfs`: Cloudflare R2 (S3互換) からの直接読取
- **実行モード**: `:memory:` (Backend でステートレス実行)
- **理由**: 列指向処理による高速集計、ファイルベースで運用が簡単

### Qdrant Cloud (ベクトル検索)

- **用途**: 意味検索、RAG のインデックス
- **Free Tier**: 1GB メモリ（約10万ベクトル）
- **理由**: マネージドサービスで運用不要、Backend のメモリ負荷を削減

### Cloudflare R2 (Object Storage)

- **用途**: 正本（Parquet/Raw JSON）の永続化
- **特徴**: S3 互換、egress 無料
- **構造**:
  - `events/`: 時系列データ（年月パーティショニング）
  - `master/`: マスターデータ
  - `raw/`: API レスポンス（監査用）
  - `state/`: 増分取り込みカーソル

---

## 2. Shared Library（共有ライブラリ）

- **Language**: Python 3.13
- **主要ライブラリ**:
  - `pydantic`: データモデル定義
  - `python-dotenv`: 環境変数管理
- **パッケージング**: Hatchling でビルド、`__init__.py` で公開 API を再エクスポート
- **用途**: ingest, backend で共通利用するモデル・設定・ユーティリティ

---

## 3. Ingest Pipeline（データ収集）

- **Language**: Python 3.13
- **実行環境**: GitHub Actions（定期実行: 1日2回）
- **主要ライブラリ**:
  - `spotipy`: Spotify API クライアント
  - `pyarrow`: Parquet ファイル作成
  - `boto3`: R2 アップロード
  - `duckdb`: データ変換・検証
- **特性**: Idempotent（冪等性）、Stateful（カーソル管理）

---

## 4. Backend（Agent API Server）

- **Framework**: FastAPI (Python 3.13)
- **Web Server**: Uvicorn (ASGI)
- **主要ライブラリ**:
  - `duckdb`: データアクセス
  - `httpx`: 外部 API 呼び出し
  - LLM プロバイダー SDK（OpenAI, Anthropic, OpenRouter）
- **Agent Framework**: LangChain / LlamaIndex（検討中）
- **LLM**:
  - Agent Reasoning: OpenAI GPT-4o / DeepSeek v3
  - Embedding: `cl-nagoya/ruri-v3-310m`（ローカル実行）
- **実行環境**: VPS/GCP VM（常駐サーバー）
- **特性**: ステートレス（DuckDB `:memory:` で初期化）

---

## 5. Frontend（モバイル/Web アプリ）

- **Framework**: Kotlin Multiplatform + Compose Multiplatform
- **Language**: Kotlin 2.3
- **Mobile Runtime**: Native Android
- **UI System**: Material3 (Compose)
- **State Management**: MVIKotlin
- **テスト**: Kotest, Turbine
- **実行環境**: モバイル（Android）

詳細: [フロントエンド技術選定](../20.technical_selections/02_frontend.md)

---

## 6. CI/CD

### GitHub Actions

| ワークフロー             | トリガー                  | 用途                    |
| ------------------------ | ------------------------- | ----------------------- |
| `ci-backend.yml`         | `backend/**`, `shared/**` | Backend テスト・Lint    |
| `ci-ingest.yml`          | `ingest/**`, `shared/**`  | Ingest テスト・Lint     |
| `ci-frontend.yml`        | `frontend/**`             | Frontend テスト (JUnit) |
| `job-ingest-spotify.yml` | Cron (1日2回)             | Spotify データ収集      |

### テストツール

- **Python**: pytest, pytest-cov, Ruff (Lint/Format)
- **Frontend**: Kotest, Ktlint, Detekt

---

## 7. Deployment Infrastructure

### 開発環境

- **Python**: uv で依存関係管理（`uv sync`）
- **Frontend**: Gradle で依存関係管理（`./gradlew build`）

### 本番環境（想定）

- **Server**: VPS (Hetzner / Sakura) or GCP VM
- **Storage**:
  - Cloudflare R2: 正本（Parquet/Raw JSON）
  - Local SSD: DuckDB キャッシュ
- **Monitoring**: (未実装)
- **Deployment**: (未実装、将来的に Docker Compose 等)

---

## なぜこの技術スタックか？

### DuckDB + Qdrant のハイブリッド構成

1. **Separation of Concerns**: 分析（集計）と探索（意味検索）を分離
2. **Performance**: DuckDB の列指向処理 + Qdrant の高速ベクトル検索
3. **Simplicity**: ファイルベースで大規模 DWH 不要、個人運用に最適
4. **Cost Effective**: VPS + マネージドサービス（Qdrant Free Tier）で低コスト

### モノレポ + uv workspace

1. **コード共有**: shared/ で型安全なモデル・設定を一元管理
2. **依存関係の透明性**: workspace 依存で ingest/backend の共通基盤を明示
3. **開発効率**: `uv sync` 一発で全 Python パッケージをセットアップ
4. **CI/CD の最適化**: コンポーネント別テストで高速フィードバック

### Mobile First (KMP)

1. **Native Performance**: ネイティブAndroidアプリとしての高速な動作
2. **Type Safety**: Kotlinによる堅牢な型システムと、Backend (Pydantic) との連携
3. **Future Proof**: iOS版も同じコードベース（Compose Multiplatform）で展開可能
