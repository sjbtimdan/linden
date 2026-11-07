package org.sjbtimdan.linden.ui.ledger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.EntryType

/**
 * Type options offered by the categories view: transfers never have a category,
 * so a Transfer type filter is not offered there (the ViewModel treats one as
 * All anyway).
 */
private val categoryTypeOptions = listOf(EntryType.Expense, EntryType.Income)

/** The collapsible search/filter header and its panel. */
@Composable
internal fun LedgerFilterPanel(
    expanded: Boolean,
    viewMode: LedgerViewMode,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<Category>,
    accounts: List<Account>,
    activeCategoryId: Long?,
    activeAccountId: Long?,
    onCategorySelected: (Long) -> Unit,
    onAccountSelected: (Long) -> Unit,
    focusRequester: FocusRequester,
    typeFilter: EntryType?,
    onTypeFilterChange: (EntryType?) -> Unit,
    amountFilter: AmountFilter?,
    onAmountFilterChange: (AmountFilter?) -> Unit,
    onClearAmountFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = expanded, modifier = modifier) {
        Column {
            LedgerSearchPanel(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                viewMode = viewMode,
                categories = categories,
                accounts = accounts,
                activeCategoryId = activeCategoryId,
                activeAccountId = activeAccountId,
                onCategorySelected = onCategorySelected,
                onAccountSelected = onAccountSelected,
                focusRequester = focusRequester,
            )

            // The accounts view has no chip filters: a balance mixes every
            // entry type, so its panel only holds the search field. The other
            // views edit their filters inline — no dialog.
            if (viewMode != LedgerViewMode.Accounts) {
                Spacer(modifier = Modifier.height(8.dp))
                LedgerFilterControls(
                    typeFilter = typeFilter,
                    onTypeFilterChange = onTypeFilterChange,
                    typeOptions = if (viewMode == LedgerViewMode.Categories) {
                        categoryTypeOptions
                    } else {
                        typeOrder
                    },
                    showAmountFilter = viewMode == LedgerViewMode.Entries,
                    amountFilter = amountFilter,
                    onAmountFilterChange = onAmountFilterChange,
                    onClearAmountFilter = onClearAmountFilter,
                )
            }
        }
    }
}
