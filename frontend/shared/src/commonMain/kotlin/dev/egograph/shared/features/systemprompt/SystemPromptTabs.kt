package dev.egograph.shared.ui.systemprompt

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import dev.egograph.shared.dto.SystemPromptName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemPromptTabs(
    selectedTab: SystemPromptName,
    onTabSelected: (SystemPromptName) -> Unit,
    modifier: Modifier = Modifier,
) {
    TabRow(
        selectedTabIndex = selectedTab.ordinal,
        modifier = modifier,
    ) {
        SystemPromptName.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                modifier =
                    Modifier
                        .semantics { testTagsAsResourceId = true }
                        .testTag("prompt_tab_${tab.apiName}"),
                text = { Text(tab.name) },
            )
        }
    }
}
