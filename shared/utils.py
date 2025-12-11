"""EgoGraphのためのユーティリティ関数。"""

import logging
from datetime import datetime
from typing import Any, Dict, List
from uuid import UUID

logger = logging.getLogger(__name__)


def serialize_for_json(obj: Any) -> Any:
    """JSONエンコード用にオブジェクトをシリアライズします。

    Args:
        obj: シリアライズするオブジェクト

    Returns:
        JSONシリアライズ可能な表現
    """
    if isinstance(obj, UUID):
        return str(obj)
    elif isinstance(obj, datetime):
        return obj.isoformat()
    elif isinstance(obj, dict):
        return {k: serialize_for_json(v) for k, v in obj.items()}
    elif isinstance(obj, (list, tuple)):
        return [serialize_for_json(item) for item in obj]
    else:
        return obj


def batch_items(items: List[Any], batch_size: int) -> List[List[Any]]:
    """アイテムをバッチに分割します。

    Args:
        items: バッチ処理するアイテムのリスト
        batch_size: 各バッチの最大サイズ

    Returns:
        バッチのリスト
    """
    return [
        items[i:i + batch_size]
        for i in range(0, len(items), batch_size)
    ]


def format_duration_ms(duration_ms: int) -> str:
    """ミリ秒単位の時間を人間が読みやすい文字列にフォーマットします。

    Args:
        duration_ms: ミリ秒単位の時間

    Returns:
        フォーマットされた時間文字列 (例: "3:45")
    """
    seconds = duration_ms // 1000
    minutes = seconds // 60
    seconds = seconds % 60
    return f"{minutes}:{seconds:02d}"


def safe_get(data: Dict[str, Any], *keys: str, default: Any = None) -> Any:
    """ネストされた辞書の値を安全に取得します。

    Args:
        data: クエリ対象の辞書
        *keys: 探索するキーのシーケンス
        default: キーが見つからない場合のデフォルト値

    Returns:
        ネストされたキーの値、または見つからない場合はデフォルト値
    """
    current = data
    for key in keys:
        if isinstance(current, dict) and key in current:
            current = current[key]
        else:
            return default
    return current


def log_execution_time(func):
    """関数の実行時間をログ出力するデコレータ。

    Args:
        func: ラップする関数

    Returns:
        ラップされた関数
    """
    from datetime import timezone

    def wrapper(*args, **kwargs):
        start_time = datetime.now(timezone.utc)
        logger.info(f"Starting {func.__name__}")

        try:
            result = func(*args, **kwargs)
            elapsed = (datetime.now(timezone.utc) - start_time).total_seconds()
            logger.info(f"Completed {func.__name__} in {elapsed:.2f}s")
        except Exception:
            elapsed = (datetime.now(timezone.utc) - start_time).total_seconds()
            logger.exception(
                f"Failed {func.__name__} after {elapsed:.2f}s"
            )
            raise
        else:
            return result

    return wrapper
