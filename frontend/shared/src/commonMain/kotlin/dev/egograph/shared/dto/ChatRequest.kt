package dev.egograph.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * チャットリクエスト
 */
@Serializable
data class ChatRequest(
    val messages: List<Message>,
    val stream: Boolean? = null,
    @SerialName("thread_id")
    val threadId: String? = null,
    @SerialName("model_name")
    val modelName: String? = null,
)
