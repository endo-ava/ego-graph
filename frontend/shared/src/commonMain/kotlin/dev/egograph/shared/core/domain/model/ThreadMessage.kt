package dev.egograph.shared.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * スレッド内のメッセージ
 */
@Serializable
data class ThreadMessage(
    @SerialName("message_id")
    val messageId: String,
    @SerialName("thread_id")
    val threadId: String,
    @SerialName("user_id")
    val userId: String,
    val role: MessageRole,
    val content: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("model_name")
    val modelName: String? = null,
)
