package dev.egograph.shared.features.terminal.settings

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.egograph.shared.core.platform.PlatformPreferences
import dev.egograph.shared.core.platform.PlatformPrefsDefaults
import dev.egograph.shared.core.platform.PlatformPrefsKeys
import dev.egograph.shared.core.platform.getDefaultGatewayBaseUrl
import dev.egograph.shared.core.platform.isValidUrl
import dev.egograph.shared.core.platform.normalizeBaseUrl
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Gateway設定画面のScreenModel
 *
 * Gateway接続情報（URL、API Key）の管理・保存・検証を行う。
 * 入力値の検証、正規化、永続化を担当し、UI StateとOne-shotイベントを管理する。
 *
 * @property preferences プラットフォーム設定ストア（URL/Key永続化用）
 */

class GatewaySettingsScreenModel(
    private val preferences: PlatformPreferences,
) : ScreenModel {
    private val _state = MutableStateFlow(GatewaySettingsState())
    val state: StateFlow<GatewaySettingsState> = _state.asStateFlow()

    private val _effect = Channel<GatewaySettingsEffect>()
    val effect: Flow<GatewaySettingsEffect> = _effect.receiveAsFlow()

    init {
        _state.update {
            it.copy(
                inputGatewayUrl =
                    preferences
                        .getString(
                            PlatformPrefsKeys.KEY_GATEWAY_API_URL,
                            PlatformPrefsDefaults.DEFAULT_GATEWAY_API_URL,
                        ).ifBlank { getDefaultGatewayBaseUrl() },
                inputApiKey =
                    preferences.getString(
                        PlatformPrefsKeys.KEY_GATEWAY_API_KEY,
                        PlatformPrefsDefaults.DEFAULT_GATEWAY_API_KEY,
                    ),
            )
        }
    }

    fun onGatewayUrlChange(value: String) {
        _state.update { it.copy(inputGatewayUrl = value) }
    }

    fun onApiKeyChange(value: String) {
        _state.update { it.copy(inputApiKey = value) }
    }

    fun saveSettings() {
        val current = _state.value
        if (current.isSaving || !isValidUrl(current.inputGatewayUrl) || current.inputApiKey.isBlank()) {
            return
        }

        screenModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val normalizedGatewayUrl = normalizeBaseUrl(current.inputGatewayUrl)
            val trimmedApiKey = current.inputApiKey.trim()

            preferences.putString(PlatformPrefsKeys.KEY_GATEWAY_API_URL, normalizedGatewayUrl)
            preferences.putString(PlatformPrefsKeys.KEY_GATEWAY_API_KEY, trimmedApiKey)

            _state.update {
                it.copy(
                    inputGatewayUrl = normalizedGatewayUrl,
                    inputApiKey = trimmedApiKey,
                    isSaving = false,
                )
            }
            _effect.send(GatewaySettingsEffect.ShowMessage("Gateway settings saved"))
            _effect.send(GatewaySettingsEffect.NavigateBack)
        }
    }
}
