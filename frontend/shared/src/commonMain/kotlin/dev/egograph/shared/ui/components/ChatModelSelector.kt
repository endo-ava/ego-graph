package dev.egograph.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.mvikotlin.extensions.coroutines.states
import dev.egograph.shared.core.ui.components.ModelSelector
import dev.egograph.shared.platform.PlatformPreferences
import dev.egograph.shared.platform.PlatformPrefsKeys
import dev.egograph.shared.store.chat.ChatIntent
import dev.egograph.shared.store.chat.ChatStore

@Composable
fun ChatModelSelector(
    store: ChatStore,
    preferences: PlatformPreferences,
    modifier: Modifier = Modifier,
) {
    val state by store.states.collectAsState(initial = store.state)

    LaunchedEffect(Unit) {
        if (state.models.isEmpty() && !state.isLoadingModels) {
            store.accept(ChatIntent.LoadModels)
        }
    }

    ModelSelector(
        models = state.models,
        selectedModelId = state.selectedModel,
        isLoading = state.isLoadingModels,
        error = state.modelsError,
        onModelSelected = { modelId ->
            store.accept(ChatIntent.SelectModel(modelId))
            preferences.putString(PlatformPrefsKeys.KEY_SELECTED_MODEL, modelId)
        },
        modifier = modifier,
    )
}
