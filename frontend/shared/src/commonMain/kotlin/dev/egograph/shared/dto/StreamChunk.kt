package dev.egograph.shared.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * ストリーミングチャンクの種類
 */
@Serializable
enum class StreamChunkType {
    DELTA,
    TOOL_CALL,
    TOOL_RESULT,
    DONE,
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
