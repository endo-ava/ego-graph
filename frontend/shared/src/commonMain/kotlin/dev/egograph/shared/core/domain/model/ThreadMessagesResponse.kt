package dev.egograph.shared.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * スレッドメッセージ一覧レスポンス
 */
@Serializable
data class ThreadMessagesResponse(
    @SerialName("thread_id")
    val threadId: String,
    val messages: List<ThreadMessage>,
)
