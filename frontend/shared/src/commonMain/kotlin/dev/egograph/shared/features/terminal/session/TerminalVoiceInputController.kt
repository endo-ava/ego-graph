package dev.egograph.shared.features.terminal.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.egograph.shared.core.platform.terminal.createPermissionUtil
import dev.egograph.shared.core.platform.terminal.createSpeechRecognizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class TerminalVoiceInputController(
    val isActive: Boolean,
    val onToggle: () -> Unit,
)

@Composable
internal fun rememberTerminalVoiceInputController(
    onRecognizedText: (String) -> Unit,
    onError: (String) -> Unit,
): TerminalVoiceInputController {
    val coroutineScope = rememberCoroutineScope()
    val permissionUtil = remember { createPermissionUtil() }
    val speechRecognizer = remember { createSpeechRecognizer() }

    var voiceInputJob by remember { mutableStateOf<Job?>(null) }
    var isVoiceInputActive by remember { mutableStateOf(false) }
    val voiceInputMutex = remember { Mutex() }

    val stopVoiceInput: () -> Unit = {
        speechRecognizer.stopRecognition()
        voiceInputJob?.cancel()
        voiceInputJob = null
        isVoiceInputActive = false
    }

    val toggleVoiceInput: () -> Unit = {
        if (isVoiceInputActive) {
            stopVoiceInput()
        } else {
            coroutineScope.launch {
                // Acquire lock to prevent concurrent startups
                voiceInputMutex.withLock {
                    // Double-check after acquiring lock
                    if (isVoiceInputActive) {
                        return@withLock
                    }

                    val hasPermission =
                        permissionUtil.hasRecordAudioPermission() ||
                            permissionUtil.requestRecordAudioPermission().granted
                    if (!hasPermission) {
                        onError("Microphone permission is required")
                        return@withLock
                    }

                    onError("")
                    isVoiceInputActive = true
                    voiceInputJob?.cancel()
                    voiceInputJob =
                        launch {
                            try {
                                speechRecognizer.startRecognition().collectLatest { recognizedText ->
                                    if (recognizedText.isNotBlank()) {
                                        onRecognizedText(recognizedText)
                                    }
                                }
                            } catch (e: Exception) {
                                onError(e.message ?: "Voice input failed")
                            } finally {
                                isVoiceInputActive = false
                                voiceInputJob = null
                            }
                        }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopVoiceInput()
        }
    }

    return TerminalVoiceInputController(
        isActive = isVoiceInputActive,
        onToggle = toggleVoiceInput,
    )
}
