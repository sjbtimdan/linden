package org.sjbtimdan.linden.ui.categories

import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.ui.history.sumInDefaultMinor

/** A category paired with its net balance in the default currency (minor units). */
data class CategoryWithBalance(
    val category: Category,
    val balance: Long?,
)

/**
 * Net balance of [categoryId]'s entries (income minus expenses, transfers excluded)
 * converted to [defaultCurrency] minor units from the per-currency [totals] produced
 * by the categoryTotals aggregate. Returns null when an entry currency has no stored
 * rate against the default currency, since the balance would be incomplete.
 */
fun categoryBalanceMinor(
    totals: Map<Pair<Long, Currency>, Long>,
    categoryId: Long,
    defaultCurrency: Currency,
    rates: List<FxRate>,
): Long? = sumInDefaultMinor(
    totals.filterKeys { it.first == categoryId }.map { (key, net) -> key.second to net },
    defaultCurrency,
    rates,
)
