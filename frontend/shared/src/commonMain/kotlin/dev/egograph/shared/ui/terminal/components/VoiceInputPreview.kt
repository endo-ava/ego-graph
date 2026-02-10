package dev.egograph.shared.ui.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.egograph.shared.platform.terminal.createSpeechRecognizer
import kotlinx.coroutines.launch

/**
 * 音声入力プレビューコンポーネント
 *
 * 音声認識を開始し、認識結果をプレビュー表示する。
 * ユーザーは送信前にテキストを確認・編集できる。
 *
 * @param onSend テキストを送信するコールバック
 * @param onCancel キャンセルコールバック
 * @param hasRecordAudioPermission RECORD_AUDIOパーミッションが許可されているか
 * @param requestRecordAudioPermission パーミッションをリクエストする関数
 * @param modifier Modifier
 */
@Composable
fun VoiceInputPreview(
    onSend: (String) -> Unit,
    onCancel: () -> Unit,
    hasRecordAudioPermission: () -> Boolean = { true },
    requestRecordAudioPermission: suspend () -> Boolean = { true },
    modifier: Modifier = Modifier,
) {
    var recognizedText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPermissionRequested by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val speechRecognizer = remember { createSpeechRecognizer() }

    // 音声認識の開始・停止
    LaunchedEffect(isRecording) {
        if (isRecording) {
            scope.launch {
                try {
                    speechRecognizer.startRecognition().collect { result ->
                        recognizedText = result
                    }
                } catch (e: Exception) {
                    errorMessage = e.message
                    isRecording = false
                }
            }
        } else {
            speechRecognizer.stopRecognition()
        }
    }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
        ) {
            // ヘッダー
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Voice Input",
                    style = MaterialTheme.typography.titleMedium,
                )

                // 録音インジケーター
                if (isRecording) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red),
                        )
                        Text(
                            text = "Recording...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // プレビューテキストエリア
            OutlinedTextField(
                value = recognizedText,
                onValueChange = { recognizedText = it },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("voice_input_preview_field"),
                placeholder = { Text("Tap the mic button to start speaking...") },
                enabled = !isRecording,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // アクションボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // マイクボタン（録音開始/停止）
                IconButton(
                    onClick = {
                        if (isRecording) {
                            isRecording = false
                        } else {
                            scope.launch {
                                // パーミッションチェック
                                if (!hasRecordAudioPermission()) {
                                    if (!isPermissionRequested) {
                                        isPermissionRequested = true
                                        val granted = requestRecordAudioPermission()
                                        if (!granted) {
                                            errorMessage = "Microphone permission is required for voice input"
                                            isRecording = false
                                            return@launch
                                        }
                                    } else {
                                        errorMessage = "Microphone permission is required for voice input"
                                        return@launch
                                    }
                                }

                                errorMessage = null
                                isRecording = true
                            }
                        }
                    },
                    modifier =
                        Modifier
                            .size(56.dp)
                            .testTag("voice_input_mic_button"),
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = if (isRecording) "Stop recording" else "Start recording",
                        tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                }

                // 送信・キャンセルボタン
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // キャンセルボタン
                    Button(
                        onClick = onCancel,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        modifier =
                            Modifier
                                .testTag("voice_input_cancel_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Cancel,
                            contentDescription = "Cancel",
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel")
                    }

                    // 送信ボタン
                    Button(
                        onClick = { onSend(recognizedText) },
                        enabled = recognizedText.isNotBlank() && !isRecording,
                        modifier =
                            Modifier
                                .testTag("voice_input_send_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send",
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send")
                    }
                }
            }
        }
    }
}
