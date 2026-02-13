package dev.egograph.shared.features.terminal.session.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * ターミナル画面のヘッダー
 *
 * エージェントID、接続状態、閉じるボタン、音声入力を含む。
 *
 * @param agentId エージェントID
 * @param isLoading 接続中フラグ
 * @param error エラーメッセージ（nullの場合は正常）
 * @param onClose 閉じるボタンコールバック
 * @param onVoiceInputToggle 音声入力切替コールバック
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalHeader(
    agentId: String,
    isLoading: Boolean,
    error: String?,
    onClose: () -> Unit,
    onVoiceInputToggle: () -> Unit = {},
) {
    val title =
        when {
            isLoading -> "Connecting to $agentId..."
            else -> agentId
        }

    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(text = title)
                Spacer(modifier = Modifier.size(8.dp))
                when {
                    isLoading -> {
                        Text(
                            "Connecting...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    error == null -> {
                        Text(
                            "●",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "Connected"
                                },
                        )
                    }
                    else -> {
                        Text(
                            "●",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "Disconnected"
                                },
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        },
        actions = {
            IconButton(onClick = onVoiceInputToggle) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Voice Input",
                )
            }
        },
    )
}
