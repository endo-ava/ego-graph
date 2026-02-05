package dev.egograph.shared.settings

import dev.egograph.shared.platform.PlatformPreferences
import dev.egograph.shared.platform.PlatformPrefsDefaults
import dev.egograph.shared.platform.PlatformPrefsKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ThemeRepository {
    val theme: StateFlow<AppTheme>

    fun setTheme(theme: AppTheme)
}

class ThemeRepositoryImpl(
    private val preferences: PlatformPreferences,
) : ThemeRepository {
    private val _theme = MutableStateFlow(loadTheme())
    override val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private fun loadTheme(): AppTheme {
        val savedTheme =
            preferences.getString(
                PlatformPrefsKeys.KEY_THEME,
                PlatformPrefsDefaults.DEFAULT_THEME,
            )
        return savedTheme.toAppTheme()
    }

    override fun setTheme(theme: AppTheme) {
        preferences.putString(
            PlatformPrefsKeys.KEY_THEME,
            theme.toStorageString(),
        )
        _theme.value = theme
    }
}
