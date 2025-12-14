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