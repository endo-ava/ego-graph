package dev.egograph.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * チャットレスポンス
 */
@Serializable
data class ChatResponse(
    val id: String,
    val message: Message,
    @SerialName("tool_calls")
    val toolCalls: List<ToolCall>? = null,
    val usage: Usage? = null,
    @SerialName("thread_id")
    val threadId: String,
    @SerialName("model_name")
    val modelName: String? = null
)
