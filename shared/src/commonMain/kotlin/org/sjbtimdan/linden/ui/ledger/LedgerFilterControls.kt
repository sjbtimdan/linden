package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_all
import org.sjbtimdan.linden.ui.entry.displayName

/**
 * Same order as the type selector on the entry screen; EntryType.entries itself
 * is declared Income, Transfer, Expense.
 */
internal val typeOrder = listOf(EntryType.Expense, EntryType.Income, EntryType.Transfer)

/**
 * The chip filters of the current view, laid out inline under the search field —
 * no dialog. The type filter is a single-select chip row (All by default, then
 * [typeOptions], normally Expense/Income/Transfer) and narrows both the entries
 * list and the category totals; the categories view passes [typeOptions]
 * without Transfer, which never has a category. All stays selected whenever the
 * current filter is not among the offered types. The amount filter only applies
 * to the entries list ([showAmountFilter]). Category and account narrowing is
 * done from the search field's suggestion chips instead of dropdowns. Every
 * filter applies immediately; the removable summary chips below the period bar
 * keep reporting active filters.
 */
@Composable
fun LedgerFilterControls(
    typeFilter: EntryType?,
    onTypeFilterChange: (EntryType?) -> Unit,
    typeOptions: List<EntryType> = typeOrder,
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
        FilterChip(
            selected = typeFilter == null || typeFilter !in typeOptions,
            onClick = { onTypeFilterChange(null) },
            label = { Text(stringResource(Res.string.common_all)) },
            modifier = Modifier.testTag("typeFilter-All"),
        )
        typeOptions.forEach { type ->
            FilterChip(
                selected = typeFilter == type,
                onClick = { onTypeFilterChange(type) },
                label = { Text(type.displayName()) },
                modifier = Modifier.testTag("typeFilter-${type.name}"),
            )
        }
        if (showAmountFilter) {
            AmountFilterChip(
                filter = amountFilter,
                onApply = onAmountFilterChange,
                onClear = onClearAmountFilter,
            )
        }
    }
}
