package dev.egograph.shared.dto

import kotlinx.serialization.Serializable

/**
 * スレッドメッセージ一覧レスポンス
 */
@Serializable
data class ThreadMessagesResponse(
    val threadId: String,
    val messages: List<ThreadMessage>
)
