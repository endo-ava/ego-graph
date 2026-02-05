package dev.egograph.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * LLMモデル情報
 */
@Serializable
data class LLMModel(
    val id: String,
    val name: String,
    val provider: String,
    @SerialName("input_cost_per_1m")
    val inputCostPer1m: Double,
    @SerialName("output_cost_per_1m")
    val outputCostPer1m: Double,
    @SerialName("is_free")
    val isFree: Boolean,
)
