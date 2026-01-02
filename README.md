# EgoGraph

**Personal Data Warehouse in a File.**
DuckDB を用いた、プライバシー重視・サーバーレスな個人ログ分析 RAG システム。

## 概要

EgoGraph は、個人のデジタルライフログ（Spotify, Web, Bank, etc.）をローカルファイル（Parquet/DuckDB）に集約し、**高速な SQL 分析** と **ベクトル検索** を提供するエージェントシステムです。

## 特徴

- **Hybrid Architecture**: **DuckDB** (分析) と **Qdrant** (検索) のベストミックス構成。
- **Data Enrichment**: 外部 API と連携し、個人のログに豊かなコンテキストを付与。
- **Cost Effective**: 安価な VPS と無料のマネージドサービスで動作する、個人に最適な設計。
- **Mobile First**: スマホからいつでも自分のデータにアクセス・対話可能。

## System Architecture

![Architecture Diagram](./docs/10.architecture/diagrams/architecture_diagram.png)

詳細: [システムアーキテクチャ](./docs/10.architecture/1001_system_architecture.md)

---

## モノレポ構成

このプロジェクトは、Python (uv workspace) + Node.js (npm) のモノレポです。

```text
ego-graph/
├── shared/                # 共有Pythonライブラリ（uv workspace メンバー）
├── ingest/                # データ収集ワーカー（uv workspace メンバー）
├── backend/               # FastAPI サーバー（uv workspace メンバー）
├── frontend/              # React + Capacitor アプリ（npm 独立パッケージ）
│
├── docs/                  # プロジェクトドキュメント
├── .github/workflows/     # CI/CD ワークフロー
├── pyproject.toml         # Python workspace 設定
└── uv.lock                # Python 依存関係ロック
```

### コンポーネント概要

| コンポーネント | 役割 | 技術スタック | 実行環境 |
|--------------|------|------------|---------|
| **shared/** | 共有ライブラリ（モデル、設定） | Python 3.13, Pydantic | ライブラリ |
| **ingest/** | データ収集・変換・保存 | Python 3.13, Spotipy, DuckDB, boto3 | GitHub Actions (定期実行) |
| **backend/** | Agent API・データアクセス | FastAPI, DuckDB, LLM (DeepSeek/OpenAI) | VPS/GCP (常駐サーバー) |
| **frontend/** | チャット UI | React 19, Capacitor 8, TanStack Query | モバイル/Web (Vite) |

---

## Quick Start

### 前提条件

- **Python**: 3.13+ ([uv](https://github.com/astral-sh/uv) 推奨)
- **Node.js**: 20+ (frontend のみ)
- **環境変数**: `.env.example` を参考に `.env` を作成

### 1. 全体セットアップ

```bash
# uv のインストール（未インストールの場合）
curl -LsSf https://astral.sh/uv/install.sh | sh

# Python 依存関係の同期（shared, ingest, backend を一括）
uv sync

# Frontend 依存関係のインストール
cd frontend && npm install && cd ..
```

### 2. コンポーネント別セットアップ

#### A. Ingest（データ収集）

```bash
# Spotify から最近の再生履歴を取得し、R2 に保存
uv run python -m ingest.spotify.main

# テスト実行
uv run pytest ingest/tests --cov=ingest
```

**必要な環境変数**:
- `SPOTIFY_CLIENT_ID`, `SPOTIFY_CLIENT_SECRET`, `SPOTIFY_REFRESH_TOKEN`
- `R2_ENDPOINT_URL`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_BUCKET_NAME`

#### B. Backend（API サーバー）

```bash
# 開発サーバー起動（自動リロード）
uv run uvicorn backend.main:app --reload --host 127.0.0.1 --port 8000

# ヘルスチェック
curl http://localhost:8000/health

# API ドキュメント
open http://localhost:8000/docs
```

**必要な環境変数**:
- `R2_*`（データアクセス）
- `LLM_*`（チャット機能）

#### C. Frontend（モバイル/Web アプリ）

```bash
cd frontend

# Web 開発サーバー
npm run dev
# -> http://localhost:5174

# テスト実行
npm run test:run

# Android ビルド（Capacitor）
npm run build
npm run android:sync
npm run android:open  # Android Studio が開く
```

**必要な環境変数**:
- `VITE_API_URL=http://localhost:8000`

---

## Development

### テスト実行

```bash
# Python 全テスト
uv run pytest

# コンポーネント別
uv run pytest ingest/tests --cov=ingest
uv run pytest backend/tests --cov=backend

# Frontend
cd frontend && npm run test:run
```

### Lint & Format

```bash
# Python (Ruff)
uv run ruff check .          # チェックのみ
uv run ruff format .         # フォーマット
uv run ruff check --fix .    # 自動修正

# Frontend (ESLint)
cd frontend && npm run lint
```

### CI/CD

GitHub Actions でコンポーネント別に自動テストが実行されます。

- **ci-backend.yml**: `backend/`, `shared/` の変更時
- **ci-ingest.yml**: `ingest/`, `shared/` の変更時
- **ci-frontend.yml**: `frontend/` の変更時
- **job-ingest-spotify.yml**: 1日2回（02:00, 14:00 UTC）定期実行

---

## Documentation

### コンポーネント詳細

- **[Shared](./shared/README.md)**: 共有ライブラリ（モデル、設定、ユーティリティ）
- **[Ingest](./ingest/README.md)**: データ収集ワーカー、R2 ストレージロジック
- **[Backend](./backend/README.md)**: Agent API、DuckDB 接続、LLM 統合
- **[Frontend](./frontend/README.md)**: モバイル/Web アプリケーション

### アーキテクチャ & 設計

- **[プロジェクト概要](./docs/00.project/0001_overview.md)**: ビジョンと目標
- **[システムアーキテクチャ](./docs/10.architecture/1001_system_architecture.md)**: 全体構成とデータフロー
- **[データモデル](./docs/10.architecture/1002_data_model.md)**: スキーマ定義
- **[技術スタック](./docs/10.architecture/1004_tech_stack.md)**: 技術選定理由
- **[技術選定記録 (ADR)](./docs/20.technical_selections/README.md)**: 設計判断の記録
- **[開発ルール](./docs/30.dev_practices/README.md)**: コーディング規約、テスト戦略

### 開発者向けガイド

詳細な開発ガイドラインは [CLAUDE.md](./CLAUDE.md) を参照してください。

