# Backend Architecture

## 概要

EgoGraph Backend は Clean Architecture に準拠した4層構造で実装されています。
この設計により、保守性・テスタビリティ・拡張性が向上しています。

## レイヤー構成

```
backend/
├── dependencies.py          # 依存性注入（レイヤー横断）
├── api/                     # Presentation Layer（15-30行/エンドポイント）
│   ├── schemas/             # APIレスポンスモデル
│   │   ├── llm_model.py     # LLMモデル定義
│   │   └── thread.py        # スレッドレスポンスモデル
│   ├── chat.py              # チャットエンドポイント
│   ├── data.py              # データエンドポイント
│   ├── health.py            # ヘルスチェック
│   └── threads.py           # スレッド管理エンドポイント
├── usecases/                # Application Layer（ビジネスロジック）
│   ├── chat/                # チャットユースケース
│   │   ├── chat_usecase.py  # メインオーケストレーター
│   │   ├── system_prompt_builder.py
│   │   └── tool_executor.py # ツール実行管理
│   ├── tools/               # ツール実装
│   │   ├── base.py          # ツール基底クラス
│   │   ├── registry.py      # ツールレジストリ
│   │   └── spotify/         # Spotifyツール
│   └── spotify_stats.py     # Spotify統計ユースケース
├── domain/                  # Domain Layer（ビジネスルール）
│   ├── models/              # ドメインモデル
│   │   ├── chat.py          # ConversationContext
│   │   └── thread.py        # Thread, ThreadMessage
│   ├── repositories/        # リポジトリインターフェース
│   │   └── thread_repository.py
│   └── services/            # ドメインサービス
│       └── conversation_manager.py
└── infrastructure/          # Infrastructure Layer（外部システム統合）
    ├── database/            # データベース接続・クエリ
    │   ├── connection.py    # DuckDB接続
    │   └── queries.py       # Parquetクエリ
    ├── llm/                 # LLMプロバイダー
    │   ├── client.py        # LLMクライアント
    │   ├── models.py        # LLMモデル
    │   └── providers/       # プロバイダー実装
    │       ├── base.py
    │       ├── openai.py
    │       └── anthropic.py
    └── repositories/        # リポジトリ実装
        └── thread_repository_impl.py
```

## 依存関係ルール

外側から内側への**単方向依存**を厳守します：

```
Presentation → Application → Domain ← Infrastructure
     |              |           |           |
   API層      UseCase層   Domain層   Infrastructure層
```

- **API層**: UseCase層のみに依存
- **UseCase層**: Domain層のインターフェースに依存
- **Domain層**: 外部に依存しない（純粋なビジネスロジック）
- **Infrastructure層**: Domain層のインターフェースを実装

## 各層の責務

### Presentation Layer (api/)

リクエストの受付とレスポンスの生成を担当します。

**責務:**
- HTTPリクエストの受付とバリデーション
- UseCase層への処理委譲
- レスポンスの整形
- エラーの HTTP ステータスコードへの変換

**特徴:**
- 各エンドポイントは15-30行程度の薄いハンドラー
- ビジネスロジックを含まない
- FastAPI の Depends() による依存性注入

```python
# 例: chat.py
@router.post("", response_model=ChatResponseModel)
async def chat(request: ChatRequest, ...):
    # 1. 設定検証
    # 2. UseCase実行
    # 3. レスポンス返却
```

### Application Layer (usecases/)

ビジネスロジックのオーケストレーションを担当します。

**責務:**
- 複数のサービス/リポジトリの協調
- トランザクション管理
- ビジネスルールの適用

**主要クラス:**
- `ChatUseCase`: チャット会話全体の管理
- `ToolExecutor`: LLMツール実行ループの管理
- `SystemPromptBuilder`: システムプロンプトの構築

### Domain Layer (domain/)

純粋なビジネスルールとドメインモデルを定義します。

**責務:**
- ビジネスエンティティの定義
- ビジネスルールの実装
- リポジトリインターフェースの定義

**主要クラス:**
- `ConversationContext`: 会話状態を保持
- `IThreadRepository`: スレッド管理の抽象化
- `ConversationManager`: 会話準備ロジック

### Infrastructure Layer (infrastructure/)

外部システムとの統合を担当します。

**責務:**
- データベースアクセス
- 外部API呼び出し
- ファイルシステム操作

**主要クラス:**
- `DuckDBThreadRepository`: IThreadRepository の DuckDB 実装

## 実装パターン

### 薄いルーター（Thin Handler）

各エンドポイントは最小限の責務のみを持ちます：

```python
@router.post("")
async def endpoint(request: Request, deps = Depends(get_deps)):
    # 1. 入力バリデーション（FastAPI/Pydantic）
    # 2. UseCase呼び出し
    # 3. レスポンス変換
```

### 依存性注入（Dependency Injection）

`dependencies.py` を通じて全ての依存性を注入します：

```python
# backend/dependencies.py
def get_thread_repository(
    chat_db = Depends(get_chat_db),
) -> IThreadRepository:
    return DuckDBThreadRepository(chat_db)
```

**配置理由:**
- DIはレイヤー横断的な「配線」の役割
- `main.py`と同レベルで「アプリケーション構成」を表現
- Clean Architectureの依存性逆転の原則を実現

### Repository パターン

データアクセスを抽象化し、テスタビリティを向上：

```python
class IThreadRepository(ABC):
    @abstractmethod
    def create_thread(self, user_id: str, content: str) -> Thread:
        pass
```

## エラーハンドリング

| UseCase例外 | HTTPステータス | 説明 |
|------------|---------------|------|
| `NoUserMessageError` | 400 | ユーザーメッセージなし |
| `ValueError` (invalid_model_name) | 400 | 無効なモデル名 |
| `ThreadNotFoundError` | 404 | スレッド未検出 |
| `MaxIterationsExceeded` | 500 | 最大イテレーション到達 |
| LLM設定なし | 501 | LLM未設定 |
| `Exception` (LLMエラー) | 502 | LLM APIエラー |
| `asyncio.TimeoutError` | 504 | タイムアウト |

## テスト戦略

### Unit Tests

各層を個別にテスト：
- UseCase層: ドメインロジックのテスト
- Infrastructure層: DB操作のテスト

### Integration Tests

HTTP契約を検証：
- エンドポイントの動作確認
- エラーレスポンスの検証

### Mock境界

テストでモックする境界：
- `LLMClient`: LLM API呼び出し
- `ToolRegistry`: ツール実行
- `ThreadRepository`: データアクセス

## ディレクトリ構造（最終形）

```
backend/
├── __init__.py
├── main.py                  # FastAPI アプリケーション
├── config.py                # 設定管理
├── dependencies.py          # 依存性注入
├── validators.py            # バリデーター
├── api/                     # Presentation Layer
│   ├── __init__.py
│   ├── schemas/             # APIレスポンスモデル
│   │   ├── __init__.py
│   │   ├── llm_model.py
│   │   └── thread.py
│   ├── chat.py
│   ├── data.py
│   ├── health.py
│   └── threads.py
├── usecases/                # Application Layer
│   ├── __init__.py
│   ├── chat/
│   │   ├── __init__.py
│   │   ├── chat_usecase.py
│   │   ├── system_prompt_builder.py
│   │   └── tool_executor.py
│   ├── tools/               # ツール実装
│   │   ├── __init__.py
│   │   ├── base.py
│   │   ├── registry.py
│   │   └── spotify/
│   │       ├── __init__.py
│   │       └── stats.py
│   └── spotify_stats.py
├── domain/                  # Domain Layer
│   ├── __init__.py
│   ├── models/
│   │   ├── __init__.py
│   │   ├── chat.py
│   │   └── thread.py        # Thread, ThreadMessage
│   ├── repositories/
│   │   ├── __init__.py
│   │   └── thread_repository.py
│   └── services/
│       ├── __init__.py
│       └── conversation_manager.py
├── infrastructure/          # Infrastructure Layer
│   ├── __init__.py
│   ├── database/            # データベース統合
│   │   ├── __init__.py
│   │   ├── connection.py
│   │   ├── chat_connection.py
│   │   └── queries.py
│   ├── llm/                 # LLM統合
│   │   ├── __init__.py
│   │   ├── client.py
│   │   ├── models.py
│   │   └── providers/
│   │       ├── __init__.py
│   │       ├── base.py
│   │       ├── openai.py
│   │       └── anthropic.py
│   └── repositories/
│       ├── __init__.py
│       └── thread_repository_impl.py
└── tests/                   # テスト
    ├── conftest.py
    ├── fixtures/
    ├── integration/
    ├── performance/
    └── unit/
```

## アーキテクチャ変遷

### 完了した移行（2025年1月）

以下のリファクタリングが完了し、Clean Architectureへの移行が完了しました：

1. ✅ `services/thread_service.py` → `infrastructure/repositories/thread_repository_impl.py`
   - サービス層をリポジトリパターンに統合
2. ✅ `models/thread.py` → `domain/models/thread.py`
   - ドメインエンティティを適切な層に配置
3. ✅ `database/` → `infrastructure/database/`
   - インフラ層への集約
4. ✅ `llm/` → `infrastructure/llm/`
   - LLM統合をインフラ層に配置
5. ✅ `models/` → `api/schemas/`
   - APIレスポンスモデルの明確化
6. ✅ `tools/` → `usecases/tools/`
   - ツール実装をユースケース層に配置
7. ✅ `api/deps.py` → `dependencies.py`
   - DIをレイヤー横断的に配置

**結果:** 全206テスト合格、カバレッジ91%維持

### 推奨される開発フロー

新機能を追加する際は、以下の順序で実装してください：

1. **ドメイン層**: エンティティとリポジトリインターフェースを定義
2. **インフラ層**: リポジトリ実装、外部システム統合
3. **ユースケース層**: ビジネスロジックのオーケストレーション
4. **API層**: 薄いハンドラーでリクエスト/レスポンス処理
5. **テスト**: Integration testで全体の動作を検証

### 今後の拡張方針

- **認証の拡張**: JWT/OAuth導入時は`backend/api/auth.py`に分離
- **新規データソース**: `infrastructure/`配下に追加
- **複雑なドメインロジック**: `domain/services/`に追加
