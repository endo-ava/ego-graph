package dev.egograph.shared.ui.terminal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.egograph.shared.platform.PlatformPreferences
import dev.egograph.shared.platform.PlatformPrefsDefaults
import dev.egograph.shared.platform.PlatformPrefsKeys
import dev.egograph.shared.platform.getDefaultGatewayBaseUrl
import dev.egograph.shared.platform.normalizeBaseUrl
import io.ktor.http.encodeURLParameter

data class TerminalSettings(
    val wsUrl: String?,
    val apiKey: String?,
    val error: String?,
)

@Composable
fun rememberTerminalSettings(
    agentId: String,
    preferences: PlatformPreferences,
): TerminalSettings {
    return remember(agentId, preferences) {
        val gatewayUrl =
            preferences
                .getString(
                    PlatformPrefsKeys.KEY_GATEWAY_API_URL,
                    PlatformPrefsDefaults.DEFAULT_GATEWAY_API_URL,
                ).ifBlank { getDefaultGatewayBaseUrl() }
                .trim()
        val apiKey =
            preferences
                .getString(
                    PlatformPrefsKeys.KEY_GATEWAY_API_KEY,
                    PlatformPrefsDefaults.DEFAULT_GATEWAY_API_KEY,
                ).trim()

        if (gatewayUrl.isBlank()) {
            TerminalSettings(
                wsUrl = null,
                apiKey = null,
                error = "Gateway API URL is not configured",
            )
        } else if (apiKey.isBlank()) {
            TerminalSettings(
                wsUrl = null,
                apiKey = null,
                error = "Gateway API key is not configured",
            )
        } else {
            val normalizedUrl =
                try {
                    normalizeBaseUrl(gatewayUrl)
                } catch (_: IllegalArgumentException) {
                    return@remember TerminalSettings(
                        wsUrl = null,
                        apiKey = null,
                        error = "Gateway API URL is invalid",
                    )
                }
            val wsBaseUrl = normalizedUrl.replace("http://", "ws://").replace("https://", "wss://")
            TerminalSettings(
                wsUrl = "$wsBaseUrl/api/ws/terminal?session_id=${agentId.encodeURLParameter()}",
                apiKey = apiKey,
                error = null,
            )
        }
    }
}
