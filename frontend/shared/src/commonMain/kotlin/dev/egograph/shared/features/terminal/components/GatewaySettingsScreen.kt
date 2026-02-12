package dev.egograph.shared.ui.terminal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import dev.egograph.shared.platform.PlatformPreferences
import dev.egograph.shared.platform.PlatformPrefsDefaults
import dev.egograph.shared.platform.PlatformPrefsKeys
import dev.egograph.shared.platform.getDefaultGatewayBaseUrl
import dev.egograph.shared.platform.isValidUrl
import dev.egograph.shared.platform.normalizeBaseUrl
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class GatewaySettingsScreen(
    private val onBack: () -> Unit = {},
) : Screen {
    @Composable
    override fun Content() {
        val preferences = koinInject<PlatformPreferences>()
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        var inputGatewayUrl by remember {
            mutableStateOf(
                preferences
                    .getString(
                        PlatformPrefsKeys.KEY_GATEWAY_API_URL,
                        PlatformPrefsDefaults.DEFAULT_GATEWAY_API_URL,
                    ).ifBlank { getDefaultGatewayBaseUrl() },
            )
        }

        var inputApiKey by remember {
            mutableStateOf(
                preferences.getString(
                    PlatformPrefsKeys.KEY_GATEWAY_API_KEY,
                    PlatformPrefsDefaults.DEFAULT_GATEWAY_API_KEY,
                ),
            )
        }

        fun saveSettings() {
            val normalizedGatewayUrl = normalizeBaseUrl(inputGatewayUrl)
            val trimmedApiKey = inputApiKey.trim()

            preferences.putString(PlatformPrefsKeys.KEY_GATEWAY_API_URL, normalizedGatewayUrl)
            preferences.putString(PlatformPrefsKeys.KEY_GATEWAY_API_KEY, trimmedApiKey)

            inputGatewayUrl = normalizedGatewayUrl
            inputApiKey = trimmedApiKey

            coroutineScope.launch {
                snackbarHostState.showSnackbar("Gateway settings saved")
            }
            onBack()
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                GatewaySettingsTopBar(onBack = onBack)
            },
        ) { paddingValues ->
            Surface(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
            ) {
                GatewaySettingsContent(
                    gatewayUrl = inputGatewayUrl,
                    onGatewayUrlChange = { inputGatewayUrl = it },
                    apiKey = inputApiKey,
                    onApiKeyChange = { inputApiKey = it },
                    onSave = { saveSettings() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GatewaySettingsTopBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text("Gateway Settings") },
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
}

@Composable
private fun GatewaySettingsContent(
    gatewayUrl: String,
    onGatewayUrlChange: (String) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    var isTokenVisible by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text(
            text = "Gateway API Configuration",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        OutlinedTextField(
            value = gatewayUrl,
            onValueChange = onGatewayUrlChange,
            label = { Text("Gateway API URL") },
            placeholder = { Text("http://100.x.x.x:8001") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = gatewayUrl.isNotBlank() && !isValidUrl(gatewayUrl),
            supportingText = {
                Text(
                    text = "Example: http://100.x.x.x:8001",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("Gateway API Key") },
            placeholder = { Text("Required") },
            visualTransformation =
                if (isTokenVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            trailingIcon = {
                val description = if (isTokenVisible) "Hide token" else "Show token"
                val icon = if (isTokenVisible) Icons.Default.LockOpen else Icons.Default.Lock
                IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                    Icon(imageVector = icon, contentDescription = description)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = isValidUrl(gatewayUrl) && apiKey.isNotBlank(),
        ) {
            Text("Save Gateway Settings")
        }
    }
}
