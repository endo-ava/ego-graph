package dev.egograph.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import dev.egograph.shared.platform.terminal.ActivityRecorder
import dev.egograph.shared.settings.AppTheme
import dev.egograph.shared.settings.ThemeRepository
import dev.egograph.shared.ui.sidebar.SidebarScreen
import dev.egograph.shared.ui.theme.EgoGraphTheme
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

/**
 * MainActivity
 *
 * アプリケーションのメインアクティビティ。
 * 音声認識のためにActivityRecorderにコンテキストを設定する。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set activity context for speech recognizer
        ActivityRecorder.currentActivity = this@MainActivity

        // Clear context when activity is destroyed
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    ActivityRecorder.currentActivity = null
                }
            },
        )

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
                    Navigator(SidebarScreen()) {
                        CurrentScreen()
                    }
                }
            }
        }
    }
}
