package dev.egograph.shared.ui.systemprompt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import dev.egograph.shared.dto.SystemPromptName
import dev.egograph.shared.repository.SystemPromptRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class SystemPromptEditorScreen(
    private val onBack: () -> Unit = {},
) : Screen {
    @Composable
    override fun Content() {
        val repository = koinInject<SystemPromptRepository>()
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        var selectedTab by remember { mutableStateOf(SystemPromptName.USER) }
        var originalContent by remember { mutableStateOf("") }
        var draftContent by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        suspend fun fetchContent() {
            isLoading = true
            try {
                val result = repository.getSystemPrompt(selectedTab)
                result
                    .onSuccess {
                        originalContent = it.content
                        draftContent = it.content
                    }.onFailure {
                        snackbarHostState.showSnackbar("Error: ${it.message}")
                    }
            } finally {
                isLoading = false
            }
        }

        // Fetch content when tab changes
        LaunchedEffect(selectedTab) {
            fetchContent()
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                Column {
                    SystemPromptTabs(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                    )
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = onBack,
                            enabled = !isLoading,
                            modifier =
                                Modifier
                                    .semantics { testTagsAsResourceId = true }
                                    .testTag("back_button"),
                        ) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        val result = repository.updateSystemPrompt(selectedTab, draftContent)
                                        result
                                            .onSuccess {
                                                originalContent = it.content
                                                draftContent = it.content
                                                snackbarHostState.showSnackbar("Saved successfully")
                                            }.onFailure {
                                                snackbarHostState.showSnackbar("Error: ${it.message}")
                                            }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading && draftContent != originalContent,
                            modifier =
                                Modifier
                                    .semantics { testTagsAsResourceId = true }
                                    .testTag("save_prompt_button"),
                        ) {
                            Text("Save")
                        }
                    }
                }
            },
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(16.dp),
                    )
                }

                SystemPromptEditor(
                    content = draftContent,
                    onContentChange = { draftContent = it },
                    enabled = !isLoading,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(16.dp),
                )
            }
        }
    }
}
