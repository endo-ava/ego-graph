package dev.egograph.shared.features.settings

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.egograph.shared.core.platform.PlatformPreferences
import dev.egograph.shared.core.platform.PlatformPrefsDefaults
import dev.egograph.shared.core.platform.PlatformPrefsKeys
import dev.egograph.shared.core.platform.isValidUrl
import dev.egograph.shared.core.platform.normalizeBaseUrl
import dev.egograph.shared.core.settings.AppTheme
import dev.egograph.shared.core.settings.ThemeRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * アプリケーション設定画面のScreenModel
 *
 * API接続設定（URL、API Key）とテーマ設定の管理・保存を行う。
 * 入力値の検証、正規化、永続化を担当し、UI StateとOne-shotイベントを管理する。
 *
 * @property preferences プラットフォーム設定ストア（URL/Key永続化用）
 * @property themeRepository テーマ設定のRepository
 */

class SettingsScreenModel(
    private val preferences: PlatformPreferences,
    private val themeRepository: ThemeRepository,
) : ScreenModel {
    private val _state =
        MutableStateFlow(
            SettingsState(
                inputUrl =
                    preferences.getString(
                        PlatformPrefsKeys.KEY_API_URL,
                        PlatformPrefsDefaults.DEFAULT_API_URL,
                    ),
                inputKey =
                    preferences.getString(
                        PlatformPrefsKeys.KEY_API_KEY,
                        PlatformPrefsDefaults.DEFAULT_API_KEY,
                    ),
            ),
        )
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _effect = Channel<SettingsEffect>()
    val effect: Flow<SettingsEffect> = _effect.receiveAsFlow()

    init {
        screenModelScope.launch {
            themeRepository.theme.collect { theme ->
                _state.update { it.copy(selectedTheme = theme) }
            }
        }
    }

    fun onThemeSelected(theme: AppTheme) {
        themeRepository.setTheme(theme)
    }

    fun onUrlChange(value: String) {
        _state.update { it.copy(inputUrl = value) }
    }

    fun onKeyChange(value: String) {
        _state.update { it.copy(inputKey = value) }
    }

    fun saveSettings() {
        val current = _state.value
        if (current.isSaving) {
            return
        }

        screenModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            val trimmedUrl = current.inputUrl.trim()
            val normalizedUrl = if (isValidUrl(trimmedUrl)) normalizeBaseUrl(trimmedUrl) else null
            val savedUrl = normalizedUrl ?: current.inputUrl
            val savedKey = current.inputKey.trim()

            if (normalizedUrl != null) {
                preferences.putString(PlatformPrefsKeys.KEY_API_URL, normalizedUrl)
            }
            preferences.putString(PlatformPrefsKeys.KEY_API_KEY, savedKey)

            _state.update {
                it.copy(
                    inputUrl = savedUrl,
                    inputKey = savedKey,
                    isSaving = false,
                )
            }
            _effect.send(SettingsEffect.ShowMessage("Settings saved"))
            _effect.send(SettingsEffect.NavigateBack)
        }
    }
}
