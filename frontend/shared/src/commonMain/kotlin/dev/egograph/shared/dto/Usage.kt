package dev.egograph.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * トークン使用量情報
 */
@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)
