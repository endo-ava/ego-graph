package dev.egograph.shared.settings

import dev.egograph.shared.platform.PlatformPreferences
import dev.egograph.shared.platform.PlatformPrefsDefaults
import dev.egograph.shared.platform.PlatformPrefsKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface ThemeRepository {
    val theme: StateFlow<AppTheme>

    fun setTheme(theme: AppTheme)
}

class ThemeRepositoryImpl(
    private val preferences: PlatformPreferences,
) : ThemeRepository {
    private val _theme = MutableStateFlow(AppTheme.SYSTEM)
    override val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            val savedTheme =
                preferences.getString(
                    PlatformPrefsKeys.KEY_THEME,
                    PlatformPrefsDefaults.DEFAULT_THEME,
                )
            _theme.value = savedTheme.toAppTheme()
        }
    }

    override fun setTheme(theme: AppTheme) {
        preferences.putString(
            PlatformPrefsKeys.KEY_THEME,
            theme.toStorageString(),
        )
        _theme.value = theme
    }
}
