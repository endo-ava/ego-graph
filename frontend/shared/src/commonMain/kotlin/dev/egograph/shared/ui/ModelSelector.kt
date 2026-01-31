package dev.egograph.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arkivanov.mvikotlin.extensions.coroutines.states
import dev.egograph.shared.dto.LLMModel
import dev.egograph.shared.store.chat.ChatIntent
import dev.egograph.shared.store.chat.ChatStore

@Composable
fun ModelSelector(
    store: ChatStore,
    modifier: Modifier = Modifier
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
        onModelSelected = { store.accept(ChatIntent.SelectModel(it)) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelector(
    models: List<LLMModel>,
    selectedModelId: String?,
    isLoading: Boolean,
    error: String?,
    onModelSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedModel = models.find { it.id == selectedModelId }

    val displayText = when {
        isLoading -> "Loading..."
        error != null -> "Error"
        models.isEmpty() -> "No models"
        selectedModel != null -> selectedModel.name
        else -> "Select Model"
    }

    val isEnabled = !isLoading && error == null && models.isNotEmpty()

    Box(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (isEnabled) expanded = !expanded }
        ) {
            OutlinedTextField(
                value = displayText,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                enabled = isEnabled,
                singleLine = true
            )

            if (isEnabled) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    models.forEach { model ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = model.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = formatCost(model),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onModelSelected(model.id)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
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
