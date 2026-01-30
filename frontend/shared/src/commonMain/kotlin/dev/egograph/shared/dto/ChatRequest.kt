package dev.egograph.shared.dto

import kotlinx.serialization.Serializable

/**
 * チャットリクエスト
 */
@Serializable
data class ChatRequest(
    val messages: List<Message>,
    val stream: Boolean? = null,
    val threadId: String? = null,
    val modelName: String? = null
)
