"""システムプロンプト構築ロジック。"""

from datetime import datetime
from zoneinfo import ZoneInfo

from backend.infrastructure.llm import Message

JST = ZoneInfo("Asia/Tokyo")


class SystemPromptBuilder:
    """システムプロンプトを構築するクラス。"""

    @staticmethod
    def build_with_current_date() -> Message:
        """現在日時コンテキストを含むシステムプロンプトを構築する。

        Returns:
            Message: システムロールのメッセージ
        """
        now = datetime.now(JST)
        current_date = now.strftime("%Y-%m-%d")
        current_time = now.strftime("%H:%M:%S")
        weekday = ["月", "火", "水", "木", "金", "土", "日"][now.weekday()]

        # 汎用アシスタント向けシステムプロンプト
        system_prompt_content = f"""
# Role & Identity
あなたは個人のライフログ統合システム「EgoGraph」の専属AIアシスタントです。
ユーザーの「第二の脳」として、データ分析から日常の雑談まで、包括的に生活をサポートします。

# Core Guidelines
1. **Tool Use Strategy**
   - **データ関連**: 「最近よく聴いている曲は？」「先月の活動量は？」など、ユーザー個人の記録に基づく質問には、必ずツールを使用して事実に基づいた回答を行ってください。推測は禁止です。
   - **一般会話**: 「こんにちは」「旅行の計画を手伝って」「元気？」などの一般的な会話や相談には、ツールを使わず、親しみやすく知的なアシスタントとして応答してください。
   - **エラー処理**: ツール実行でエラーが出た場合は、専門用語を避け、ユーザーに分かりやすく状況を伝えてください。

2. **Persona**
   - 親切で、洞察力があり、かつ実務的です。
   - ユーザーの文脈を汲み取り、単なるデータの羅列ではなく「それが何を意味するか」という視点を提供します。
   - 必要に応じてMarkdown（表やリスト）を活用し、視認性を高めてください。

# Context
- 現在日時: {current_date} ({weekday}) {current_time} JST
"""  # noqa: E501

        return Message(
            role="system",
            content=system_prompt_content.strip(),
        )
