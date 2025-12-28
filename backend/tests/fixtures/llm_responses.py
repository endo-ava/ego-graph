"""モックLLMレスポンス。"""

from typing import Any, Dict, List, Optional


def get_mock_openai_response(
    content: str = "Test response", tool_calls: Optional[List[Dict[str, Any]]] = None
):
    """モックOpenAI APIレスポンス。

    Args:
        content: レスポンスのコンテンツ
        tool_calls: ツール呼び出しのリスト

    Returns:
        OpenAI API形式のレスポンス辞書
    """
    # tool_callsがある場合はfinish_reasonを"tool_calls"にする
    finish_reason = "tool_calls" if tool_calls else "stop"

    return {
        "id": "chatcmpl-test-123",
        "object": "chat.completion",
        "created": 1234567890,
        "model": "gpt-4o-mini",
        "choices": [
            {
                "index": 0,
                "message": {
                    "role": "assistant",
                    "content": content,
                    "tool_calls": tool_calls,
                },
                "finish_reason": finish_reason,
            }
        ],
        "usage": {
            "prompt_tokens": 10,
            "completion_tokens": 20,
            "total_tokens": 30,
        },
    }


def get_mock_openai_tool_call_response(tool_name: str, tool_arguments: Dict[str, Any]):
    """ツール呼び出しを含むモックOpenAI APIレスポンス。

    Args:
        tool_name: ツール名
        tool_arguments: ツール引数

    Returns:
        ツール呼び出しを含むOpenAI API形式のレスポンス辞書
    """
    import json

    tool_calls = [
        {
            "id": "call_test_123",
            "type": "function",
            "function": {"name": tool_name, "arguments": json.dumps(tool_arguments)},
        }
    ]

    return get_mock_openai_response(content="", tool_calls=tool_calls)


def get_mock_anthropic_response(content: str = "Test response"):
    """モックAnthropic APIレスポンス。

    Args:
        content: レスポンスのコンテンツ

    Returns:
        Anthropic API形式のレスポンス辞書
    """
    return {
        "id": "msg_test_123",
        "type": "message",
        "role": "assistant",
        "content": [{"type": "text", "text": content}],
        "model": "claude-3-5-sonnet-20241022",
        "usage": {"input_tokens": 10, "output_tokens": 20},
    }


def get_mock_anthropic_tool_use_response(
    tool_name: str, tool_input: Dict[str, Any], text_content: str = ""
):
    """ツール使用を含むモックAnthropic APIレスポンス。

    Args:
        tool_name: ツール名
        tool_input: ツール入力
        text_content: テキストコンテンツ（オプション）

    Returns:
        ツール使用を含むAnthropic API形式のレスポンス辞書
    """
    content = []
    if text_content:
        content.append({"type": "text", "text": text_content})

    content.append(
        {
            "type": "tool_use",
            "id": "toolu_test_123",
            "name": tool_name,
            "input": tool_input,
        }
    )

    return {
        "id": "msg_test_123",
        "type": "message",
        "role": "assistant",
        "content": content,
        "model": "claude-3-5-sonnet-20241022",
        "usage": {"input_tokens": 10, "output_tokens": 20},
    }
