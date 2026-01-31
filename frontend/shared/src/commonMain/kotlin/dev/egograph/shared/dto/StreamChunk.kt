package dev.egograph.shared.dto

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
    ERROR
}

/**
 * ストリーミングチャンク
 */
@Serializable
data class StreamChunk(
    val type: StreamChunkType,
    val delta: String? = null,
    val toolCalls: List<ToolCall>? = null,
    val toolName: String? = null,
    val toolResult: JsonObject? = null,
    val finishReason: String? = null,
    val usage: Usage? = null,
    val error: String? = null,
    val threadId: String? = null
)
