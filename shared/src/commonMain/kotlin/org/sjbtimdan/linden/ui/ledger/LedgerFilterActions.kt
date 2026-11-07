package org.sjbtimdan.linden.ui.ledger

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.ledger_filters_header

/**
 * The single filter entry point in the period toolbar: a filter icon that
 * toggles the search/filter panel. It lights up (filled/tonal) while any
 * filter is active, so a narrowed view reads at a glance without a count.
 */
@Composable
fun LedgerFilterActions(hasActiveFilters: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    IconToggleButton(
        checked = hasActiveFilters,
        onCheckedChange = { onToggle() },
        modifier = modifier.testTag("filterAction"),
    ) {
        Icon(
            imageVector = Icons.Filled.FilterList,
            contentDescription = stringResource(Res.string.ledger_filters_header),
        )
    }
}
