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

                        // 画面左端（サイドバー領域）からのスワイプはModalNavigationDrawerに任せる
                        val sidebarEdgeWidth = 20f
                        val shouldProcessSwipe =
                            when (activeView) {
                                SidebarView.Chat -> startX > screenWidth / 2
                                SidebarView.Terminal -> startX > sidebarEdgeWidth
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
