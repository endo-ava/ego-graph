package dev.egograph.shared.dto

import kotlinx.serialization.Serializable

/**
 * チャットスレッド
 */
@Serializable
data class Thread(
    val threadId: String,
    val userId: String,
    val title: String,
    val preview: String?,
    val messageCount: Int,
    val createdAt: String,
    val lastMessageAt: String
)
