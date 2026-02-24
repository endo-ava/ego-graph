package dev.egograph.shared.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.egograph.shared.core.platform.PlatformPreferences
import dev.egograph.shared.core.platform.PlatformPrefsDefaults
import dev.egograph.shared.core.platform.PlatformPrefsKeys
import dev.egograph.shared.core.platform.isValidUrl
import dev.egograph.shared.core.platform.normalizeBaseUrl
import dev.egograph.shared.core.settings.AppTheme
import dev.egograph.shared.core.settings.ThemeRepository
import dev.egograph.shared.core.ui.common.testTagResourceId
import dev.egograph.shared.core.ui.components.SecretTextField
import dev.egograph.shared.core.ui.components.SettingsTopBar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 設定画面
 *
 * テーマ選択、API URL、API Keyの設定を行う。
 *
 * @param preferences プラットフォーム設定
 * @param onBack 戻るボタンコールバック
 */
@Composable
fun SettingsScreen(
    preferences: PlatformPreferences,
    onBack: () -> Unit,
) {
    val themeRepository = koinInject<ThemeRepository>()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedTheme by themeRepository.theme.collectAsState()

    var inputUrl by remember {
        mutableStateOf(
            preferences.getString(
                PlatformPrefsKeys.KEY_API_URL,
                PlatformPrefsDefaults.DEFAULT_API_URL,
            ),
        )
    }

    var inputKey by remember {
        mutableStateOf(
            preferences.getString(
                PlatformPrefsKeys.KEY_API_KEY,
                PlatformPrefsDefaults.DEFAULT_API_KEY,
            ),
        )
    }

    fun saveSettings() {
        val urlToSave = inputUrl.trim()
        if (isValidUrl(urlToSave)) {
            val normalizedUrl = normalizeBaseUrl(urlToSave)
            preferences.putString(
                PlatformPrefsKeys.KEY_API_URL,
                normalizedUrl,
            )
            inputUrl = normalizedUrl
        }
        val keyToSave = inputKey.trim()
        preferences.putString(
            PlatformPrefsKeys.KEY_API_KEY,
            keyToSave,
        )
        inputKey = keyToSave

        coroutineScope.launch {
            snackbarHostState.showSnackbar("Settings saved")
        }
        onBack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SettingsTopBar(title = "Settings", onBack = onBack)
        },
    ) { paddingValues ->
        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
            ) {
                AppearanceSection(
                    selectedTheme = selectedTheme,
                    onThemeSelected = { themeRepository.setTheme(it) },
                )

                Spacer(modifier = Modifier.height(24.dp))

                ApiConfigurationSection(
                    inputUrl = inputUrl,
                    onUrlChange = { inputUrl = it },
                    inputKey = inputKey,
                    onKeyChange = { inputKey = it },
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsActions(
                    inputUrl = inputUrl,
                    onSave = ::saveSettings,
                )
            }
        }
    }
}

@Composable
private fun AppearanceSection(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
) {
    Text(
        text = "Appearance",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    AppTheme.entries.forEach { theme ->
        ThemeOption(
            text = theme.displayName,
            selected = selectedTheme == theme,
            onClick = {
                onThemeSelected(theme)
            },
        )
    }
}

@Composable
private fun ApiConfigurationSection(
    inputUrl: String,
    onUrlChange: (String) -> Unit,
    inputKey: String,
    onKeyChange: (String) -> Unit,
) {
    Text(
        text = "API Configuration",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    OutlinedTextField(
        value = inputUrl,
        onValueChange = onUrlChange,
        label = { Text("API URL") },
        placeholder = { Text("https://api.egograph.dev") },
        modifier =
            Modifier
                .testTagResourceId("api_url_input")
                .fillMaxWidth(),
        singleLine = true,
        isError = inputUrl.isNotBlank() && !isValidUrl(inputUrl),
        supportingText = {
            Text(
                text = "Production: https://api.egograph.dev | Tailscale: http://100.x.x.x:8000",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )

    Spacer(modifier = Modifier.height(16.dp))

    SecretTextField(
        value = inputKey,
        onValueChange = onKeyChange,
        label = "API Key",
        placeholder = "Optional: Enter your API key",
        modifier =
            Modifier
                .testTagResourceId("api_key_input")
                .fillMaxWidth(),
        showContentDescription = "Show API Key",
        hideContentDescription = "Hide API Key",
    )
}

@Composable
private fun SettingsActions(
    inputUrl: String,
    onSave: () -> Unit,
) {
    Button(
        onClick = onSave,
        modifier =
            Modifier
                .testTagResourceId("save_settings_button")
                .fillMaxWidth(),
        enabled = isValidUrl(inputUrl),
    ) {
        Text("Save Settings")
    }
}

@Composable
private fun ThemeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}
