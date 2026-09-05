package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.entry.displayName

/**
 * The chip filters of the current view, laid out inline under the search field —
 * no dialog. Every filter applies immediately and carries an explicit "All"
 * option to reset it. The type filter always shows (it narrows both the entries
 * list and the category totals); the category, account and amount filters only
 * apply to the entries list ([showEntryFilters]). Changes apply immediately; the
 * removable summary chips below the period bar keep reporting active filters.
 */
@Composable
fun LedgerFilterControls(
    showEntryFilters: Boolean,
    typeFilter: EntryType?,
    onTypeFilterChange: (EntryType?) -> Unit,
    categories: List<Category>,
    categoryFilter: Long?,
    onCategoryFilterChange: (Long?) -> Unit,
    accounts: List<Account>,
    accountFilter: Long?,
    onAccountFilterChange: (Long?) -> Unit,
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
        if (showEntryFilters) {
            ChipDropdown(
                selected = categoryFilter,
                options = listOf(null) + categories.map { it.id },
                optionLabel = { id ->
                    id?.let { cid -> categories.firstOrNull { it.id == cid }?.name }
                        ?: "Category: All"
                },
                onSelect = onCategoryFilterChange,
                modifier = Modifier.testTag("categoryFilterDropdown"),
            )
            ChipDropdown(
                selected = accountFilter,
                options = listOf(null) + accounts.map { it.id },
                optionLabel = { id ->
                    id?.let { aid -> accounts.firstOrNull { it.id == aid }?.name }
                        ?: "Account: All"
                },
                onSelect = onAccountFilterChange,
                modifier = Modifier.testTag("accountFilterDropdown"),
            )
            AmountFilterChip(
                filter = amountFilter,
                onApply = onAmountFilterChange,
                onClear = onClearAmountFilter,
            )
        }
    }
}
