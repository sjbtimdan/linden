package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.entry.displayName
import org.sjbtimdan.linden.ui.theme.DialogShape

/**
 * The single consolidated surface for the chip filters. Every filter of the
 * current view is edited here — the type filter always, plus the category,
 * account and amount filters when [showCategoryAndAccountFilters] (the entries
 * view; category totals are only grouped per type). Changes apply immediately;
 * "Done" closes the dialog. "Clear" (shown while a filter is active) resets
 * every chip filter to its "All" state and keeps the dialog open. Active
 * filters keep their removable summary chips below the period bar.
 */
@Composable
fun EntryFiltersDialog(
    showCategoryAndAccountFilters: Boolean,
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
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    // "Clear" shows only while a filter this dialog edits is set.
    val entryFiltersActive = showCategoryAndAccountFilters &&
        (categoryFilter != null || accountFilter != null || amountFilter != null)
    val hasActiveFilters = typeFilter != null || entryFiltersActive
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        title = { Text("Filters") },
        text = {
            Column {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
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
                    if (showCategoryAndAccountFilters) {
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
                    }
                }
                if (showCategoryAndAccountFilters) {
                    Spacer(modifier = Modifier.height(4.dp))
                    AmountFilterChip(
                        filter = amountFilter,
                        onApply = onAmountFilterChange,
                        onClear = onClearAmountFilter,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            if (hasActiveFilters) {
                TextButton(
                    onClick = onClearAll,
                    modifier = Modifier.testTag("clearFiltersButton"),
                ) {
                    Text("Clear", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
    )
}
