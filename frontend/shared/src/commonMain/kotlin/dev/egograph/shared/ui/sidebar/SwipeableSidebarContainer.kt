package dev.egograph.shared.ui.sidebar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * チャット画面とターミナル画面間のスワイプ遷移を管理するコンテナ
 *
 * **重要**: このコンポーネントは ModalNavigationDrawer のサイドバー開閉ジェスチャーと共存する必要があります。
 * - チャット画面では左端領域（DRAWER_EDGE_WIDTH）からのスワイプをサイドバー用に確保
 * - それ以外の領域でチャット⇔ターミナルのスワイプを処理
 *
 * **バグ履歴**:
 * - 2026-02-11: screenWidth判定を画面中央（screenWidth/2）に変更したことで、
 *   左端からのサイドバースワイプがブロックされる問題が発生（commit 02b8923）
 * - 修正: DRAWER_EDGE_WIDTH を導入し、左端領域を明示的に除外
 */
@Composable
fun SwipeableSidebarContainer(
    activeView: SidebarView,
    onSwipeToTerminal: () -> Unit,
    onSwipeToChat: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val swipeOffset = remember { Animatable(0f) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()

    // ModalNavigationDrawerのサイドバー開閉ジェスチャー用の左端領域（dp単位）
    // この領域からのスワイプはModalNavigationDrawerに処理を委譲する
    val drawerEdgeWidth = 50f

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        x = swipeOffset.value.roundToInt(),
                        y = 0,
                    )
                }.pointerInput(activeView, screenWidth) {
                    val swipeThreshold = screenWidth * 0.3f

                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val startX = down.position.x

                        // CRITICAL: 左端領域（drawerEdgeWidth）を除外してサイドバージェスチャーを優先
                        // この条件を変更する場合、ModalNavigationDrawerのサイドバー開閉が妨げられないか必ず確認すること
                        val shouldProcessSwipe =
                            when (activeView) {
                                SidebarView.Chat -> startX > drawerEdgeWidth
                                SidebarView.Terminal -> true
                                else -> false
                            }

                        if (shouldProcessSwipe) {
                            drag(down.id) { change ->
                                val dragAmount = change.positionChange().x
                                val newOffset = swipeOffset.value + dragAmount

                                val boundedOffset =
                                    when (activeView) {
                                        SidebarView.Chat -> newOffset.coerceIn(-screenWidth, 0f)
                                        SidebarView.Terminal -> newOffset.coerceIn(0f, screenWidth)
                                        else -> swipeOffset.value
                                    }

                                change.consume()
                                scope.launch {
                                    swipeOffset.snapTo(boundedOffset)
                                }
                            }

                            when {
                                swipeOffset.value < -swipeThreshold && activeView == SidebarView.Chat -> {
                                    onSwipeToTerminal()
                                    scope.launch {
                                        swipeOffset.animateTo(
                                            0f,
                                            animationSpec = spring(),
                                        )
                                    }
                                }
                                swipeOffset.value > swipeThreshold && activeView == SidebarView.Terminal -> {
                                    onSwipeToChat()
                                    scope.launch {
                                        swipeOffset.animateTo(
                                            0f,
                                            animationSpec = spring(),
                                        )
                                    }
                                }
                                else -> {
                                    scope.launch {
                                        swipeOffset.animateTo(
                                            0f,
                                            animationSpec = spring(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
        content = content,
    )
}
