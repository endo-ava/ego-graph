# 技術スタック (DuckDB + Qdrant)

## 1. Core Database Engine
- **DuckDB**: **OLAP (分析) 専用エンジン**。
  - **Extension: `parquet`**: ローカルのParquetファイル群に対してSQLを実行する。
  - **Extension: `httpfs`**: Cloudflare R2 (S3互換) 上のデータを参照するために使用。
- **Qdrant Cloud**: **ベクトル検索専用エンジン**。
  - **Free Tier**: 1GBメモリまで無料（約10万ベクトル※次元数に依存）。
  - **役割**: 日次要約やチャンクの意味検索を担当し、BEサーバーのメモリ負荷を下げる。

## 2. Ingestion Pipeline
- **Language**: **Python 3.13+**
- **Libraries**:
  - `pandas` / `polars`: データ加工用。DuckDBとのZero-Copy連携が強力。
  - `spotipy`: Spotify APIクライアント。
  - `playwright`: Webスクレイピング。

## 3. Application / Agent Layer
- **Framework**: **FastAPI** (Python)
  - 軽量なREST APIサーバーとして、Agentのインターフェースを提供。
- **Agent Framework**: **LangChain** or **LlamaIndex**
  - Toolとして `DuckDBQueryTool` を定義し、LLMにSQLを書かせる。
- **LLM**:
  - **Agent Reasoning**: OpenAI GPT-4o / DeepSeek v3.
  - **Embedding**: `cl-nagoya/ruri-v3-310m` (Local execution via `sentence-transformers`).

## 4. Frontend / Client
- **Mobile/Web App**: **Capacitor** (Cross-platform).
  - ユーザーインターフェースの主役。
  - チャット、ダッシュボード
  - **詳細**: [フロントエンド技術選定](../20.technical_selections/02_frontend.md)

## 5. Deployment Infrastructure
- **Server**: **VPS (Hetzner / Sakura)** or **Cloud VM (AWS EC2 / GCP Compute)**.
  - Docker Compose で Agent + Ingestion Worker を同居させる。
- **Storage**:
  - **Object Storage (Cloudflare R2)**: **正本 (Original)**。生データとParquetファイルの主格納場所。
  - **Local SSD (Volume)**: **Cache & Ledger**。DuckDBファイルと、頻繁にアクセスするParquetのキャッシュ。

---

## 理由: なぜ DuckDB + Qdrant か？

1.  **Separation of Concerns**: 分析（集計）と探索（意味検索）のワークロードを分離。重いベクトル検索をマネージドサービス（Qdrant）に逃がすことで、Agentサーバーを軽量に保てる。
2.  **Performance**: DuckDBは列指向処理により、大規模データの集計が非常に高速。QdrantはRust製の専用エンジンでベクトル検索が高速。
3.  **Simplicity**: 大掛かりなDWHを構築せずとも、ファイルベースでお手軽に本格的なOLAP環境が手に入る。個人運用に最適。
