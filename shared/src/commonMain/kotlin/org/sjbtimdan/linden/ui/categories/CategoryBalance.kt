package org.sjbtimdan.linden.ui.categories

import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.ui.history.periodTotalMinor

/** A category paired with its net balance in the default currency (minor units). */
data class CategoryWithBalance(
    val category: Category,
    val balance: Long?,
)

/**
 * Net balance of [category]'s entries (income minus expenses, transfers excluded)
 * converted to [defaultCurrency] minor units. Returns null when an entry assigned to
 * the category is in a foreign currency with no stored rate against the default
 * currency, since the balance would be incomplete.
 */
fun categoryBalanceMinor(
    entries: List<Entry>,
    category: Category,
    defaultCurrency: Currency,
    rates: List<FxRate>,
): Long? = periodTotalMinor(
    entries.filter { it.category?.id == category.id },
    defaultCurrency,
    rates,
)
