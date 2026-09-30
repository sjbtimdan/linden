package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.ledger_clear_all

/** One removable summary chip for an active ledger filter. */
internal data class ActiveFilter(
    val name: String,
    val testTag: String,
    val onClear: () -> Unit,
    val leadingColor: Color? = null,
)

/**
 * Removable chips reporting every active filter, shown whether the filter panel
 * is expanded or collapsed; "Clear all" appears once more than one filter is
 * active.
 */
@Composable
internal fun ActiveFilterChips(filters: List<ActiveFilter>, onClearAll: () -> Unit, modifier: Modifier = Modifier) {
    if (filters.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        filters.forEach { filter ->
            EntryFilterChip(
                name = filter.name,
                onClick = filter.onClear,
                leadingColor = filter.leadingColor,
                modifier = Modifier.testTag(filter.testTag),
            )
        }
        if (filters.size > 1) {
            TextButton(
                onClick = onClearAll,
                modifier = Modifier.testTag("clearAllFilters"),
            ) {
                Text(stringResource(Res.string.ledger_clear_all))
            }
        }
    }
}
