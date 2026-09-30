package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_clear
import org.sjbtimdan.linden.resources.ledger_filter_accounts
import org.sjbtimdan.linden.resources.ledger_filter_categories
import org.sjbtimdan.linden.resources.ledger_search_entries
import org.sjbtimdan.linden.ui.entry.OptionChipRow

/**
 * Search field of the filter panel with its structural suggestions. Typing a
 * category or account name in the entries view offers filter chips; tapping one
 * pins the exact entity and clears the free text so the two never combine. Chips
 * carry a kind-specific icon so a mixed row still reads clearly. The label
 * names what the search narrows: entry text, account names or category names.
 */
@Composable
internal fun LedgerSearchPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    viewMode: LedgerViewMode,
    categories: List<Category>,
    accounts: List<Account>,
    activeCategoryId: Long?,
    activeAccountId: Long?,
    onCategorySelected: (Long) -> Unit,
    onAccountSelected: (Long) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(
        when (viewMode) {
            LedgerViewMode.Entries -> Res.string.ledger_search_entries
            LedgerViewMode.Accounts -> Res.string.ledger_filter_accounts
            LedgerViewMode.Categories -> Res.string.ledger_filter_categories
        },
    )
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text(label) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { onQueryChange("") },
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(Res.string.common_clear),
                            )
                        }
                    }
                } else {
                    null
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .testTag("searchField"),
            )
        }

        if (viewMode == LedgerViewMode.Entries) {
            val suggestions = rankFilterSuggestions(
                query = query,
                categories = categories,
                accounts = accounts,
                activeCategoryId = activeCategoryId,
                activeAccountId = activeAccountId,
            )
            if (suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                OptionChipRow(
                    options = suggestions,
                    optionLabel = { it.name },
                    optionIcon = { suggestion ->
                        when (suggestion.kind) {
                            FilterSuggestionKind.Account -> Icons.Filled.AccountBalanceWallet

                            FilterSuggestionKind.Category ->
                                categories.firstOrNull { it.id == suggestion.id }
                                    ?.icon?.imageVector() ?: Icons.Filled.Category
                        }
                    },
                    onSelect = { suggestion ->
                        when (suggestion.kind) {
                            FilterSuggestionKind.Account ->
                                onAccountSelected(suggestion.id)

                            FilterSuggestionKind.Category ->
                                onCategorySelected(suggestion.id)
                        }
                        onQueryChange("")
                    },
                    modifier = Modifier.testTag("filterSuggestions"),
                )
            }
        }
    }
}
