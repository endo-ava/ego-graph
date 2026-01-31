package dev.egograph.shared.dto

import kotlinx.serialization.Serializable

/**
 * LLMモデル情報
 */
@Serializable
data class LLMModel(
    val id: String,
    val name: String,
    val provider: String,
    val inputCostPer1m: Double,
    val outputCostPer1m: Double,
    val isFree: Boolean,
)
