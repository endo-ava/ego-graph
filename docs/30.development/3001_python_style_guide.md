# Python Best Practices & Style Guide (2025.12)

本ドキュメントは、Python 3.12+ 時代における「Pythonic」なコードを書くためのベストプラクティスと、PEP 8 の要約をまとめたものです。
可読性、保守性、そしてパフォーマンスを重視した現代的な開発基準として機能することを目的としています。

## 1. Modern Pythonic Idioms (最新のプラクティス)

### 1.1 型ヒントと静的解析 (Type Hinting)
Python 3.9以降、標準コレクション型（`list`, `dict`, `tuple`等）での型ヒントが可能になりました。`typing.List`などは非推奨です。

*   **基本則**: すべての関数引数と戻り値に型ヒントを記述する。
*   **ジェネリクス**: `list[str]`, `dict[str, Any]`, `tuple[int, ...]` のように記述。
*   **Union型**: Python 3.10以降は `Optional[T]` や `Union[int, str]` の代わりに `T | None`, `int | str` を使用する。
*   **Self型**: 返り値が自身のインスタンスである場合、`typing.Self` (3.11+) を使用する。

```python
def process_items(items: list[str]) -> int | None:
    if not items:
        return None
    return len(items)
```

### 1.2 データ構造 (Dataclasses & Pydantic)
単なるデータを保持するクラスには、`__init__`を手書きせず `dataclasses` を使用する。

*   **dataclasses**: 標準ライブラリ。`slots=True` (3.10+) を指定してメモリ効率と属性アクセス速度を向上させる。
*   **Pydantic**: 外部入出力（APIリクエスト/レスポンス、設定ファイル）のバリデーションには `Pydantic` モデルを使用する。

```python
from dataclasses import dataclass

@dataclass(slots=True, frozen=True)
class UserContext:
    user_id: str
    roles: list[str]
    is_active: bool = True
```

### 1.3 制御フローとパターンマッチング
Python 3.10で導入された `match` 文を活用し、複雑な `if-elif` チェーンを簡潔化する。

```python
match command.split():
    case ["quit"]:
        print("Goodbye")
    case ["load", filename]:
        load_file(filename)
    case _:
        print("Unknown command")
```

### 1.4 最近の構文機能
*   **f-strings**: 文字列フォーマットは `.format()` や `%` ではなく常に f-strings を使用する。デバッグには `f"{var=}"` が便利。
*   **Walrus Operator (`:=`)**: 条件式内で値を計算・再利用する場合に使用し、スコープ汚染を防ぐ。
*   **Pathlib**: ファイルパス操作には `os.path` ではなく `pathlib.Path` を使用する。

### 1.5 非同期処理 (Asyncio)
*   `asyncio.run()` をエントリーポイントとする。
*   **TaskGroup (3.11+)**: `asyncio.gather` の代わりに `asyncio.TaskGroup` を使用し、構造化された並行処理を行う（例外処理が安全）。

```python
async def main():
    async with asyncio.TaskGroup() as tg:
        tg.create_task(fetch_data_a())
        tg.create_task(fetch_data_b())
```

### 1.6 一般的な禁止事項・注意点
*   **可変デフォルト引数**: `def func(a=[])` は禁止。`def func(a=None)` とし内部で初期化する。
*   **内包表記**: `map` や `filter` よりもリスト/辞書内包表記を優先するが、複雑すぎる場合は通常のループにする。
*   **コンテキストマネージャ**: リソース（ファイル、DB接続）は必ず `with` 構文で管理する。

---

## 2. PEP 8 Essentials (スタイルガイド要約)

PEP 8 はPythonコードのスタイルガイドです。コードの「一貫性」を保つために以下を遵守します。現代では `Ruff` や `Black` などのフォーマッタに任せるのが一般的です。

### 2.1 レイアウトと空白
*   **インデント**: スペース4つ。タブは使用禁止。
*   **行の長さ**: PEP 8規定は79文字だが、現代的なプロジェクト（Black等）では **88文字** または **100文字** が許容されることが多い。チーム内で統一する。
*   **空白行**:
    *   トップレベルの関数・クラス定義の間: **2行**
    *   クラス内のメソッド定義の間: **1行**
*   **演算子の周囲**: 代入(`=`)、比較(`==`, `<`)、論理演算子(`and`)の周囲にはスペースを1つ入れる。
    *   *例外*: キーワード引数の代入 `func(arg=value)` にはスペースを入れない。

### 2.2 インポート (Imports)
ファイルの先頭に記述し、以下の順序でグループ化する（各グループ間は1行空ける）。

1.  **標準ライブラリ** (`os`, `sys`, `typing`...)
2.  **サードパーティライブラリ** (`numpy`, `pydantic`...)
3.  **ローカルアプリケーション/ライブラリ** (`myapp.models`...)

*   `from module import *` (ワイルドカードインポート) は名前空間を汚染するため**禁止**。

### 2.3 命名規則 (Naming Conventions)

| 対象 | 規則 | 例 |
| :--- | :--- | :--- |
| **変数 / 関数 / メソッド** | `snake_case` | `user_name`, `calculate_total()` |
| **クラス / 例外** | `CamelCase` (PascalCase) | `UserContext`, `DatabaseError` |
| **定数** | `UPPER_CASE` | `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT` |
| **モジュール / パッケージ** | `snake_case` (短く) | `utils`, `data_processing` |
| **プライベート属性** | `_snake_case` (先頭アンスコ) | `_internal_cache` |

### 2.4 ドキュメンテーション
*   **Docstrings**: すべての公開モジュール、関数、クラス、メソッドに記述する。
*   形式は **Google Style** または **NumPy Style** を推奨（チームで統一）。

```python
def fetch_user(user_id: int) -> dict[str, Any]:
    """ユーザー情報を外部APIから取得する。

    Args:
      user_id: 取得対象のユーザーID。

    Returns:
      ユーザー情報を含む辞書。見つからない場合は空辞書。
    """
    ...
```
