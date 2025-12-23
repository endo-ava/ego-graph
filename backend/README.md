# EgoGraph Backend

ハイブリッドBackend: **LLMエージェント機能** + **汎用データアクセスREST API**

## 概要

EgoGraphのBackendは、以下の2つの使い方に対応しています：

1. **LLMエージェント**: チャットでデータを分析・取得
2. **汎用データアクセスAPI**: LLMを介さず直接データを取得

## アーキテクチャ

- **ステートレスDuckDB**: `:memory:`モードでR2のParquetを直接クエリ
- **マルチLLM対応**: OpenAI, Anthropic, OpenRouter に対応
- **MCP風ツール**: LLMがデータにアクセスするためのツール設計

## セットアップ

### 1. 依存関係のインストール

```bash
# プロジェクトルートで
uv sync
```

### 2. 環境変数の設定

`.env` ファイルに以下を追加：

```bash
# Backend Server
BACKEND_HOST=127.0.0.1
BACKEND_PORT=8000
BACKEND_RELOAD=true
BACKEND_API_KEY=  # オプション（空=認証なし）

# LLM Configuration（チャット機能を使う場合のみ）
LLM_PROVIDER=openai  # openai | anthropic | openrouter
LLM_API_KEY=sk-...
LLM_MODEL_NAME=gpt-4o-mini
LLM_TEMPERATURE=0.7
LLM_MAX_TOKENS=2048

# R2 Configuration（必須）
R2_ENDPOINT_URL=https://...
R2_ACCESS_KEY_ID=...
R2_SECRET_ACCESS_KEY=...
R2_BUCKET_NAME=egograph
R2_EVENTS_PATH=events/
```

## 起動方法

```bash
# 開発モード（自動リロード）
uv run python backend/main.py

# または
uv run uvicorn backend.main:app --reload --host 127.0.0.1 --port 8000
```

サーバー起動後、以下にアクセス：
- API Docs: http://localhost:8000/docs
- Health Check: http://localhost:8000/health

## APIエンドポイント

### システム

#### `GET /health`

ヘルスチェック。DuckDB + R2接続を確認。

```bash
curl http://localhost:8000/health
```

**レスポンス例:**
```json
{
  "status": "ok",
  "duckdb": "connected",
  "r2": "accessible",
  "total_plays": 91
}
```

---

### 汎用データアクセスAPI

#### `GET /v1/data/spotify/stats/top-tracks`

指定期間で最も再生された曲を取得。

**パラメータ:**
- `start_date` (required): 開始日（YYYY-MM-DD）
- `end_date` (required): 終了日（YYYY-MM-DD）
- `limit` (optional): 取得数（デフォルト: 10, 最大: 100）

```bash
curl "http://localhost:8000/v1/data/spotify/stats/top-tracks?start_date=2025-12-01&end_date=2025-12-31&limit=5"
```

**レスポンス例:**
```json
[
  {
    "track_name": "Clean.Clean up",
    "artist": "コメティック",
    "play_count": 2,
    "total_minutes": 7.04
  }
]
```

#### `GET /v1/data/spotify/stats/listening`

期間別の視聴統計を取得。

**パラメータ:**
- `start_date` (required): 開始日（YYYY-MM-DD）
- `end_date` (required): 終了日（YYYY-MM-DD）
- `granularity` (optional): 集計単位（"day", "week", "month"、デフォルト: "day"）

---

### チャット（LLMエージェント用）

#### `POST /v1/chat`

LLMエージェントとのチャット。ツールを使ってデータにアクセス。

```bash
curl -X POST http://localhost:8000/v1/chat \
  -H "Content-Type: application/json" \
  -d '{
    "messages": [
      {"role": "user", "content": "2025年12月に最も聴いた曲は？"}
    ]
  }'
```

---

## 実装詳細

### レイヤー構成

```
backend/
├── config.py          # 設定管理（LLM + Backend設定）
├── database/          # DuckDB接続とクエリ
├── tools/             # MCP風ツール（get_top_tracks等）
├── llm/               # LLM統合（OpenAI, Anthropic）
├── api/               # FastAPIエンドポイント
└── main.py            # FastAPIアプリ
```

### 主要な設計判断

1. **ステートレスDuckDB**: `:memory:`で毎回新規接続、R2から直接Parquetを読む
2. **MCP風ツール**: フルMCPサーバーではなく、シンプルなPython関数として実装
3. **プロバイダー非依存**: 手動実装で完全制御（litellm等を使わない）
4. **ハイブリッドAPI**: 会話的なLLMエージェント機能と、直接データアクセス用REST APIの両方を提供

---

## トラブルシューティング

### サーバーが起動しない

1. 依存関係を確認: `uv sync --package egograph-backend`
2. 環境変数を確認: `R2_*` が設定されているか

### データが取得できない

1. ヘルスチェックを確認: `curl http://localhost:8000/health`
2. R2のデータを確認: `uv run python backend/scripts/verify_parquet_read.py`
3. 日付範囲を確認: データが存在する期間でクエリしているか

### LLMチャットが動かない

1. LLM環境変数を確認: `LLM_PROVIDER`, `LLM_API_KEY`, `LLM_MODEL_NAME`
2. APIキーが有効か確認
