package dev.egograph.shared.ui.sidebar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * チャット画面とターミナル画面間のスワイプ遷移を管理するコンテナ
 *
 * チャット画面では左端領域を除外して、ModalNavigationDrawerのサイドバースワイプと共存
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
    // サイドバー開閉用の左端領域（この領域ではスワイプ処理を行わない）
    val drawerEdgeWidth = 50f

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            x = swipeOffset.value.roundToInt(),
                            y = 0,
                        )
                    },
            content = content,
        )

        // スワイプジェスチャーを処理するオーバーレイ
        // チャット画面では左端領域を除外（サイドバー用に空ける）
        if (activeView == SidebarView.Chat) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .width((screenWidth - drawerEdgeWidth).dp)
                        .align(Alignment.TopEnd)
                        .pointerInput(screenWidth) {
                            val swipeThreshold = screenWidth * 0.3f

                            awaitEachGesture {
                                val down = awaitFirstDown()
                                drag(down.id) { change ->
                                    val dragAmount = change.positionChange().x
                                    val newOffset = swipeOffset.value + dragAmount
                                    val boundedOffset = newOffset.coerceIn(-screenWidth, 0f)

                                    change.consume()
                                    scope.launch {
                                        swipeOffset.snapTo(boundedOffset)
                                    }
                                }

                                when {
                                    swipeOffset.value < -swipeThreshold -> {
                                        onSwipeToTerminal()
                                        scope.launch {
                                            swipeOffset.animateTo(0f, animationSpec = spring())
                                        }
                                    }
                                    else -> {
                                        scope.launch {
                                            swipeOffset.animateTo(0f, animationSpec = spring())
                                        }
                                    }
                                }
                            }
                        },
            )
        } else if (activeView == SidebarView.Terminal) {
            // ターミナル画面では全域でスワイプ処理（右スワイプでチャットに戻る）
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(screenWidth) {
                            val swipeThreshold = screenWidth * 0.3f

                            awaitEachGesture {
                                val down = awaitFirstDown()
                                drag(down.id) { change ->
                                    val dragAmount = change.positionChange().x
                                    val newOffset = swipeOffset.value + dragAmount
                                    val boundedOffset = newOffset.coerceIn(0f, screenWidth)

                                    change.consume()
                                    scope.launch {
                                        swipeOffset.snapTo(boundedOffset)
                                    }
                                }

                                when {
                                    swipeOffset.value > swipeThreshold -> {
                                        onSwipeToChat()
                                        scope.launch {
                                            swipeOffset.animateTo(0f, animationSpec = spring())
                                        }
                                    }
                                    else -> {
                                        scope.launch {
                                            swipeOffset.animateTo(0f, animationSpec = spring())
                                        }
                                    }
                                }
                            }
                        },
            )
        }
    }
}
