package dev.egograph.shared.features.navigation

import androidx.compose.runtime.Composable
import dev.egograph.shared.features.sidebar.MainView

/**
 * メインのナビゲーションHost
 *
 * スワイプジェスチャーと画面遷移を管理する。
 *
 * @param activeView 現在のアクティブなView
 * @param onSwipeToSidebar スワイプでサイドバーへ移動するコールバック
 * @param onSwipeToTerminal スワイプでターミナルへ移動するコールバック
 * @param onSwipeToChat スワイプでチャットへ移動するコールバック
 * @param content 表示するコンテンツ
 */
@Composable
fun MainNavigationHost(
    activeView: MainView,
    onSwipeToSidebar: () -> Unit,
    onSwipeToTerminal: () -> Unit,
    onSwipeToChat: () -> Unit,
    content: @Composable (MainView) -> Unit,
) {
    SwipeNavigationContainer(
        activeView = activeView,
        onSwipeToSidebar = onSwipeToSidebar,
        onSwipeToTerminal = onSwipeToTerminal,
        onSwipeToChat = onSwipeToChat,
    ) {
        MainViewTransition(
            activeView = activeView,
            content = content,
        )
    }
}
