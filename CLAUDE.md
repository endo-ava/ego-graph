# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## プロジェクト概要

EgoGraphは、個人データ統合RAGシステム。Spotify等の複数ソースからデータ収集し、プライバシー優先で「分身RAG」を構築。

現在のMVPではSpotifyの視聴履歴・プレイリストを収集し、ローカル埋め込みモデル（cl-nagoya/ruri-v3-310m）でベクトル化してQdrant Cloudに保存。

## 開発コマンド

### パッケージ管理（uv使用）

```bash
# 全依存関係の同期
uv sync

# 特定パッケージのみ
uv sync --package egograph-ingest

# Spotify取り込みパイプライン実行
uv run --package egograph-ingest python ingest/main.py
```

### テスト

```bash
# 全テスト実行
uv run pytest

# 特定テストファイル
uv run pytest ingest/tests/test_etl.py

# カバレッジ付き
uv run pytest --cov=ingest/src --cov=shared
```

### コード品質

```bash
uv run black .        # フォーマット
uv run flake8 .       # リント
uv run mypy .         # 型チェック
```

## アーキテクチャ

### モノレポ構成

- **`shared/`**: 共通パッケージ（`egograph`）- データモデル、設定、ユーティリティ
- **`ingest/`**: データ収集サービス（GitHub Actions実行）
- **`backend/`**: FastAPI（将来用プレースホルダ）
- **`frontend/`**: Next.js（将来用プレースホルダ）

### データフロー

```
Spotify API → Collector → Transformer (UnifiedDataModel)
→ ETL (LlamaIndex) → Embedder (ローカルモデル 768次元) → Qdrant Cloud
```

### 統一データモデル（Lexia標準スキーマ）

全データソースは`UnifiedDataModel`（`shared/models.py`）に変換:

- **`id`**: UUID v4
- **`source`**: spotify/youtube/browser等
- **`type`**: music/video/purchase等
- **`timestamp`**: ISO8601日時
- **`raw_text`**: 検索用テキスト（Semantification済み）
- **`metadata`**: ソース固有メタデータ（`original_data`に生データJSON保存）
- **`embedding`**: ベクトル（ローカルモデル生成）
- **`sensitivity`**: low/medium/high（プライバシーレベル）
- **`nsfw`**: NSFW判定フラグ

設計のポイント:
- **二層構造**: `raw_text`で検索、`metadata.original_data`で正確な回答生成
- **粒度管理**: atomic/summary/chunkで検索効率化
- **date_bucket**: 日付フィルタ高速化用

### 設定管理

`shared/config.py`で一元管理（Pydantic使用）:
- **SpotifyConfig**: API認証情報
- **EmbeddingConfig**: ローカルモデル設定
- **QdrantConfig**: Qdrant Cloud接続設定

## 重要な実装ポイント

### ワークスペース依存関係

- `shared/`（`egograph`）は`ingest/`と`backend/`からワークスペース依存関係として参照
- `shared/`の変更は即座に反映（再インストール不要）

### uvでのコード実行

必ず`uv run --package`を使用:

```bash
# 正しい
uv run --package egograph-ingest python ingest/main.py

# 誤り（ワークスペース依存関係が解決されない可能性）
python ingest/main.py
```

### 環境変数

ローカルテスト用（`.env`ファイル）:

```bash
# Spotify API（必須）
SPOTIFY_CLIENT_ID=...
SPOTIFY_CLIENT_SECRET=...
SPOTIFY_REFRESH_TOKEN=...

# ローカル埋め込みモデル（オプション、デフォルト値あり）
EMBEDDING_MODEL_NAME=cl-nagoya/ruri-v3-310m
EMBEDDING_BATCH_SIZE=32
EMBEDDING_DEVICE=cuda  # またはcpu, mps
EMBEDDING_DIMENSION=768

# Qdrant Cloud（必須）
QDRANT_URL=https://....qdrant.io
QDRANT_API_KEY=...
QDRANT_COLLECTION_NAME=egograph_spotify_ruri
QDRANT_VECTOR_SIZE=768
```

GitHub ActionsではGitHub Secretsとして設定（`docs/90.plan/mvp_spotify/03.github_secrets.md`参照）。

### GitHub Actions

- **ワークフロー**: `.github/workflows/spotify_ingest.yml`
- **スケジュール**: 毎日02:00 UTC（日本時間11:00）
- **手動実行**: workflow_dispatchで可能

### ローカル埋め込み生成

`ingest/pipeline/embeddings.py`でローカルモデル使用（外部API不使用）:

- **モデル**: cl-nagoya/ruri-v3-310m（日本語最適化、768次元）
- **フレームワーク**: sentence-transformers
- **デバイス**: CPU/CUDA/MPS対応

利点: APIコスト不要、プライバシー保護、バッチ処理高速化

## 主要ファイル

### コアモデル
- `shared/models.py`: UnifiedDataModelとEnum定義
- `shared/config.py`: 設定管理

### 取り込みパイプライン
- `ingest/main.py`: メインパイプライン（5ステップ）
- `ingest/spotify/collector.py`: Spotify API収集
- `ingest/spotify/transformer.py`: Spotify→UnifiedDataModel変換
- `ingest/pipeline/etl.py`: LlamaIndex ETL処理
- `ingest/pipeline/embeddings.py`: ローカル埋め込み生成
- `ingest/pipeline/storage.py`: Qdrant保存

### ドキュメント
- `docs/10.architecture/1001_system_architecture.md`: システム設計全体
- `docs/10.architecture/1002_data_model.md`: Lexiaスキーマ詳細
- `docs/90.plan/mvp_spotify/00.plan.md`: MVP実装計画

## Obsidianの活用

技術的な学びなどのナレッジや、複雑な問題が解決できた場合、知識財産としてObsidianにまとめます。
`~/myVault` がObsidianのデータディレクトリです。

## 将来拡張

Phase 2以降で追加データソース対応予定（YouTube、銀行取引、Amazon、Gmail等）。各ソースは`ingest/<source>/`に実装し、同じパイプラインフローに従う。
