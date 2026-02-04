package dev.egograph.shared.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.mvikotlin.extensions.coroutines.states
import dev.egograph.shared.dto.LLMModel
import dev.egograph.shared.platform.PlatformPreferences
import dev.egograph.shared.platform.PlatformPrefsKeys
import dev.egograph.shared.store.chat.ChatIntent
import dev.egograph.shared.store.chat.ChatStore

@Composable
fun ModelSelector(
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

@Composable
fun ModelSelector(
    models: List<LLMModel>,
    selectedModelId: String?,
    isLoading: Boolean,
    error: String?,
    onModelSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedModel = models.find { it.id == selectedModelId }

    val displayText =
        when {
            isLoading -> "Loading..."
            error != null -> "Error"
            models.isEmpty() -> "No models"
            selectedModel != null -> selectedModel.name
            else -> "Select Model"
        }

    val isEnabled = !isLoading && error == null && models.isNotEmpty()

    Box(modifier = modifier) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.small,
            modifier =
                Modifier
                    .widthIn(max = 160.dp)
                    .clickable(enabled = isEnabled) { expanded = !expanded },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        if (isEnabled) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                models.forEach { model ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = model.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = formatCost(model),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            onModelSelected(model.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

private fun formatCost(model: LLMModel): String {
    if (model.isFree) return "Free"
    val inputCost = model.inputCostPer1m
    val outputCost = model.outputCostPer1m
    return if (inputCost == outputCost) {
        "$$inputCost/1M"
    } else {
        "In: $$inputCost/1M, Out: $$outputCost/1M"
    }
}
