package dev.egograph.shared.features.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.egograph.shared.features.chat.ChatScreenModel
import dev.egograph.shared.platform.PlatformPreferences

/**
 * チャット入力コンポーネント
 *
 * テキスト入力とマイクボタンを含むチャット入力UI。
 * マイクボタンがクリックされると、コールバックが呼び出される。
 *
 * @param screenModel チャット画面モデル
 * @param preferences プラットフォーム設定
 * @param onSendMessage メッセージ送信コールバック
 * @param isLoading ローディング状態
 * @param onVoiceInputClick 音声入力ボタンクリックコールバック（オプション）
 * @param modifier Modifier
 */
@Composable
fun ChatInput(
    screenModel: ChatScreenModel,
    preferences: PlatformPreferences,
    onSendMessage: (String) -> Unit,
    isLoading: Boolean = false,
    onVoiceInputClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier = Modifier.weight(1f),
        ) {
            ChatTextField(
                text = text,
                onTextChange = { text = it },
                isLoading = isLoading,
            )

            ChatModelSelector(
                screenModel = screenModel,
                preferences = preferences,
                modifier =
                    Modifier
                        .padding(start = 12.dp, bottom = 8.dp)
                        .align(Alignment.BottomStart),
            )
        }

        // マイクボタン（コールバックが提供されている場合のみ表示）
        if (onVoiceInputClick != null) {
            MicButton(
                onClick = onVoiceInputClick,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        SendButton(
            enabled = text.isNotBlank() && !isLoading,
            onClick = {
                onSendMessage(text)
                text = ""
            },
        )
    }
}
