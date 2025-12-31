# EgoGraph Ingest Service

データ収集、変換、Parquetデータレイク構築サービス

## 機能

- **Spotify データ収集**: 視聴履歴の取得（get_recently_played API使用）
- **Parquet Data Lake**: 再生履歴をParquet形式でCloudflare R2に保存
- **Idempotent 設計**: 再実行してもデータが重複しない
- **State Management**: 増分取り込みによる効率的なデータ収集

## Parquet スキーマ

### events/spotify/plays/year=YYYY/month=MM/*.parquet
再生履歴を年月でパーティショニングして保存

| カラム名 | 型 | 説明 |
|---------|---|------|
| play_id | VARCHAR | 決定的ID (played_at_track_id) |
| played_at_utc | TIMESTAMP | 再生日時（UTC） |
| track_id | VARCHAR | SpotifyトラックID |
| track_name | VARCHAR | 曲名 |
| artist_ids | VARCHAR[] | アーティストIDの配列 |
| artist_names | VARCHAR[] | アーティスト名の配列 |
| album_id | VARCHAR | アルバムID |
| album_name | VARCHAR | アルバム名 |
| ms_played | INTEGER | 再生時間（ミリ秒） |
| context_type | VARCHAR | 再生コンテキスト（playlist等） |
| device_name | VARCHAR | デバイス名 |

### raw/spotify/recently_played/YYYY/MM/DD/*.json
Spotify APIの生レスポンスを日付でパーティショニングして保存（監査用）

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

# Cloudflare R2 (S3互換)
R2_ENDPOINT_URL=https://<account-id>.r2.cloudflarestorage.com
R2_ACCESS_KEY_ID=your_access_key_id
R2_SECRET_ACCESS_KEY=your_secret_access_key
R2_BUCKET_NAME=egograph
R2_RAW_PATH=raw/        # オプション（デフォルト: raw/）
R2_EVENTS_PATH=events/  # オプション（デフォルト: events/）
```

### 3. ローカル実行
```bash
# リポジトリルートから
uv run python -m ingest.spotify.main
```

## 自動実行

GitHub Actionsで毎日02:00 UTC（11:00 JST）と14:00 UTC（23:00 JST）に自動実行されます。

ワークフロー: `.github/workflows/job-ingest-spotify.yml`

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
transform.py (クレンジング・変換)
  ↓
storage.py (R2保存)
  ↓------------------↓------------------↓
Raw JSON           Events Parquet    State JSON
(監査用正本)         (分析用構造化)      (増分管理)
  ↓                  ↓
Cloudflare R2 Data Lake
  ↓
DuckDB (Backend: read_parquet で直接クエリ)
```

### データレイク構成 (R2)

```
s3://egograph/
├── raw/spotify/recently_played/YYYY/MM/DD/*.json
│   └── APIレスポンスの正本（監査用）
├── events/spotify/plays/year=YYYY/month=MM/*.parquet
│   └── 分析用の構造化データ（パーティショニング済み）
└── state/spotify_ingest_state.json
    └── 増分取り込み用のカーソル管理
```

DuckDB (Backend) は R2 上の Parquet ファイルを `read_parquet()` で直接読み込むステートレス設計。

### Idempotent設計の詳細

再生履歴の`play_id`は以下のように決定的に生成:

```python
play_id = f"{played_at_utc}_{track_id}"
```

これにより、同じデータを何度取り込んでも Parquet ファイル上でユニークキー制約として機能します。

## トラブルシューティング

### データが取得されない場合

1. Spotify APIの認証情報を確認
2. R2の認証情報と接続を確認
3. ログレベルをDEBUGに設定して詳細を確認:

   ```bash
   LOG_LEVEL=DEBUG uv run python -m ingest.spotify.main
   ```

### テストが失敗する場合

依存関係を再インストール:

```bash
uv sync --all-packages
```

### R2上のデータを確認したい場合

Backend検証スクリプトを使用:

```bash
uv run python backend/scripts/verify_parquet_read.py
```
