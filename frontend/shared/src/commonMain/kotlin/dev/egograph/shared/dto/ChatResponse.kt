package dev.egograph.shared.dto

import kotlinx.serialization.Serializable

/**
 * チャットレスポンス
 */
@Serializable
data class ChatResponse(
    val id: String,
    val message: Message,
    val toolCalls: List<ToolCall>? = null,
    val usage: Usage? = null,
    val threadId: String,
    val modelName: String? = null
)
