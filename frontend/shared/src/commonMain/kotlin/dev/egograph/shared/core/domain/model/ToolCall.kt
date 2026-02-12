package dev.egograph.shared.core.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * ツール呼び出しリクエスト
 */
@Serializable
data class ToolCall(
    val id: String,
    val name: String,
    val parameters: JsonObject,
)
