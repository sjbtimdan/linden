package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.entry.displayName

/**
 * The chip filters of the current view, laid out inline under the search field —
 * no dialog. Every filter applies immediately and carries an explicit "All"
 * option to reset it. The type filter always shows (it narrows both the entries
 * list and the category totals); the amount filter only applies to the entries
 * list ([showAmountFilter]). Category and account narrowing is done from the
 * search field's suggestion chips instead of dropdowns. Changes apply
 * immediately; the removable summary chips below the period bar keep reporting
 * active filters.
 */
@Composable
fun LedgerFilterControls(
    typeFilter: EntryType?,
    onTypeFilterChange: (EntryType?) -> Unit,
    showAmountFilter: Boolean,
    amountFilter: AmountFilter?,
    onAmountFilterChange: (AmountFilter?) -> Unit,
    onClearAmountFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ChipDropdown(
            selected = typeFilter,
            options = listOf(null) + EntryType.entries,
            optionLabel = { it?.displayName() ?: "Types: All" },
            onSelect = onTypeFilterChange,
            modifier = Modifier.testTag("typeFilterDropdown"),
        )
        if (showAmountFilter) {
            AmountFilterChip(
                filter = amountFilter,
                onApply = onAmountFilterChange,
                onClear = onClearAmountFilter,
            )
        }
    }
}
