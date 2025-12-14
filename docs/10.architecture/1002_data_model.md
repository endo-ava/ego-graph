# データモデル (GCS / DuckDB / Qdrant)

## 1. 責務の分離 (3層モデル)

データの保管場所と役割を以下の3つに明確に分離する。

1.  **GCS (Google Cloud Storage) or 個人NAS**: **正本 (Original)**
    - データの「原本」置き場。テキスト本文やログの実体はここに置く。
2.  **DuckDB**: **台帳 (Ledger) & 運用 (Ops)**
    - データの「所在」と「状態」を管理するカタログ。
    - 「昨日の曲」のような決定論的なクエリ（集計・分析）を担当。
3.  **Qdrant**: **索引 (Index)**
    - 意味検索（ベクトル検索）のためのディクショナリ。
    - 正本は持たず、IDと検索に必要な最小限のタグのみを持つ。

---

## 2. Layer 1: 正本 (GCS) - "The Bookshelf"

ソースデータから抽出・加工された実データ（Parquet形式推奨）の永続化場所。

- **Raw Documents**:
    - 収集したドキュメントの原文（Markdown, Text, HTMLなど）。
    - Path: `gs://{bucket}/docs/raw/{source}/{doc_id}.{ext}`
- **Document Chunks**:
    - RAG用に分割されたテキストチャンク（Parquet）。
    - Path: `gs://{bucket}/docs/chunks/{doc_id}.parquet`
- **Spotify Archives**:
    - 再生履歴の事実データ（Parquet）。
    - Path: `gs://{bucket}/spotify/events/year={yyyy}/month={mm}/{uuid}.parquet`

---

## 3. Layer 2: 台帳 (DuckDB) - "The Catalog"

実データへの参照（パス）、メタデータ、運用状態を管理する。LLMやAPIはこの層を通じてデータにアクセスする。

### 3.1 Schema Layers

データの用途に応じて3つのスキーマを使い分ける。

```sql
CREATE SCHEMA IF NOT EXISTS raw;   -- 取り込み直後（ほぼ生）
CREATE SCHEMA IF NOT EXISTS mart;  -- API/LLMが使う整形済み
CREATE SCHEMA IF NOT EXISTS ops;   -- ingest状態・ログ
```

### 3.1 管理データ (Meta & Ops)

- **Documents Ledger (`mart.documents`)**
    - **役割**: ドキュメントの管理台帳。
    - **主な項目**: `doc_id`, `title`, `uri` (GCS path), `hash` (変更検知用), `updated_at`.
- **Ingest State (`ops.ingest_state`)**
    - **役割**: 取り込み処理の進捗管理（カーソル）。
    - **主な項目**: `source`, `cursor_value` (timestamp/token), `status`.

### 3.2 分析・参照データ (Analytics & Lookup)

- **Spotify History (`mart.spotify_plays`)**
    - **役割**: 履歴の検索・集計用ビュー。GCS上のParquetを参照。
    - **主な項目**: `play_id`, `track_name`, `played_at`, `artist_names`.
- **Daily Summaries (`mart.daily_summaries`)**
    - **役割**: エージェントが生成した日次サマリーの正本（テキスト）。
    - **主な項目**: `summary_id`, `date`, `summary_text`, `stats_json`.

---

## 4. Layer 3: 索引 (Qdrant) - "The Index Cards"

意味検索を行い、候補となるIDリストを返すことに特化する。**本文（Text）は保持しない。**

### 4.1 Document Index (`doc_chunks_v1`)
- **Vector**: チャンクテキストの埋め込み。
- **Payload**: フィルタリング用メタデータのみ。
    - `type`: "doc_chunk"
    - `doc_id`: UUID
    - `lang`: "ja"
    - `source`: "drive", "notion" etc.
    - `tags`: ["topic:RAG"]

### 4.2 Summary Index (`daily_summaries_v1`)
- **Vector**: サマリーテキストの埋め込み。
- **Payload**: 日付検索用メタデータ。
    - `type`: "daily_summary"
    - `summary_id`: UUID
    - `date`: "YYYY-MM-DD"
    - `mood`: ["focus", "chill"]
