package dev.egograph.shared.features.chat.components

import androidx.compose.foundation.layout.Arrangement
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
import dev.egograph.shared.core.domain.model.LLMModel

@Composable
fun ChatComposer(
    models: List<LLMModel>,
    selectedModelId: String?,
    isLoadingModels: Boolean,
    modelsError: String?,
    onModelSelected: (String) -> Unit,
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
                .padding(
                    horizontal = ChatComposerMetrics.outerHorizontalPadding,
                    vertical = ChatComposerMetrics.outerVerticalPadding,
                ),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(ChatComposerMetrics.actionButtonsSpacing),
    ) {
        ChatComposerField(
            text = text,
            onTextChange = { text = it },
            isLoading = isLoading,
            models = models,
            selectedModelId = selectedModelId,
            isLoadingModels = isLoadingModels,
            modelsError = modelsError,
            onModelSelected = onModelSelected,
            modifier = Modifier.weight(1f),
        )

        if (onVoiceInputClick != null) {
            MicButton(
                onClick = onVoiceInputClick,
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
