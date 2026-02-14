package dev.egograph.shared.features.navigation

import androidx.compose.runtime.Composable

/**
 * メインのナビゲーションHost
 *
 * スワイプジェスチャーと画面遷移を管理する。
 *
 * @param activeView 現在のアクティブなView
 * @param onSwipeToSidebar スワイプでサイドバーへ遷移するコールバック
 * @param onSwipeToTerminal スワイプでターミナルへ遷移するコールバック
 * @param onSwipeToChat スワイプでチャットへ遷移するコールバック
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
