package dev.egograph.shared.dto

import kotlinx.serialization.Serializable

/**
 * トークン使用量情報
 */
@Serializable
data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)
