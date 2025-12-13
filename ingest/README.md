# EgoGraph Ingest Service

データ収集、変換、ストレージサービス

## 機能

- Spotifyデータ収集（視聴履歴、プレイリスト）
- 統一スキーマへのデータ変換
- LlamaIndex ETLパイプライン
- ローカル埋め込み生成（ruri-v3-310m）
- Qdrantベクトルストレージ

## セットアップ

1. 依存関係のインストール:
```bash
uv sync --package egograph-ingest
```

2. 環境変数の設定（ルートの `.env.example` を参照）

3. 実行（リポジトリルートから）:
```bash
uv run --package egograph-ingest python ingest/main.py
```

## 自動実行

GitHub Actionsで毎日02:00 UTC（11:00 JST）に自動実行されます。

## テスト

```bash
pytest
# カバレッジ付き
pytest --cov=src tests/
```
