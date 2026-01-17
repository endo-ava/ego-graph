# Backend Architecture

Clean Architecture に準拠した4層構造で実装されています。

## 依存関係ルール

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

HTTPリクエストの受付とレスポンスの生成。各エンドポイントは15-30行程度の薄いハンドラーで構成。

**責務:**
- HTTPリクエストの受付とバリデーション
- UseCase層への処理委譲
- レスポンスの整形
- エラーの HTTP ステータスコードへの変換

### Application Layer (usecases/)

ビジネスロジックのオーケストレーション。

**主要クラス:**
- `ChatUseCase`: チャット会話全体の管理
- `ToolExecutor`: LLMツール実行ループの管理
- `SystemPromptBuilder`: システムプロンプトの構築

### Domain Layer (domain/)

純粋なビジネスルールとドメインモデル。

**主要クラス:**
- `ConversationContext`: 会話状態
- `IThreadRepository`: スレッド管理の抽象化
- `ConversationManager`: 会話準備ロジック

### Infrastructure Layer (infrastructure/)

外部システムとの統合（DBアクセス、LLM呼び出し）。

**主要クラス:**
- `DuckDBThreadRepository`: IThreadRepository の DuckDB 実装

## 実装パターン

### 薄いルーター

```python
@router.post("")
async def endpoint(request: Request, deps = Depends(get_deps)):
    # 1. 入力バリデーション
    # 2. UseCase呼び出し
    # 3. レスポンス変換
```

### 依存性注入

`dependencies.py` で全ての依存性を注入（レイヤー横断の配線）。

### Repository パターン

データアクセスを抽象化し、テスタビリティを向上。

## ディレクトリ構造

```
backend/
├── main.py                  # FastAPI アプリケーション
├── config.py                # 設定管理
├── dependencies.py          # 依存性注入
├── api/                     # Presentation Layer
│   ├── schemas/             # APIレスポンスモデル
│   │   ├── llm_model.py
│   │   └── thread.py
│   ├── chat.py
│   ├── data.py
│   ├── health.py
│   └── threads.py
├── usecases/                # Application Layer
│   ├── chat/
│   │   ├── chat_usecase.py
│   │   ├── system_prompt_builder.py
│   │   └── tool_executor.py
│   ├── tools/
│   │   ├── base.py
│   │   ├── registry.py
│   │   └── spotify/stats.py
│   └── spotify_stats.py
├── domain/                  # Domain Layer
│   ├── models/
│   │   ├── chat.py
│   │   └── thread.py
│   ├── repositories/
│   │   └── thread_repository.py
│   └── services/
│   │       └── conversation_manager.py
├── infrastructure/          # Infrastructure Layer
│   ├── database/
│   │   ├── connection.py
│   │   ├── chat_connection.py
│   │   └── queries.py
│   ├── llm/
│   │   ├── client.py
│   │   ├── models.py
│   │   └── providers/
│   │       ├── base.py
│   │       ├── openai.py
│   │       └── anthropic.py
│   └── repositories/
│       └── thread_repository_impl.py
└── tests/
    ├── conftest.py
    ├── integration/
    └── unit/
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
- UseCase層: ドメインロジックのテスト
- Infrastructure層: DB操作のテスト

### Integration Tests
- エンドポイントの動作確認
- エラーレスポンスの検証

### Mock境界
- `LLMClient`: LLM API呼び出し
- `ToolRegistry`: ツール実行
- `ThreadRepository`: データアクセス

## 開発ガイド

### 新機能追加の順序

1. **Domain層**: エンティティとリポジトリインターフェースを定義
2. **Infrastructure層**: リポジトリ実装、外部システム統合
3. **UseCase層**: ビジネスロジックのオーケストレーション
4. **API層**: 薄いハンドラーでリクエスト/レスポンス処理
5. **テスト**: Integration testで全体の動作を検証

### 今後の拡張方針

- **認証**: JWT/OAuth導入時は `api/auth.py` に分離
- **新規データソース**: `infrastructure/` 配下に追加
- **複雑なドメインロジック**: `domain/services/` に追加
