---
paths:
  - **/test_*.py
  - **/tests/**/*.py
---

# Backend テスト戦略 (Testing Strategy)

EgoGraph backend のテスト実装に関する標準ガイドライン。

## 1. テストレベルとディレクトリ構成

テストは粒度と目的に応じて `unit` (単体) と `integration` (統合) に分離し、対象モジュールごとにディレクトリを切る。

```text
backend/tests/
├── unit/                 # 単体テスト (純粋ロジック + in-memory DBは可)
├── integration/          # API/Usecase/DBの結合テスト
└── performance/          # 計測・負荷の簡易テスト (必要な場合のみ)
```

- **Unit Test**: 関数/クラス単体のロジックと境界条件を検証する。外部 IO (R2, API, LLM) は原則モックする。DuckDB の in-memory は「純粋ロジック検証の補助」として許容する。
- **Integration Test**: 2〜3コンポーネントの結合（API + usecase + DB など）を検証する。外部サービスはモックし、DB は in-memory を許容する。
- **E2E**: 本プロジェクトでは実施しない。

### 1.1 テストレベルの目的と依存ポリシー

| レベル | 目的 | 対象 | 依存の扱い |
|---|---|---|---|
| Unit | ロジックの正しさ/境界条件 | 単一関数・クラス | 外部IOは全てモック。時間/乱数も差し替え。 |
| Integration | 契約・連携の妥当性 | 2〜3コンポーネントの結合 | 外部API/LLMはモック。DBは in-memory を許容。 |

## 2. IT の範囲

個人PJでE2Eを行わない前提のため、IT は「本番に近い結合を、外部サービスなしで再現できる範囲」に限定する。

### 2.1 API 統合

- **対象**: FastAPI ルーティング + バリデーション + usecase 結合
- **依存**: LLM/外部API はモック。DB は usecase が依存する場合のみ in-memory
- **目的**: API契約とユースケース結合の保証

### 2.2 DB/Query 統合

- **対象**: DuckDB 実接続 + 小さな fixture Parquet
- **依存**: R2 は使わない。ローカルファイルのみ
- **目的**: SQL の集計/結合ロジックの妥当性

### 2.3 ストリーミング最小ケース

- **対象**: SSE の最小チャンク結合と保存
- **依存**: LLM/外部API はモック。DB は in-memory
- **目的**: 重要経路の動作保証

## 3. 実装スタイルと規約

### 3.1 AAA パターン (Arrange-Act-Assert)

テストメソッド内は以下の3つのセクションに明確に分割し、コメントで明示する。

```python
    # Arrange: テストデータの準備とモックの設定
    # Act: テスト対象メソッドの実行
    # Assert: 結果の検証 (戻り値、副作用、呼び出し引数)
```

### 3.2 日本語ドキュメントとコメント

- **Docstring**: テストメソッドの目的を日本語で簡潔に記述する。
- **Comments**: 複雑なモック設定や検証ロジックには、意図を説明する日本語コメントを付記する。

### 3.3 命名規則

- **ファイル名**: `test_<module_name>.py` (例: `test_queries.py`)
- **テストクラス**: `Test<ClassName>` (例: `TestChatEndpoint`)
- **テストメソッド**: `test_<method_name>_<condition>` (例: `test_chat_requires_api_key`)

## 4. ツールとライブラリ

| 役割 | ツール | 備考 |
|---|---|---|
| テストランナー | **pytest** | 標準ランナー。`uv run pytest` で実行。 |
| モック | **unittest.mock** | `patch`, `MagicMock` を使用して依存関係を切り離す。 |

## 5. CI/CD 統合

GitHub Actions (`ci-backend.yml`) において、以下のポリシーでテストを実行する。

- **トリガー**: 関連ディレクトリ (`backend/**`, `shared/**`) の変更時。
- **実行コマンド**: `uv run pytest tests/ -v --cov=backend --cov-report=xml --cov-report=term`
- **環境変数**: CI では `R2_*` をダミー値で与える。
- **カバレッジ**: ワークフロー内で計測する。Codecov 連携は将来的に有効化予定。

## 6. チェックリスト

PR 作成前に以下を確認すること：

- [ ] 全てのテストがパスするか (`uv run pytest`)
- [ ] AAA パターンで記述されているか
- [ ] テストの目的が日本語 Docstring で説明されているか
- [ ] モックが適切に使われ、意図しない外部アクセスが発生していないか

## 7. モックの粒度ルール

- **API統合テスト**: ルーティングと入力バリデーションを検証し、外部API/LLMはモックする。DBはユースケース依存がある場合のみ in-memory。
- **usecaseテスト**: DB/外部IOをモックし、呼び出し引数と戻り値を検証する。
- **DB/クエリテスト**: DuckDB を使った最小のSQL検証に留め、API層は含めない。
