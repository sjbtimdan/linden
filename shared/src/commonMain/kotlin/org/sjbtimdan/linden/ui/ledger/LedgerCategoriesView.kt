package org.sjbtimdan.linden.ui.ledger

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.categories_empty_none
import org.sjbtimdan.linden.resources.ledger_action_add_categories
import org.sjbtimdan.linden.resources.ledger_categories_future_hidden
import org.sjbtimdan.linden.resources.ledger_categories_no_match
import org.sjbtimdan.linden.resources.ledger_categories_no_spending

/** The categories view: period-end totals with search, budget progress and empty states. */
@Composable
internal fun LedgerCategoriesView(
    categories: List<Category>,
    totals: List<CategoryWithTotal>,
    defaultCurrency: Currency,
    hideAmounts: Boolean,
    searchQuery: String,
    showFuture: Boolean,
    upcomingCount: Int,
    onEmptyAction: () -> Unit,
    onCategoryClick: (CategoryWithTotal) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filter = searchQuery.trim()
    val shownCategories =
        if (filter.isEmpty()) {
            totals
        } else {
            totals.filter {
                it.category?.name?.contains(filter, ignoreCase = true) == true
            }
        }
    CategoryTotalsList(
        categories = shownCategories,
        currency = defaultCurrency,
        hideAmounts = hideAmounts,
        emptyMessage = when {
            filter.isNotEmpty() && totals.isNotEmpty() ->
                stringResource(Res.string.ledger_categories_no_match)

            categories.isEmpty() -> stringResource(Res.string.categories_empty_none)

            // Spending exists but only after today, hidden by the show-future rule.
            !showFuture && upcomingCount > 0 ->
                stringResource(Res.string.ledger_categories_future_hidden)

            // Categories exist but none of them was used in the period.
            else -> stringResource(Res.string.ledger_categories_no_spending)
        },
        emptyActionLabel = if (filter.isEmpty() && categories.isEmpty()) {
            stringResource(Res.string.ledger_action_add_categories)
        } else {
            null
        },
        onEmptyAction = onEmptyAction,
        onCategoryClick = onCategoryClick,
        modifier = modifier,
    )
}
