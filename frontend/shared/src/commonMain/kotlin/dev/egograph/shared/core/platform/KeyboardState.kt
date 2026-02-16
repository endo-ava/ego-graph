package dev.egograph.shared.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp

/**
 * キーボード状態を管理するクラス
 *
 * ソフトウェアキーボードの表示状態と高さを保持する。
 */
@Stable
class KeyboardState {
    /** キーボードが表示されているか */
    var isVisible by mutableStateOf(false)

    /** キーボードの高さ */
    var height: Dp by mutableStateOf(Dp(0f))
}

/**
 * キーボード状態を記憶するComposable
 *
 * プラットフォーム固有の実装により、キーボードの表示状態と高さを監視する。
 */
@Composable
expect fun rememberKeyboardState(): KeyboardState
