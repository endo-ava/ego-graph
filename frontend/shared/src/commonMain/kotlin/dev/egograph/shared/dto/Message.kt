package dev.egograph.shared.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * チャットメッセージの役割
 */
@Serializable
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
    TOOL
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
    val toolCalls: List<ToolCall>? = null
)
