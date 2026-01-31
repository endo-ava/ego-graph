package dev.egograph.shared.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.egograph.shared.platform.PlatformPreferences
import dev.egograph.shared.platform.PlatformPrefsDefaults
import dev.egograph.shared.platform.PlatformPrefsKeys
import dev.egograph.shared.settings.AppTheme
import dev.egograph.shared.settings.toAppTheme
import dev.egograph.shared.settings.toStorageString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: PlatformPreferences,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var selectedTheme by remember {
        val savedTheme = preferences.getString(
            PlatformPrefsKeys.KEY_THEME,
            PlatformPrefsDefaults.DEFAULT_THEME
        )
        mutableStateOf(savedTheme.toAppTheme())
    }

    var apiUrl by remember {
        mutableStateOf(
            preferences.getString(
                PlatformPrefsKeys.KEY_API_URL,
                PlatformPrefsDefaults.DEFAULT_API_URL
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    Button(onClick = onBack) {
                        Text("← Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                AppTheme.LIGHT.displayName.also { lightText ->
                    ThemeOption(
                        text = lightText,
                        selected = selectedTheme == AppTheme.LIGHT,
                        onClick = {
                            selectedTheme = AppTheme.LIGHT
                            preferences.putString(
                                PlatformPrefsKeys.KEY_THEME,
                                AppTheme.LIGHT.toStorageString()
                            )
                        }
                    )
                }

                AppTheme.DARK.displayName.also { darkText ->
                    ThemeOption(
                        text = darkText,
                        selected = selectedTheme == AppTheme.DARK,
                        onClick = {
                            selectedTheme = AppTheme.DARK
                            preferences.putString(
                                PlatformPrefsKeys.KEY_THEME,
                                AppTheme.DARK.toStorageString()
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "API Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = { newValue ->
                        apiUrl = newValue
                    },
                    label = { Text("API URL") },
                    placeholder = { Text("https://api.example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            preferences.putString(
                                PlatformPrefsKeys.KEY_API_URL,
                                apiUrl
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
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
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}
