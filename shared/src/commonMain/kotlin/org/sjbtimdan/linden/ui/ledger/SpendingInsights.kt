package org.sjbtimdan.linden.ui.ledger

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.FxRate

/** An inclusive calendar window used by the spending insights comparison. */
data class InsightWindow(
    val start: LocalDate,
    val end: LocalDate,
)

/** Spending in the selected month compared with the same day-range of the previous month. */
data class SpendingInsights(
    val currentSpent: Long,
    val previousSpent: Long,
    /** Rounded percent change vs the previous month; null when the previous month had no spending. */
    val changePercent: Int?,
    val topCategories: List<CategoryShare>,
)

/** An expense category with its converted spending and share of the month's total spending. */
data class CategoryShare(
    val category: Category?,
    val amount: Long,
    val sharePercent: Int,
)

/**
 * Windows for the month-to-date spending comparison: the month containing [anchor]
 * up to [today] (or its end, whichever is earlier) and the same day-range of the
 * previous month, clamped to that month's length. Returns null when the month has
 * no elapsed days yet (it is entirely in the future).
 */
fun monthInsightWindows(anchor: LocalDate, today: LocalDate): Pair<InsightWindow, InsightWindow>? {
    val monthStart = LedgerPeriod.Month.windowStart(anchor) ?: return null
    val monthEnd = LedgerPeriod.Month.windowEnd(anchor) ?: return null
    val currentEnd = minOf(today, monthEnd)
    if (currentEnd < monthStart) return null
    val previousAnchor = LedgerPeriod.Month.previousAnchor(anchor)
    val previousStart = LedgerPeriod.Month.windowStart(previousAnchor) ?: return null
    val previousMonthEnd = LedgerPeriod.Month.windowEnd(previousAnchor) ?: return null
    val offset = currentEnd.day - monthStart.day
    val previousEnd = minOf(previousStart.plus(offset, DateTimeUnit.DAY), previousMonthEnd)
    return InsightWindow(monthStart, currentEnd) to InsightWindow(previousStart, previousEnd)
}

/**
 * Spending insights for [currentEntries] compared against [previousEntries]: total
 * expenses in the default currency plus the top expense categories. Returns null when
 * a foreign currency has no stored rate against the default currency, since the
 * comparison would be incomplete.
 */
fun computeSpendingInsights(
    currentEntries: List<Entry>,
    previousEntries: List<Entry>,
    defaultCurrency: Currency,
    rates: List<FxRate>,
): SpendingInsights? {
    val currentSpent = expensesInDefaultMinor(currentEntries, defaultCurrency, rates) ?: return null
    val previousSpent = expensesInDefaultMinor(previousEntries, defaultCurrency, rates) ?: return null
    val changePercent = if (previousSpent == 0L) {
        null
    } else {
        ((currentSpent - previousSpent) * 100 / previousSpent).toInt()
    }
    return SpendingInsights(
        currentSpent = currentSpent,
        previousSpent = previousSpent,
        changePercent = changePercent,
        topCategories = topExpenseCategories(currentEntries, defaultCurrency, rates, limit = 3),
    )
}

/** Total expenses of [entries] (transfers excluded) in [defaultCurrency] minor units; null when a rate is missing. */
internal fun expensesInDefaultMinor(entries: List<Entry>, defaultCurrency: Currency, rates: List<FxRate>): Long? {
    val expenses = entries.filter { it.type == EntryType.Expense }
    val groups = expenses.groupBy { it.account.currency }
        .map { (currency, group) -> currency to group.sumOf { it.amount } }
    return sumInDefaultMinor(groups, defaultCurrency, rates)
}

/** Top [limit] expense categories by converted amount, each with its share of total spending. */
internal fun topExpenseCategories(
    entries: List<Entry>,
    defaultCurrency: Currency,
    rates: List<FxRate>,
    limit: Int,
): List<CategoryShare> {
    val expenses = entries.filter { it.type == EntryType.Expense }
    val total = expensesInDefaultMinor(expenses, defaultCurrency, rates) ?: return emptyList()
    if (total == 0L) return emptyList()
    return expenses
        .groupBy { it.category }
        .mapNotNull { (category, group) ->
            val amount = sumInDefaultMinor(
                group.groupBy { it.account.currency }
                    .map { (currency, entries) -> currency to entries.sumOf { it.amount } },
                defaultCurrency,
                rates,
            ) ?: return@mapNotNull null
            CategoryShare(category, amount, (amount * 100 / total).toInt())
        }
        .sortedByDescending { it.amount }
        .take(limit)
}
