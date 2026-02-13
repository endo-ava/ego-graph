package dev.egograph.shared.features.chat.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.egograph.shared.core.ui.components.ModelSelector
import dev.egograph.shared.features.chat.ChatScreenModel

@Composable
fun ChatModelSelector(
    screenModel: ChatScreenModel,
    modifier: Modifier = Modifier,
) {
    val state by screenModel.state.collectAsState()

    LaunchedEffect(Unit) {
        if (state.models.isEmpty() && !state.isLoadingModels) {
            screenModel.loadModels()
        }
    }

    ModelSelector(
        models = state.models,
        selectedModelId = state.selectedModel,
        isLoading = state.isLoadingModels,
        error = state.modelsError,
        onModelSelected = { modelId ->
            screenModel.selectModel(modelId)
        },
        modifier = modifier,
    )
}
