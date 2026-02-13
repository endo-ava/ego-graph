package dev.egograph.shared.core.domain.model

import kotlinx.serialization.Serializable

/**
 * System Prompt 更新リクエスト
 */
@Serializable
data class SystemPromptUpdateRequest(
    val content: String,
)
