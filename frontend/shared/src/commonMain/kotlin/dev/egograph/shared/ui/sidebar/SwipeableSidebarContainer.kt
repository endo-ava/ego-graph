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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * チャット画面とターミナル画面間のスワイプ遷移を管理するコンテナ
 */
@Composable
fun SwipeableSidebarContainer(
    activeView: SidebarView,
    onSwipeToTerminal: () -> Unit,
    onSwipeToChat: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    // スワイプアニメーション用のオフセット
    val swipeOffset = remember { Animatable(0f) }
    val screenWidth = with(LocalDensity.current) { 400.dp.toPx() }
    val swipeThreshold = screenWidth * 0.3f // 画面幅の30%で遷移

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        x = swipeOffset.value.roundToInt(),
                        y = 0,
                    )
                }.pointerInput(activeView) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val startX = down.position.x

                        // チャット画面で左半分からのスワイプはサイドバー用として処理しない
                        val shouldProcessSwipe =
                            when (activeView) {
                                SidebarView.Chat -> startX > screenWidth / 2 // 右半分のみ処理
                                SidebarView.Terminal -> true // ターミナル画面では全域処理
                                else -> false
                            }

                        if (shouldProcessSwipe) {
                            drag(down.id) { change ->
                                val dragAmount = change.positionChange().x
                                val newOffset = (swipeOffset.value + dragAmount)

                                // オフセットを制限（画面幅の範囲内）
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

                            // ドラッグ終了時にアニメーションと画面切り替え
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
                                    // 閾値未達の場合は元の位置に戻す
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
