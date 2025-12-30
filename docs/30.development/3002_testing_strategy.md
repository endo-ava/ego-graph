# テスト戦略 (Testing Strategy)

EgoGraph プロジェクトにおけるテスト実装の標準ガイドライン。
保守性が高く、開発者が意図を理解しやすいテストコードを維持することを目的とする。

## 1. テストレベルとディレクトリ構成

テストは粒度と目的に応じて `unit` (単体) と `integration` (統合) に分離し、対象モジュールごとにディレクトリを切る。

```text
ingest/tests/
├── unit/                 # 単体テスト (外部依存なし / モック使用)
│   ├── spotify/          # Spotify モジュール用
│   │   ├── test_collector.py
│   │   ├── test_storage.py
│   │   └── ...
└── integration/          # 統合テスト (実際のワークフロー / 外部APIはモック可)
    ├── spotify/
```

- **Unit Test**: クラスや関数単体のロジックを検証する。外部 IO (R2, API) は原則モックする。
- **Integration Test**: 複数のコンポーネント (Collector -> Storage -> DuckDB) が連携するパイプライン全体の動作を検証する。

## 2. 実装スタイルと規約

### 2.1 AAA パターン (Arrange-Act-Assert)

テストメソッド内は以下の3つのセクションに明確に分割し、コメントで明示する。これにより、「何を準備し、何を実行し、何を検証しているか」を一目で理解できるようにする。

```python
def test_save_parquet_success(self, storage, mock_s3_client):
    """Parquet ファイル保存の成功ケースをテストする。"""
    # Arrange: テストデータの準備とモックの設定
    data = [{"track_name": "Test Track", "artist_name": "Test Artist"}]
    mock_s3 = mock_s3_client.return_value
    
    # Act: テスト対象メソッドの実行
    key = storage.save_parquet(data, 2023, 10)
    
    # Assert: 結果の検証 (戻り値、副作用、呼び出し引数)
    assert key is not None
    assert key.endswith(".parquet")
    mock_s3.put_object.assert_called_once()
```

### 2.2 日本語ドキュメントとコメント

- **Docstring**: テストメソッドの目的を日本語で簡潔に記述する。
- **Comments**: 複雑なモック設定や検証ロジックには、意図を説明する日本語コメントを付記する。

### 2.3 命名規則

- **ファイル名**: `test_<module_name>.py` (例: `test_storage.py`)
- **テストクラス**: `Test<ClassName>` (例: `TestSpotifyStorage`)
- **テストメソッド**: `test_<method_name>_<condition>` (例: `test_get_track_info_not_found`)

## 3. ツールとライブラリ

| 役割 | ツール | 備考 |
|---|---|---|
| テストランナー | **pytest** | 標準ランナー。`uv run pytest` で実行。 |
| HTTP モック | **responses** | 外部 API リクエストの検証とモックレスポンス定義に使用。 |
| オブジェクトモック | **unittest.mock** | `patch`, `MagicMock` を使用して依存関係を切り離す。 |
| AWS/S3 モック | **unittest.mock** | `boto3.client` をパッチして R2 へのアクセスを遮断する。必要に応じて `moto` の導入も検討するが、現状は Mock で十分とする。 |

## 4. CI/CD 統合

GitHub Actions (`ci-ingest.yml` 等) において、以下のポリシーでテストを実行する。

- **トリガー**: 関連ディレクトリ (`ingest/**`, `shared/**`) の変更時。
- **実行コマンド**: `uv run pytest tests/ -v --cov=ingest --cov-report=xml --cov-report=term`
- **カバレッジ**: ワークフロー実行ごとに自動的に計測され、Codecov へアップロードされる。重要なロジック変更時はレポートを確認し、テスト漏れを防ぐ。

## 5. チェックリスト

PR 作成前に以下を確認すること：

- [ ] 全てのテストがパスするか (`uv run pytest`)
- [ ] AAA パターンで記述されているか
- [ ] テストの目的が日本語 Docstring で説明されているか
- [ ] モックが適切に使われ、意図しない外部アクセスが発生していないか
