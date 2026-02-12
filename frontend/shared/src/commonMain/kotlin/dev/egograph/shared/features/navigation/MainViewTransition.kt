package dev.egograph.shared.features.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import dev.egograph.shared.features.sidebar.MainView

@Composable
fun MainViewTransition(
    activeView: MainView,
    content: @Composable (MainView) -> Unit,
) {
    AnimatedContent(
        targetState = activeView,
        transitionSpec = {
            when {
                initialState == MainView.Chat && targetState == MainView.Terminal -> {
                    slideInHorizontally { fullWidth -> fullWidth } togetherWith slideOutHorizontally { fullWidth -> -fullWidth }
                }

                initialState == MainView.Terminal && targetState == MainView.Chat -> {
                    slideInHorizontally { fullWidth -> -fullWidth } togetherWith slideOutHorizontally { fullWidth -> fullWidth }
                }

                else -> {
                    ContentTransform(
                        targetContentEnter = fadeIn(),
                        initialContentExit = fadeOut(),
                    )
                }
            }
        },
        label = "main-view-transition",
    ) { targetView ->
        content(targetView)
    }
}
