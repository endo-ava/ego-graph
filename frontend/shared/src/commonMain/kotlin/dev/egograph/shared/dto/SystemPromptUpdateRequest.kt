package dev.egograph.shared.dto

import kotlinx.serialization.Serializable

/**
 * System Prompt 更新リクエスト
 */
@Serializable
data class SystemPromptUpdateRequest(
    val content: String
)
