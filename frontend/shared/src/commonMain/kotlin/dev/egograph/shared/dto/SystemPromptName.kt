package dev.egograph.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * System Prompt の種類
 */
@Serializable
enum class SystemPromptName(val apiName: String) {
    @SerialName("user")
    USER("user"),

    @SerialName("identity")
    IDENTITY("identity"),

    @SerialName("soul")
    SOUL("soul"),

    @SerialName("tools")
    TOOLS("tools");
}
