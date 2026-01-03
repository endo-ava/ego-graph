# EgoGraph Shared Package

共有データモデル、設定、ユーティリティを提供するライブラリパッケージ。
`ingest/`, `backend/` から workspace 依存として利用されます。

## 概要

このパッケージは以下を提供します：

- **データモデル** (`models.py`): Pydantic モデル（UnifiedDataModel, DataSource, DataType など）
- **設定管理** (`config.py`): 統一された設定クラス（Config, SpotifyConfig など）
- **ユーティリティ** (`utils.py`): 汎用的なヘルパー関数

## インストール

### モノレポ内での利用（推奨）

このパッケージは uv workspace の一部として管理されています。
ルートディレクトリで `uv sync` を実行すると、自動的にインストールされます。

```bash
# ルートディレクトリで実行
uv sync
```

### 他パッケージからの依存

`ingest/pyproject.toml` や `backend/pyproject.toml` で以下のように指定：

```toml
[project]
dependencies = [
    "shared @ {workspace = true}",
    ...
]
```

## 使用例

```python
from shared.models import UnifiedDataModel, DataSource, DataType
from shared.config import Config, SpotifyConfig
from shared.utils import batch_items
from datetime import datetime, timezone

# 設定の読み込み
config = Config(
    spotify=SpotifyConfig(
        client_id="your-client-id",
        client_secret="your-secret",
        refresh_token="your-refresh-token",
    )
)

# 統一データモデルの作成
model = UnifiedDataModel(
    source=DataSource.SPOTIFY,
    type=DataType.MUSIC,
    timestamp=datetime.now(timezone.utc),
    raw_text="Sample track",
)
```

## パッケージング規約

このパッケージは**ライブラリ型**です：

- 公開 API を `__init__.py` で再エクスポート
- `from shared import Config, SpotifyPlayEvent` のように使用可能
- `__all__` で公開範囲を明示

詳細: [CLAUDE.md - Python パッケージング](../CLAUDE.md#3-python-パッケージング-__init__py)

## テスト

```bash
# shared/ のテストのみ実行
uv run pytest shared/tests --cov=shared

# ルートから全テスト実行（shared を含む）
uv run pytest
```

## 開発

### 型チェック

```bash
uv run mypy shared/
```

### Lint & Format

```bash
uv run ruff check shared/
uv run ruff format shared/
```

## ディレクトリ構造

```
shared/
├── __init__.py        # 公開API再エクスポート
├── models.py          # Pydantic データモデル
├── config.py          # 設定クラス
├── utils.py           # ユーティリティ関数
├── tests/             # テストコード
├── pyproject.toml     # パッケージ設定
└── README.md          # このファイル
```

## 関連ドキュメント

- **[プロジェクト README](../README.md)**: 全体概要とクイックスタート
- **[開発ガイドライン](../CLAUDE.md)**: モノレポ構成と開発規約
- **[Ingest README](../ingest/README.md)**: データ収集ワーカー
- **[Backend README](../backend/README.md)**: API サーバー
