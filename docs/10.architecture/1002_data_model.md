# データモデル (Cloudflare R2 / DuckDB / Qdrant)

## 1. 責務の分離 (3層モデル)

データの保管場所と役割を以下の3つに明確に分離する。

1.  **Cloudflare R2 (Object Storage)**: **正本 (Original)**
    - データの「原本」置き場。テキスト本文やログの実体はここに置く。
2.  **DuckDB**: **台帳 (Ledger) & 運用 (Ops)**
    - データの「所在」と「状態」を管理するカタログ。R2 上の Parquet を `VIEW` (Mart) として定義し、クエリを可能にする。
    - 「昨日の曲」のような決定論的なクエリ（集計・分析）を担当。
3.  **Qdrant**: **索引 (Index)**
    - 意味検索（ベクトル検索）のためのディクショナリ。
    - 正本は持たず、IDと検索に必要な最小限のタグのみを持つ。

---

## 2. Layer 1: 正本 (Cloudflare R2) - "The Bookshelf"

ソースデータから抽出・加工された実データ（Parquet形式推奨）の永続化場所。

- **R2 Directory Structure**:
  ```text
  s3://{bucket}/
  ├── events/          # 時系列データ (Analytics / Recall)
  │   └── spotify/
  │       └── plays/   # Spotify 再生ログ (year={yyyy}/month={mm}/...)
  ├── master/          # 非時系列・マスタデータ (Enrichment)
  │   └── spotify/     # Spotify マスター (tracks/, artists/)
  ├── raw/             # 生データ (Audit / Reprocessing)
  │   └── spotify/     # API レスポンス (JSON)
  └── state/           # 進捗管理 (Cursors)
      ├── spotify_ingest_state.json
      └── lastfm_ingest_state.json  # Archived
  ```

- **Spotify Archives**:
  - 再生履歴の事実データ（Parquet）。
  - Path: `s3://{bucket}/events/spotify/plays/year={yyyy}/month={mm}/{uuid}.parquet`
- **Spotify Master**:
  - 楽曲・アーティストのマスターデータ（Parquet）。
  - Track Path: `s3://{bucket}/master/spotify/tracks/year={yyyy}/month={mm}/{uuid}.parquet`
  - Artist Path: `s3://{bucket}/master/spotify/artists/{uuid}.parquet`
- **Last.fm**:
  - Deprecated。ジョブ停止中のため新規データは投入しない。

- **State Management**:
  - インジェストの進捗管理ファイル。
  - Path: `s3://{bucket}/state/{source}_ingest.json`

---

## 3. Layer 2: 台帳 (DuckDB) - "The Catalog"

実データへの参照（パス）、メタデータ、運用状態を管理する。LLMやAPIはこの層を通じてデータにアクセスする。
**この層の `mart` スキーマは R2 上のフォルダ構造ではなく、DuckDB 内の論理的なビュー構成である。**

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
  - **主な項目**: `doc_id`, `title`, `uri` (S3 path), `hash` (変更検知用), `updated_at`.
- **Ingest State (`ops.ingest_state`)**
  - **役割**: 取り込み処理の進捗管理（カーソル）。
  - **主な項目**: `source`, `cursor_value` (timestamp/token), `status`.

### 3.2 分析・参照データ (Analytics & Lookup)

- **Spotify History (`mart.spotify_plays`)**
  - **役割**: 履歴の検索・集計用ビュー。R2上のParquetを参照。
  - **主な項目**: `play_id`, `track_name`, `played_at`, `artist_names`.
- **Spotify Master (`mart.spotify_tracks`, `mart.spotify_artists`)**
  - **役割**: 楽曲・アーティストの詳細属性（genres, popularity 等）。
  - **主な項目**: `track_id`, `name`, `genres`, `popularity`, `followers_total`.
- **Spotify Enriched (`mart.spotify_plays_enriched`)**
  - **役割**: 再生履歴にマスターデータを付与した分析用ビュー。
  - **主な項目**: `play_id`, `played_at_utc`, `track_id`, `genres`, `preview_url`.
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
