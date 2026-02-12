package dev.egograph.shared.features.navigation

import androidx.compose.runtime.Composable
import dev.egograph.shared.features.sidebar.MainView

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
