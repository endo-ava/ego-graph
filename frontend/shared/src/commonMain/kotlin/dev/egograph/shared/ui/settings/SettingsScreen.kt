package dev.egograph.shared.ui.settings

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.egograph.shared.platform.PlatformPreferences
import dev.egograph.shared.platform.PlatformPrefsDefaults
import dev.egograph.shared.platform.PlatformPrefsKeys
import dev.egograph.shared.platform.normalizeBaseUrl
import dev.egograph.shared.settings.AppTheme
import dev.egograph.shared.settings.toAppTheme
import dev.egograph.shared.settings.toStorageString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: PlatformPreferences,
    onBack: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTheme by remember {
        val savedTheme =
            preferences.getString(
                PlatformPrefsKeys.KEY_THEME,
                PlatformPrefsDefaults.DEFAULT_THEME,
            )
        mutableStateOf(savedTheme.toAppTheme())
    }

    var apiUrl by remember {
        mutableStateOf(
            preferences.getString(
                PlatformPrefsKeys.KEY_API_URL,
                PlatformPrefsDefaults.DEFAULT_API_URL,
            ),
        )
    }

    var apiKey by remember {
        mutableStateOf(
            preferences.getString(
                PlatformPrefsKeys.KEY_API_KEY,
                PlatformPrefsDefaults.DEFAULT_API_KEY,
            ),
        )
    }

    var inputUrl by remember { mutableStateOf(apiUrl) }
    var inputKey by remember { mutableStateOf(apiKey) }
    var isKeyVisible by remember { mutableStateOf(false) }

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
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                ThemeOption(
                    text = AppTheme.LIGHT.displayName,
                    selected = selectedTheme == AppTheme.LIGHT,
                    onClick = {
                        selectedTheme = AppTheme.LIGHT
                        preferences.putString(
                            PlatformPrefsKeys.KEY_THEME,
                            AppTheme.LIGHT.toStorageString(),
                        )
                    },
                )

                ThemeOption(
                    text = AppTheme.DARK.displayName,
                    selected = selectedTheme == AppTheme.DARK,
                    onClick = {
                        selectedTheme = AppTheme.DARK
                        preferences.putString(
                            PlatformPrefsKeys.KEY_THEME,
                            AppTheme.DARK.toStorageString(),
                        )
                    },
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "API Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { newValue ->
                        inputUrl = newValue
                    },
                    label = { Text("API URL") },
                    placeholder = { Text("https://api.egograph.dev") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = inputUrl.isNotBlank() && (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")),
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
                    onValueChange = { newValue ->
                        inputKey = newValue
                    },
                    label = { Text("API Key") },
                    placeholder = { Text("Optional: Enter your API key") },
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val description = if (isKeyVisible) "Hide API Key" else "Show API Key"
                        val icon = if (isKeyVisible) Icons.Default.LockOpen else Icons.Default.Lock

                        IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                            Icon(imageVector = icon, contentDescription = description)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val urlToSave = inputUrl.trim()
                        if (urlToSave.isNotBlank() && (urlToSave.startsWith("http://") || urlToSave.startsWith("https://"))) {
                            // URLを正規化して保存（末尾スラッシュを削除）
                            val normalizedUrl = normalizeBaseUrl(urlToSave)
                            preferences.putString(
                                PlatformPrefsKeys.KEY_API_URL,
                                normalizedUrl,
                            )
                            apiUrl = normalizedUrl
                            inputUrl = normalizedUrl
                        }
                        val keyToSave = inputKey.trim()
                        preferences.putString(
                            PlatformPrefsKeys.KEY_API_KEY,
                            keyToSave,
                        )
                        apiKey = keyToSave

                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Settings saved")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled =
                        inputUrl.trim().isNotEmpty() &&
                            (inputUrl.trim().startsWith("http://") || inputUrl.trim().startsWith("https://")),
                ) {
                    Text("Save Settings")
                }
            }
        }
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
