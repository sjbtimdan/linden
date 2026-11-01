package org.sjbtimdan.linden.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.model.ThemeMode

/**
 * Theme choices as wrapping chips, shared by Settings and the first-run preview.
 * A segmented row runs out of room once System/Light/Dark/AMOLED share one
 * line, and the chips match the language and currency pickers.
 */
@Composable
internal fun ThemeModePicker(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    tagPrefix: String? = null,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(mode.displayName()) },
                modifier = if (tagPrefix == null) {
                    Modifier
                } else {
                    Modifier.testTag("$tagPrefix${mode.name}")
                },
            )
        }
    }
}
