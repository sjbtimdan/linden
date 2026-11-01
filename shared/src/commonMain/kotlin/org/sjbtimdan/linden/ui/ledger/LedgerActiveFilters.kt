package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.ledger_uncategorized
import org.sjbtimdan.linden.resources.ledger_unknown_account
import org.sjbtimdan.linden.ui.entry.displayName
import org.sjbtimdan.linden.ui.theme.accentColor

/**
 * Removable chips reporting every active entries-view filter, shown whether
 * the filter panel is expanded or collapsed. The categories and accounts views
 * have no chip filters, so this renders nothing outside the entries view.
 */
@Composable
internal fun LedgerActiveFilters(
    viewMode: LedgerViewMode,
    searchQuery: String,
    typeFilter: EntryType?,
    categoryFilter: Long?,
    accountFilter: Long?,
    amountFilter: AmountFilter?,
    categories: List<Category>,
    accounts: List<Account>,
    onClearSearch: () -> Unit,
    onClearType: () -> Unit,
    onClearCategory: () -> Unit,
    onClearAccount: () -> Unit,
    onClearAmount: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (viewMode != LedgerViewMode.Entries) return

    val activeFilters = buildList {
        if (searchQuery.isNotBlank()) {
            add(
                ActiveFilter(
                    name = searchQuery.trim(),
                    testTag = "activeSearchFilterChip",
                    onClear = onClearSearch,
                ),
            )
        }
        typeFilter?.let { type ->
            add(
                ActiveFilter(
                    name = type.displayName(),
                    testTag = "activeTypeFilterChip",
                    onClear = onClearType,
                ),
            )
        }
        categoryFilter?.let { id ->
            val name = categories.firstOrNull { it.id == id }?.name
                ?: stringResource(Res.string.ledger_uncategorized)
            add(
                ActiveFilter(
                    name = name,
                    testTag = "categoryFilterChip",
                    onClear = onClearCategory,
                    leadingColor = accentColor(name),
                ),
            )
        }
        accountFilter?.let { id ->
            add(
                ActiveFilter(
                    name = accounts.firstOrNull { it.id == id }?.name
                        ?: stringResource(Res.string.ledger_unknown_account),
                    testTag = "accountFilterChip",
                    onClear = onClearAccount,
                ),
            )
        }
        amountFilter?.let { filter ->
            add(
                ActiveFilter(
                    name = filter.displayLabel(),
                    testTag = "activeAmountFilterChip",
                    onClear = onClearAmount,
                ),
            )
        }
    }
    if (activeFilters.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        ActiveFilterChips(
            filters = activeFilters,
            onClearAll = onClearAll,
            modifier = modifier,
        )
    }
}
