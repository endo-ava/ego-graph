package dev.egograph.shared.core.domain.model

import kotlinx.serialization.Serializable

/**
 * スレッド一覧レスポンス
 */
@Serializable
data class ThreadListResponse(
    val threads: List<Thread>,
    val total: Int,
    val limit: Int,
    val offset: Int,
)
