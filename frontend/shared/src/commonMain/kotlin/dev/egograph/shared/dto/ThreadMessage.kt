package dev.egograph.shared.dto

import kotlinx.serialization.Serializable

/**
 * スレッド内のメッセージ
 */
@Serializable
data class ThreadMessage(
    val messageId: String,
    val threadId: String,
    val userId: String,
    val role: MessageRole,
    val content: String,
    val createdAt: String,
    val modelName: String? = null,
)
