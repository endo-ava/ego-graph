package dev.egograph.shared.core.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * TestTag extensions for Compose
 */
internal fun Modifier.testTagResourceId(tag: String): Modifier =
    semantics { testTagsAsResourceId = true }
        .testTag(tag)
