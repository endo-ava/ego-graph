package dev.egograph.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import dev.egograph.shared.platform.PlatformPreferences
import dev.egograph.shared.ui.sidebar.SidebarScreen
import dev.egograph.shared.ui.theme.EgoGraphTheme
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val preferences = koinInject<PlatformPreferences>()
            val themePreference =
                preferences.getString(
                    dev.egograph.shared.platform.PlatformPrefsKeys.KEY_THEME,
                    "system",
                )

            val darkTheme =
                when (themePreference) {
                    "light" -> false
                    "dark" -> true
                    else -> isSystemInDarkTheme()
                }

            EgoGraphTheme(darkTheme = darkTheme) {
                SidebarScreen().Content()
            }
        }
    }
}
