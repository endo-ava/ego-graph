package dev.egograph.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.egograph.shared.settings.AppTheme
import dev.egograph.shared.settings.ThemeRepository
import dev.egograph.shared.ui.sidebar.SidebarScreen
import dev.egograph.shared.ui.theme.EgoGraphTheme
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KoinContext {
                val themeRepository = koinInject<ThemeRepository>()
                val theme by themeRepository.theme.collectAsState()

                val darkTheme =
                    when (theme) {
                        AppTheme.LIGHT -> false
                        AppTheme.DARK -> true
                        AppTheme.SYSTEM -> isSystemInDarkTheme()
                    }

                EgoGraphTheme(darkTheme = darkTheme) {
                    SidebarScreen().Content()
                }
            }
        }
    }
}
