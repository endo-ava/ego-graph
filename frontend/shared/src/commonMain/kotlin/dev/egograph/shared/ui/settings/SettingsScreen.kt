package dev.egograph.shared.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.egograph.shared.platform.PlatformPreferences
import dev.egograph.shared.platform.PlatformPrefsDefaults
import dev.egograph.shared.platform.PlatformPrefsKeys
import dev.egograph.shared.platform.normalizeBaseUrl
import dev.egograph.shared.settings.AppTheme
import dev.egograph.shared.settings.ThemeRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
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

    var isKeyVisible by remember { mutableStateOf(false) }

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
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier =
                            Modifier
                                .height(32.dp)
                                .widthIn(min = 72.dp),
                    ) {
                        Text("Back")
                    }
                },
            )
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
                    isKeyVisible = isKeyVisible,
                    onKeyVisibilityChange = { isKeyVisible = it },
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingsActions(
                    inputUrl = inputUrl,
                    onSave = { saveSettings() },
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

    ThemeOption(
        text = AppTheme.LIGHT.displayName,
        selected = selectedTheme == AppTheme.LIGHT,
        onClick = {
            onThemeSelected(AppTheme.LIGHT)
        },
    )

    ThemeOption(
        text = AppTheme.DARK.displayName,
        selected = selectedTheme == AppTheme.DARK,
        onClick = {
            onThemeSelected(AppTheme.DARK)
        },
    )
}

@Composable
private fun ApiConfigurationSection(
    inputUrl: String,
    onUrlChange: (String) -> Unit,
    inputKey: String,
    onKeyChange: (String) -> Unit,
    isKeyVisible: Boolean,
    onKeyVisibilityChange: (Boolean) -> Unit,
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
                .semantics { testTagsAsResourceId = true }
                .testTag("api_url_input")
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

    OutlinedTextField(
        value = inputKey,
        onValueChange = onKeyChange,
        label = { Text("API Key") },
        placeholder = { Text("Optional: Enter your API key") },
        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val description = if (isKeyVisible) "Hide API Key" else "Show API Key"
            val icon = if (isKeyVisible) Icons.Default.LockOpen else Icons.Default.Lock

            IconButton(onClick = { onKeyVisibilityChange(!isKeyVisible) }) {
                Icon(imageVector = icon, contentDescription = description)
            }
        },
        modifier =
            Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("api_key_input")
                .fillMaxWidth(),
        singleLine = true,
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
                .semantics { testTagsAsResourceId = true }
                .testTag("save_settings_button")
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

private fun isValidUrl(url: String): Boolean {
    val trimmed = url.trim()
    return trimmed.isNotEmpty() && (trimmed.startsWith("http://") || trimmed.startsWith("https://"))
}
