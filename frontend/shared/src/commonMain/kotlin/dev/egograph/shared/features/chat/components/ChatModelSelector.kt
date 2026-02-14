package dev.egograph.shared.features.chat.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.egograph.shared.core.domain.model.LLMModel

@Composable
fun ChatModelSelector(
    models: List<LLMModel>,
    selectedModelId: String?,
    isLoading: Boolean,
    error: String?,
    onModelSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModelSelector(
        models = models,
        selectedModelId = selectedModelId,
        isLoading = isLoading,
        error = error,
        onModelSelected = onModelSelected,
        modifier = modifier,
    )
}
