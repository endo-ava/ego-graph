package dev.egograph.shared.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * チャットメッセージの役割
 *
 * Backend API との互換性のため、JSON 値は小文字を使用する。
 */
@Serializable
enum class MessageRole {
    @SerialName("user")
    USER,

    @SerialName("assistant")
    ASSISTANT,

    @SerialName("system")
    SYSTEM,

    @SerialName("tool")
    TOOL,
}

/**
 * チャットメッセージ
 */
@Serializable
data class Message(
    val role: MessageRole,
    val content: String?,
    val toolCallId: String? = null,
    val name: String? = null,
    val toolCalls: List<ToolCall>? = null,
)
