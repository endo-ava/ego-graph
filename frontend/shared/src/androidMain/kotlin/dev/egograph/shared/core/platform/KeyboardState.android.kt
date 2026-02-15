package dev.egograph.shared.core.platform

import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

@Composable
actual fun rememberKeyboardState(): KeyboardState {
    val density = LocalDensity.current
    val view = LocalView.current
    val state = remember { KeyboardState() }

    DisposableEffect(view) {
        val rect = Rect()
        val listener =
            ViewTreeObserver.OnGlobalLayoutListener {
                view.getWindowVisibleDisplayFrame(rect)
                val screenHeight = view.rootView.height
                val keyboardHeight = screenHeight - rect.bottom
                val isVisible = keyboardHeight > screenHeight / 4
                val heightDp = with(density) { keyboardHeight.toDp() }

                state.isVisible = isVisible
                state.height = if (isVisible) heightDp else 0.dp
            }

        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    return state
}
