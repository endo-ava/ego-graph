package dev.egograph.shared.features.navigation

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * スワイプジェスチャーでViewを切り替えるコンテナ
 *
 * @param activeView 現在のアクティブなView
 * @param onSwipeToSidebar スワイプでサイドバーへ移動するコールバック
 * @param onSwipeToTerminal スワイプでターミナルへ移動するコールバック
 * @param onSwipeToChat スワイプでチャットへ移動するコールバック
 * @param content 表示するコンテンツ
 */
@Composable
fun SwipeNavigationContainer(
    activeView: MainView,
    onSwipeToSidebar: () -> Unit,
    onSwipeToTerminal: () -> Unit,
    onSwipeToChat: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(activeView) {
                    var accumulatedDragX = 0f
                    var handled = false

                    detectHorizontalDragGestures(
                        onDragStart = {
                            accumulatedDragX = 0f
                            handled = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            if (handled) {
                                return@detectHorizontalDragGestures
                            }

                            accumulatedDragX += dragAmount
                            val swipeThreshold = size.width * 0.2f

                            when {
                                activeView == MainView.Chat && accumulatedDragX >= swipeThreshold -> {
                                    handled = true
                                    onSwipeToSidebar()
                                    change.consume()
                                }

                                activeView == MainView.Chat && accumulatedDragX <= -swipeThreshold -> {
                                    handled = true
                                    onSwipeToTerminal()
                                    change.consume()
                                }

                                activeView == MainView.Terminal && accumulatedDragX >= swipeThreshold -> {
                                    handled = true
                                    onSwipeToChat()
                                    change.consume()
                                }
                            }
                        },
                    )
                },
        content = content,
    )
}
