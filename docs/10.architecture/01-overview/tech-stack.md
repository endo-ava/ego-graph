# 技術スタック

モノレポ構成における各コンポーネントの技術選定と、その責務分担をまとめる。
terminal/runtime 系の実行基盤は **Plexus** 側へ移管済みであり、EgoGraph の workspace には含めない。

---

## モノレポ構成

| コンポーネント | 言語/FW | パッケージマネージャー | 主要ライブラリ |
| -------------- | ------- | ---------------------- | -------------- |
| **ingest/** | Python 3.13 | uv | Spotipy, requests, DuckDB, boto3, pyarrow |
| **backend/** | Python 3.13 | uv | FastAPI, Uvicorn, DuckDB |
| **frontend/** | Kotlin 2.2.21 | Gradle | Compose Multiplatform, Voyager, Koin, Ktor, FCM |

- **Python Workspace**: uv で ingest, backend を一元管理
- **Frontend**: Kotlin Multiplatform (Gradle)
- **Runtime**: terminal/runtime 実装は Plexus 側で管理

---

## 1. Data Storage

### DuckDB

- **用途**: SQL 分析、集計、台帳管理
- **実行モード**: `:memory:` を中心としたステートレス利用
- **理由**: Parquet を直接扱えて、個人運用でも高い集計性能を出せる

### Qdrant Cloud

- **用途**: 意味検索、RAG インデックス
- **理由**: ベクトル検索をマネージドで分離できる

### Cloudflare R2

- **用途**: Parquet / Raw JSON の正本保存
- **理由**: S3 互換、egress 無料、Parquet-first 戦略と相性が良い

---

## 2. Ingest

- **Language**: Python 3.13
- **実行環境**: GitHub Actions
- **主要ライブラリ**:
  - `spotipy`
  - `requests`
  - `pyarrow`
  - `boto3`
  - `duckdb`
- **特性**: Idempotent、Stateful な増分取り込み

---

## 3. Backend

- **Framework**: FastAPI
- **Web Server**: Uvicorn
- **主要ライブラリ**:
  - `duckdb`
  - `httpx`
  - LLM provider SDK
- **実行環境**: VPS / GCP VM
- **特性**: ステートレスな Agent API

---

## 4. Frontend

- **Framework**: Kotlin Multiplatform + Compose Multiplatform
- **Language**: Kotlin 2.2.21
- **Navigation**: Voyager
- **State Management**: StateFlow + Channel
- **DI**: Koin
- **HTTP Client**: Ktor
- **Push Notification**: Firebase Cloud Messaging (FCM)
- **テスト**: kotlin-test, Turbine, MockK, Ktor MockEngine

補足:

- `frontend/**` には terminal 関連 UI が残っているが、対応する runtime 実装はこの repo では保持しない
- terminal/runtime の所有権は Plexus 側にある

---

## 5. CI/CD

| ワークフロー | トリガー | 用途 |
| ------------ | -------- | ---- |
| `ci-backend.yml` | `backend/**` | Backend テスト・Lint |
| `ci-ingest.yml` | `ingest/**` | Ingest テスト・Lint |
| `ci-frontend.yml` | `frontend/**` | Frontend テスト |
| `job-ingest-spotify.yml` | Cron | Spotify データ収集 |
| `job-ingest-github.yml` | Cron | GitHub データ収集 |

---

## 6. Deployment

- **Backend**: VPS / VM 常駐
- **Ingest**: GitHub Actions 定期実行
- **Frontend**: Android ビルド・配布
- **Runtime**: Plexus 側で別運用

---

## なぜこの構成か

### データ基盤と runtime を分離する

1. EgoGraph は個人データ基盤と Agent API に集中する
2. terminal/runtime は tmux や push/webhook など別種の運用責務を持つ
3. repo 境界を分けることで、設計判断とデプロイ経路を明確にできる

### モノレポ + uv workspace

1. Python 側の依存関係を一括管理できる
2. ingest と backend の責務境界を保ちながら開発効率を維持できる
3. component-based な CI を組みやすい
