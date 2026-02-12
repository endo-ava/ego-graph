package dev.egograph.shared.features.terminal.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.egograph.shared.core.domain.model.terminal.Session

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
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .statusBarsPadding(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Terminal Sessions",
                    style = MaterialTheme.typography.titleLarge,
                )
                Button(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh")
                }
            }

            OutlinedButton(
                onClick = onOpenGatewaySettings,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(32.dp).widthIn(min = 72.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                )
            }
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
        state = listState,
        modifier = modifier.fillMaxSize(),
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
