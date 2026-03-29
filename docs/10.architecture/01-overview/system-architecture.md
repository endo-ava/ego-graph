# システムアーキテクチャ

## Overview

EgoGraph は、個人ライフログを収集・保存・分析するためのデータ基盤と Agent API を中心に構成する。
terminal/runtime 系の実行基盤は 2026-03 時点で **Plexus** 側へ移管済みであり、このリポジトリでは保持しない。

![Architecture Diagram](../diagrams/architecture_diagram.png)

```mermaid
flowchart TB
    classDef client fill:#e3f2fd,stroke:#1565c0,stroke-width:2px,color:black,rx:10,ry:10;
    classDef server fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px,color:black,rx:5,ry:5;
    classDef storage fill:#fff3e0,stroke:#ef6c00,stroke-width:2px,color:black,shape:cyl;
    classDef external fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px,color:black,rx:5,ry:5;

    subgraph ClientLayer ["📱 Client Layer"]
        MobileApp["Mobile App\n(Android / KMP)"]:::client
    end

    subgraph CloudEnv ["☁️ Cloud / Server Environment"]
        subgraph AppLayer ["Application Server (VPS/VM)"]
            AgentAPI["Agent API\n(FastAPI)"]:::server
            DuckDB[("DuckDB\n(Analytics Engine)")]:::server
        end

        subgraph IngestionLayer ["GitHub Actions (Async Batch)"]
            GHA["Ingestion Workers\n(Python Scripts)"]:::server
        end
    end

    subgraph ManagedServices ["Managed Services"]
        Qdrant[("Qdrant Cloud\n(Vector Database)")]:::external
        R2{{"Cloudflare R2\n(Object Storage)"}}:::storage
        FCM["Firebase Cloud Messaging\n(FCM)"]:::external
        Plexus["Plexus\n(Terminal / Runtime)"]:::external
    end

    subgraph DataSources ["🌐 External Data Sources"]
        Spotify["Spotify API"]:::external
        Docs["Google Drive / Notion"]:::external
    end

    MobileApp <==>|"HTTPS / REST"| AgentAPI
    MobileApp -.->|"Terminal / Runtime access"| Plexus
    MobileApp <-->|"Push Token"| FCM

    AgentAPI <-->|"SQL Query"| DuckDB
    AgentAPI <-->|"Vector Search"| Qdrant
    DuckDB -.->|"Read Parquet (httpfs)"| R2

    GHA -->|"Fetch Data"| Spotify
    GHA -->|"Fetch Data"| Docs
    GHA -->|"Write Parquet/Raw"| R2
    GHA -->|"Upsert Vectors"| Qdrant
```

## コンポーネント

### Ingestion

- GitHub Actions 上で定期実行される ETL/ELT パイプライン
- 外部 API から取得したデータを Raw JSON / Parquet として R2 に保存する

### Backend

- FastAPI ベースの Agent API
- DuckDB と Qdrant を組み合わせて分析・検索・応答生成を行う

### Frontend

- Kotlin Multiplatform + Compose Multiplatform の Android アプリ
- この repo には terminal 関連 UI 実装が残っているが、対応する runtime の所有権は Plexus 側にある

### Plexus

- tmux terminal access、push/webhook、将来の worker orchestration を担う runtime repository
- EgoGraph からは別リポジトリ境界として扱う

## 責務境界

- EgoGraph:
  - 個人データの収集、保存、分析
  - Agent API とライフログ活用
  - モバイルアプリ本体
- Plexus:
  - terminal/runtime 実装
  - tmux セッション接続
  - runtime 系の push / webhook / orchestration

## Notes

- Last.fm 連携は一時停止中。
- terminal/runtime 機能の実装修正は EgoGraph ではなく Plexus 側で管理する。
