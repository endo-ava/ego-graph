# EgoGraph Ingest Service

データ収集、変換、DuckDBストレージサービス

## 機能

- **Spotify データ収集**: 視聴履歴の取得（get_recently_played API使用）
- **DuckDB ストレージ**: 再生履歴と楽曲マスタの永続化
- **Cloudflare R2**: DuckDBファイルのバックアップ・同期
- **Idempotent 設計**: 再実行してもデータが重複しない

## DuckDB スキーマ

### raw.spotify_plays（再生履歴）
1再生=1行、視聴履歴の正本として機能

```sql
CREATE TABLE raw.spotify_plays (
    play_id VARCHAR PRIMARY KEY,        -- 決定的ID (played_at_track_id)
    played_at_utc TIMESTAMP NOT NULL,
    track_id VARCHAR NOT NULL,
    track_name VARCHAR NOT NULL,
    artist_ids VARCHAR[],
    artist_names VARCHAR[],
    album_id VARCHAR,
    album_name VARCHAR,
    ms_played INTEGER,
    context_type VARCHAR,
    device_name VARCHAR,
    inserted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### mart.spotify_tracks（楽曲マスタ）
1曲=1行、楽曲メタデータの整形済みテーブル

```sql
CREATE TABLE mart.spotify_tracks (
    track_id VARCHAR PRIMARY KEY,
    name VARCHAR NOT NULL,
    artist_ids VARCHAR[],
    artist_names VARCHAR[],
    album_id VARCHAR,
    album_name VARCHAR,
    duration_ms INTEGER,
    popularity INTEGER,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## セットアップ

### 1. 依存関係のインストール
```bash
cd /path/to/ego-graph
uv sync --all-packages
```

### 2. 環境変数の設定
`.env`ファイルにSpotify APIとCloudflare R2の認証情報を設定:

```bash
# Spotify API
SPOTIFY_CLIENT_ID=your_client_id
SPOTIFY_CLIENT_SECRET=your_secret
SPOTIFY_REFRESH_TOKEN=your_refresh_token

# DuckDB設定
DUCKDB_PATH=data/analytics.duckdb

# Cloudflare R2 (S3互換)
R2_ENDPOINT_URL=https://<account-id>.r2.cloudflarestorage.com
R2_ACCESS_KEY_ID=your_access_key_id
R2_SECRET_ACCESS_KEY=your_secret_access_key
R2_BUCKET_NAME=egograph
R2_KEY_PREFIX=duckdb/
```

### 3. ローカル実行
```bash
# リポジトリルートから
uv run python ingest/spotify_duckdb_main.py
```

## 自動実行

GitHub Actionsで毎日02:00 UTC（11:00 JST）と14:00 UTC（23:00 JST）に自動実行されます。

ワークフロー: `.github/workflows/job-spotify-ingest.yml`

## テスト

```bash
# すべてのテスト実行
cd ingest
uv run pytest tests/ -v

# カバレッジ付き
uv run pytest tests/ --cov=ingest --cov-report=html

# 統合テストをスキップ
uv run pytest tests/ -m "not integration"
```

## アーキテクチャ

```text
Spotify API (最新50件)
  ↓
collector.py (データ収集)
  ↓
DuckDB Writer (upsert)
  ↓
data/analytics.duckdb (ローカルDB)
  ↓
R2 Sync (永続化)
  ↓
Cloudflare R2 (duckdb/analytics.duckdb)
```

### Idempotent設計の詳細

再生履歴の`play_id`は以下のように決定的に生成:

```python
play_id = f"{played_at_utc}_{track_id}"
```

これにより、同じデータを何度取り込んでも`INSERT OR REPLACE`で安全にupsertできます。

## トラブルシューティング

### DuckDBファイルが壊れた場合
R2から最新のバックアップをダウンロードして復元:

```bash
# R2から手動ダウンロード（boto3使用）
python -c "from ingest.spotify.r2_sync import R2Sync; ..."
```

### テストが失敗する場合
依存関係を再インストール:

```bash
uv sync --all-packages
```
