package dev.egograph.shared.features.systemprompt

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.egograph.shared.core.domain.model.SystemPromptName
import dev.egograph.shared.core.ui.common.testTagResourceId

/**
 * システムプロンプト選択タブ
 *
 * @param selectedTab 選択中のタブ
 * @param onTabSelected タブ選択コールバック
 * @param modifier Modifier
 */
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
                        .testTagResourceId("prompt_tab_${tab.apiName}"),
                text = { Text(tab.name) },
            )
        }
    }
}
