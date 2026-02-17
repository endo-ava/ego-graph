package dev.egograph.shared.features.terminal.agentlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.egograph.shared.core.domain.model.terminal.Session
import dev.egograph.shared.core.domain.model.terminal.SessionStatus

/**
 * ターミナルセッション一覧コンポーネント
 *
 * @param sessions セッション一覧
 * @param selectedSessionId 選択中のセッションID
 * @param isLoading 読み込み中フラグ
 * @param error エラーメッセージ
 * @param onSessionClick セッション選択コールバック
 * @param onRefresh 更新コールバック
 * @param onOpenGatewaySettings Gateway設定を開くコールバック
 * @param modifier Modifier
 */
@Composable
fun SessionList(
    sessions: List<Session>,
    selectedSessionId: String?,
    isLoading: Boolean,
    error: String?,
    onSessionClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenGatewaySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeSessionCount = sessions.count { it.status == SessionStatus.CONNECTED }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(8.dp)
                            .background(
                                color = if (activeSessionCount > 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            ),
                )
                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "TERMINAL SESSIONS",
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(28.dp).widthIn(min = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync",
                        modifier = Modifier.size(16.dp),
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedButton(
                    onClick = onOpenGatewaySettings,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(28.dp).widthIn(min = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$activeSessionCount ACTIVE",
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                    ),
                color = if (activeSessionCount > 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.End),
            )
        }

        when {
            isLoading && sessions.isEmpty() -> {
                SessionListLoading(modifier = Modifier.fillMaxSize())
            }
            error != null -> {
                SessionListError(
                    error = error,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            sessions.isEmpty() -> {
                SessionListEmpty(modifier = Modifier.fillMaxSize())
            }
            else -> {
                SessionListContent(
                    sessions = sessions,
                    selectedSessionId = selectedSessionId,
                    onSessionClick = onSessionClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun SessionListContent(
    sessions: List<Session>,
    selectedSessionId: String?,
    onSessionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier.padding(vertical = 8.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(
            items = sessions,
            key = { it.sessionId },
        ) { session ->
            SessionListItem(
                session = session,
                isActive = session.sessionId == selectedSessionId,
                onClick = { onSessionClick(session.sessionId) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
