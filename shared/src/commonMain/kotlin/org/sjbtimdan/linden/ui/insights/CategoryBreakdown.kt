package org.sjbtimdan.linden.ui.insights

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import org.sjbtimdan.linden.model.Budget
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.IncomeEntry

/** One category of the selected month: totals, budget and previous-month total. */
data class CategoryBreakdownRow(
    val category: Category,
    /** The month's total in default minor units; null when a currency used that month had no stored rate. */
    val amountMinor: Long?,
    /** The previous month's total for the same category; null when absent or incomplete. */
    val previousMinor: Long?,
    /** The monthly budget limit for expense categories; null when none is set. */
    val budgetMinor: Long?,
    /** Number of entries in the selected month. */
    val count: Int,
)

/** Category rows of the selected month, split by type and ranked by amount. */
data class MonthBreakdown(
    val expenses: List<CategoryBreakdownRow>,
    val incomes: List<CategoryBreakdownRow>,
) {
    val isEmpty: Boolean get() = expenses.isEmpty() && incomes.isEmpty()
}

/** First day of the month [offset] months before [windowEnd] (0 is [windowEnd]'s month). */
internal fun monthBefore(windowEnd: LocalDate, offset: Int): LocalDate {
    val index = monthIndexOf(windowEnd) - offset
    return LocalDate(index / 12, index % 12 + 1, 1)
}

/**
 * Expense and income category totals of [month] in the default currency, each
 * ranked by amount and paired with the same category's previous-month total
 * (for change chips) and its budget limit (expense categories only, matched
 * by name like the budgets screen). A row's amount is null when a currency
 * used that month has no stored rate; categories without entries in [month]
 * never appear. Transfers never contribute.
 */
fun categoryBreakdown(
    entries: List<Entry>,
    month: LocalDate,
    defaultCurrency: Currency,
    rates: List<FxRate>,
    budgets: List<Budget>,
): MonthBreakdown {
    val monthIndex = monthIndexOf(month)
    val buckets = BreakdownBuckets()
    for (entry in entries) {
        val date = entry.createdAt.toLocalDateTime(entry.createdZone).date
        val slot = when (monthIndexOf(date) - monthIndex) {
            0 -> 0
            -1 -> 1
            else -> continue
        }
        when (entry.type) {
            EntryType.Expense -> {
                val expense = entry as ExpenseEntry
                val accumulator = buckets.expense.getOrPut(expense.category.id) {
                    CategoryAccumulator(expense.category)
                }
                if (slot == 0) {
                    accumulator.curSums.merge(expense.account.currency, expense.amount, Long::plus)
                    accumulator.count++
                } else {
                    accumulator.prevSums.merge(expense.account.currency, expense.amount, Long::plus)
                }
            }

            EntryType.Income -> {
                val income = entry as IncomeEntry
                val accumulator = buckets.income.getOrPut(income.category.id) {
                    CategoryAccumulator(income.category)
                }
                if (slot == 0) {
                    accumulator.curSums.merge(income.account.currency, income.amount, Long::plus)
                    accumulator.count++
                } else {
                    accumulator.prevSums.merge(income.account.currency, income.amount, Long::plus)
                }
            }

            EntryType.Transfer -> Unit
        }
    }

    val ratesByQuote = rates
        .filter { it.baseCurrency == defaultCurrency }
        .associate { it.quoteCurrency to it.rate }
    val budgetByName = budgets.associateBy { it.categoryName.lowercase() }
    return MonthBreakdown(
        expenses = buckets.expense.values
            .filter { it.curSums.isNotEmpty() }
            .map { accumulator ->
                CategoryBreakdownRow(
                    category = accumulator.category,
                    amountMinor = convertedTotal(accumulator.curSums, defaultCurrency, ratesByQuote),
                    previousMinor = accumulator.prevSums
                        .takeUnless { it.isEmpty() }
                        ?.let { convertedTotal(it, defaultCurrency, ratesByQuote) },
                    budgetMinor = budgetByName[accumulator.category.name.lowercase()]?.limitMinor,
                    count = accumulator.count,
                )
            }
            .sortedWith(
                compareByDescending<CategoryBreakdownRow> { it.amountMinor ?: Long.MIN_VALUE }
                    .thenBy { it.category.name.lowercase() },
            ),
        incomes = buckets.income.values
            .filter { it.curSums.isNotEmpty() }
            .map { accumulator ->
                CategoryBreakdownRow(
                    category = accumulator.category,
                    amountMinor = convertedTotal(accumulator.curSums, defaultCurrency, ratesByQuote),
                    previousMinor = accumulator.prevSums
                        .takeUnless { it.isEmpty() }
                        ?.let { convertedTotal(it, defaultCurrency, ratesByQuote) },
                    budgetMinor = null,
                    count = accumulator.count,
                )
            }
            .sortedWith(
                compareByDescending<CategoryBreakdownRow> { it.amountMinor ?: Long.MIN_VALUE }
                    .thenBy { it.category.name.lowercase() },
            ),
    )
}

private class BreakdownBuckets {
    val expense = mutableMapOf<Long, CategoryAccumulator>()
    val income = mutableMapOf<Long, CategoryAccumulator>()
}

private class CategoryAccumulator(
    val category: Category,
) {
    /** Slots: 0 = the selected month, 1 = the previous month. */
    val curSums = mutableMapOf<Currency, Long>()
    val prevSums = mutableMapOf<Currency, Long>()
    var count: Int = 0
}
