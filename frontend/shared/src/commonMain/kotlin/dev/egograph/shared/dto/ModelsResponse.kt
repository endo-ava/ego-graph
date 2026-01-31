package dev.egograph.shared.dto

import kotlinx.serialization.Serializable

/**
 * モデル一覧レスポンス
 */
@Serializable
data class ModelsResponse(
    val models: List<LLMModel>,
    val defaultModel: String,
)
