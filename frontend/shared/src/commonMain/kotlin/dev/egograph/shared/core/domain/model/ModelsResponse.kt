package dev.egograph.shared.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * モデル一覧レスポンス
 */
@Serializable
data class ModelsResponse(
    val models: List<LLMModel>,
    @SerialName("default_model")
    val defaultModel: String,
)
