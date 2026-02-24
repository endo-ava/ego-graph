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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.egograph.shared.core.platform.isValidUrl
import dev.egograph.shared.core.settings.AppTheme
import dev.egograph.shared.core.ui.common.testTagResourceId
import dev.egograph.shared.core.ui.components.SecretTextField
import dev.egograph.shared.core.ui.components.SettingsTopBar
import org.koin.compose.koinInject

/**
 * 設定画面
 *
 * テーマ選択、API URL、API Keyの設定を行う。
 *
 * @param onBack 戻るボタンコールバック
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val screenModel = koinInject<SettingsScreenModel>()
    val state by screenModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        screenModel.effect.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                SettingsEffect.NavigateBack -> onBack()
            }
        }
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
                    selectedTheme = state.selectedTheme,
                    onThemeSelected = screenModel::onThemeSelected,
                )

                Spacer(modifier = Modifier.height(24.dp))

                ApiConfigurationSection(
                    inputUrl = state.inputUrl,
                    onUrlChange = screenModel::onUrlChange,
                    inputKey = state.inputKey,
                    onKeyChange = screenModel::onKeyChange,
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsActions(
                    inputUrl = state.inputUrl,
                    isSaving = state.isSaving,
                    onSave = screenModel::saveSettings,
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
    isSaving: Boolean,
    onSave: () -> Unit,
) {
    Button(
        onClick = onSave,
        modifier =
            Modifier
                .testTagResourceId("save_settings_button")
                .fillMaxWidth(),
        enabled = !isSaving && isValidUrl(inputUrl),
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
