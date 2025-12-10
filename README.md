# EgoGraph

プライバシーを重視した個人データ統合RAG（Retrieval-Augmented Generation）システム

## 概要

EgoGraphは、Spotify、YouTube、ブラウザ履歴、金融データなど、あらゆる個人データを統合し、プライバシーを最優先にした「分身RAG」を自動構築・運用するシステムです。

### MVP（Minimum Viable Product）

現在のMVPでは、Spotifyの視聴履歴とプレイリストデータの収集・埋め込み生成・ベクトルストレージを実装しています。

## 特徴

- **プライバシー優先**: データは暗号化され、機密情報は適切に分類・保護
- **自動収集**: GitHub Actionsによる毎日の自動データ収集
- **ベクトル検索**: Qdrant Cloudによる高速なセマンティック検索
- **スケーラブル**: モジュラー設計で新しいデータソースを簡単に追加可能

## アーキテクチャ

```
Spotify API → Collector → Transformer → ETL (LlamaIndex) → Embeddings (Nomic) → Qdrant Cloud
```

### 技術スタック

- **Python 3.13**: メイン言語
- **Spotipy**: Spotify API連携
- **LlamaIndex**: ETLパイプライン
- **Nomic**: 埋め込み生成（768次元）
- **Qdrant Cloud**: ベクトルデータベース
- **GitHub Actions**: 自動実行オーケストレーション

## セットアップ

### 前提条件

- Python 3.13以上
- GitHubアカウント
- Spotifyアカウント（無料でOK）

### 1. APIクレデンシャルの取得

詳細な手順は [docs/plan/02.api_keys.md](./docs/plan/02.api_keys.md) を参照してください。

必要なクレデンシャル:
- Spotify Client ID、Client Secret、Refresh Token
- Nomic API Key
- Qdrant Cloud URL、API Key

### 2. リポジトリのクローン

```bash
git clone https://github.com/yourusername/ego-graph.git
cd ego-graph
```

### 3. 依存関係のインストール

このプロジェクトはパッケージ管理に [uv](https://github.com/astral-sh/uv) を使用しています。

```bash
# uvのインストール (未インストールの場合)
curl -LsSf https://astral.sh/uv/install.sh | sh

# 依存関係の同期（全パッケージ）
uv sync
```

### 4. 環境変数の設定

ローカルテスト用:
```bash
cp .env.example .env
# .env ファイルを編集してクレデンシャルを入力
```

GitHub Actions用:
- [docs/plan/03.github_secrets.md](./docs/plan/03.github_secrets.md) を参照してGitHub Secretsを設定

### 5. ローカルでテスト実行

```bash
uv run --package egograph-ingest python ingest/src/main.py
```

### 6. GitHub Actionsの有効化

1. リポジトリの「Actions」タブを開く
2. ワークフローを有効化
3. 「Spotify Data Ingestion」を選択
4. 「Run workflow」で手動実行してテスト

## プロジェクト構造

```
ego-graph/
├── .github/workflows/       # GitHub Actions settings
├── ingest/                  # Data collection service (GitHub Actions)
│   ├── src/
│   │   ├── main.py          # Entry point
│   │   ├── ingest/          # Data collection logic
│   │   │   └── spotify/
│   │   └── pipeline/        # Processing pipeline (ETL, Embeddings, Storage)
│   ├── tests/
│   └── pyproject.toml
├── shared/                  # Shared Python package (egograph)
│   ├── egograph/            # Shared modules (models, config, utils)
│   ├── tests/
│   └── pyproject.toml
├── backend/                 # Backend API (FastAPI) - Placeholder
│   ├── app/
│   └── tests/
├── frontend/                # Frontend App (Next.js) - Placeholder
│   ├── app/
│   └── package.json
└── docs/                    # Documentation
    └── plan/                # Setup guides
```

## ドキュメント

- [01.user_setup.md](./docs/plan/01.user_setup.md) - セットアップの全体概要
- [02.api_keys.md](./docs/plan/02.api_keys.md) - APIクレデンシャル取得方法
- [03.github_secrets.md](./docs/plan/03.github_secrets.md) - GitHub Secrets設定
- [04.troubleshooting.md](./docs/plan/04.troubleshooting.md) - トラブルシューティング

## 使い方

### データ収集の自動実行

GitHub Actionsは毎日02:00 UTC（日本時間11:00）に自動実行されます。

### 手動実行

ローカル環境で:
```bash
uv run --package egograph-ingest python ingest/src/main.py
```

GitHub Actionsで:
1. Actionsタブ → 「Spotify Data Ingestion」
2. 「Run workflow」をクリック

### データの確認

Qdrant Cloudのダッシュボード (https://cloud.qdrant.io) でデータを確認できます。

## テスト

```bash
# 全テスト実行
uv run pytest

# 特定のテスト
uv run pytest ingest/tests/test_etl.py
```

## トラブルシューティング

問題が発生した場合:
1. [docs/plan/04.troubleshooting.md](./docs/plan/04.troubleshooting.md) を確認
2. GitHub Actionsのログを確認
3. GitHub Issuesで質問

## 今後の予定

- [ ] RAGクエリエンドポイント（FastAPI）
- [ ] Next.jsフロントエンドダッシュボード
- [ ] データ重複排除
- [ ] インクリメンタル更新
- [ ] 追加データソース（YouTube、ブラウザ履歴など）

## ライセンス

MIT License

## 貢献

Issue、Pull Requestを歓迎します！

## セキュリティ

セキュリティ上の問題を発見した場合は、公開Issueではなく直接ご連絡ください。
