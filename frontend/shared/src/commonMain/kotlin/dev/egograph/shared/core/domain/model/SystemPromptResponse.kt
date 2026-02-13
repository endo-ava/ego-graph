package dev.egograph.shared.core.domain.model

import kotlinx.serialization.Serializable

/**
 * System Prompt API レスポンス
 */
@Serializable
data class SystemPromptResponse(
    val name: String,
    val content: String,
)
