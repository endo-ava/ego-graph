package dev.egograph.shared.features.chat.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.egograph.shared.core.domain.model.LLMModel
import dev.egograph.shared.core.ui.common.testTagResourceId

internal object ChatComposerMetrics {
    val outerHorizontalPadding = 20.dp
    val outerVerticalPadding = 12.dp
    val actionButtonsSpacing = 8.dp
    val containerMinHeight = 100.dp
    const val inputMinLines = 2
    const val inputMaxLines = 5
    val contentHorizontalPadding = 16.dp
    val contentTopPadding = 12.dp
    val contentBottomPadding = 8.dp
    val textLaneMinHeight = 50.dp
    val modelSelectorSpacing = 8.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatComposerField(
    text: String,
    onTextChange: (String) -> Unit,
    isLoading: Boolean,
    models: List<LLMModel>,
    selectedModelId: String?,
    isLoadingModels: Boolean,
    modelsError: String?,
    onModelSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value

    val colors = OutlinedTextFieldDefaults.colors()
    val shape: Shape = RoundedCornerShape(22.dp)

    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier =
            modifier
                .testTagResourceId("chat_input_field")
                .fillMaxWidth()
                .heightIn(min = ChatComposerMetrics.containerMinHeight),
        textStyle =
            LocalTextStyle.current.copy(color = colors.unfocusedTextColor),
        enabled = !isLoading,
        minLines = ChatComposerMetrics.inputMinLines,
        maxLines = ChatComposerMetrics.inputMaxLines,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = shape,
                        ),
                shape = shape,
                color = colors.unfocusedContainerColor,
                contentColor = colors.unfocusedTextColor,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = ChatComposerMetrics.contentHorizontalPadding,
                                top = ChatComposerMetrics.contentTopPadding,
                                end = ChatComposerMetrics.contentHorizontalPadding,
                                bottom = ChatComposerMetrics.contentBottomPadding,
                            ),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = ChatComposerMetrics.textLaneMinHeight),
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                text = "Type a message...",
                                color = colors.unfocusedPlaceholderColor,
                            )
                        }
                        innerTextField()
                    }
                    Spacer(modifier = Modifier.height(ChatComposerMetrics.modelSelectorSpacing))
                    ChatModelSelector(
                        models = models,
                        selectedModelId = selectedModelId,
                        isLoading = isLoadingModels,
                        error = modelsError,
                        onModelSelected = onModelSelected,
                        modifier = Modifier.align(Alignment.Start),
                    )
                }
            }
        },
    )
}

@Composable
internal fun MicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.testTagResourceId("mic_button"),
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = "Voice input",
        )
    }
}

@Composable
internal fun SendButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.testTagResourceId("send_button"),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
        )
    }
}
