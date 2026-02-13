package dev.egograph.shared.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * チャットスレッド
 */
@Serializable
data class Thread(
    @SerialName("thread_id")
    val threadId: String,
    @SerialName("user_id")
    val userId: String,
    val title: String,
    val preview: String?,
    @SerialName("message_count")
    val messageCount: Int,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("last_message_at")
    val lastMessageAt: String,
)
