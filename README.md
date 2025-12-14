# EgoGraph

**Personal Data Warehouse in a File.**
DuckDBを用いた、プライバシー重視・サーバーレスな個人ログ分析RAGシステム。

## 概要

EgoGraphは、個人のデジタルライフログ（Spotify, Web, Bank, etc.）をローカルファイル（Parquet/DuckDB）に集約し、**高速なSQL分析** と **ベクトル検索** を提供するエージェントシステムです。

## 特徴

- **Hybrid Architecture**: **DuckDB** (分析) と **Qdrant** (検索) のベストミックス構成。
- **Cost Effective**: 安価なVPSと無料のマネージドサービスで動作する、個人に最適な設計。
- **Mobile First**: スマホからいつでも自分のデータにアクセス・対話可能。

## System Architecture

```text
[Mobile App] ──(HTTPS)──▶ [Agent API]
                             │
            ┌────────────────┴────────────────┐
            ▼                                 ▼
   [DuckDB (OLAP)]                  [Qdrant (Vector)]
            │                                 │
     (Scan Parquet)                    (Semantic Search)
```

## 技術スタック

- **Language**: Python 3.13+
- **Database**:
  - **Analytics**: **DuckDB** (Parquet / S3)
  - **Vector**: **Qdrant Cloud** (Managed)
- **Agent**: FastAPI, LangChain, DeepSeek/OpenAI
- **Client**: Capacitor (Mobile/Web)

## Setup

```bash
# Install dependencies
curl -LsSf https://astral.sh/uv/install.sh | sh
uv sync

# Run Ingestion (Spotify example)
uv run python ingest/main.py

# Query Data (CLI)
uv run duckdb
SQL> SELECT * FROM 'data/events/**/*.parquet' LIMIT 5;
```

## Documentation

- [00. Project Overview](./docs/00.project/0001_overview.md)
- [10. System Architecture](./docs/10.architecture/1001_system_architecture.md)
- [Data Model](./docs/10.architecture/1002_data_model.md)
