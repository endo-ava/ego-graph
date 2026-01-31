package dev.egograph.shared.ui.systemprompt

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.egograph.shared.dto.SystemPromptName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemPromptTabs(
    selectedTab: SystemPromptName,
    onTabSelected: (SystemPromptName) -> Unit,
    modifier: Modifier = Modifier,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        modifier = modifier,
    ) {
        SystemPromptName.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = { Text(tab.name) },
            )
        }
    }
}
