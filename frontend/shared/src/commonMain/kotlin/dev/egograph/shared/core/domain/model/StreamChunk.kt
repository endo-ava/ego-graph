package dev.egograph.shared.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * ストリーミングチャンクの種類
 */
@Serializable
enum class StreamChunkType {
    @SerialName("delta")
    DELTA,

    @SerialName("tool_call")
    TOOL_CALL,

    @SerialName("tool_result")
    TOOL_RESULT,

    @SerialName("done")
    DONE,

    @SerialName("error")
    ERROR,
}

/**
 * ストリーミングチャンク
 */
@Serializable
data class StreamChunk(
    val type: StreamChunkType,
    val delta: String? = null,
    @SerialName("tool_calls")
    val toolCalls: List<ToolCall>? = null,
    @SerialName("tool_name")
    val toolName: String? = null,
    @SerialName("tool_result")
    val toolResult: JsonObject? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null,
    val usage: Usage? = null,
    val error: String? = null,
    @SerialName("thread_id")
    val threadId: String? = null,
)
