package org.sjbtimdan.linden.ui.insights

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.ui.entry.DateLanguage
import kotlin.math.roundToLong

/**
 * One month of the trend window. [expenseMinor] and [incomeMinor] are the
 * month's totals in default-currency minor units; either is null when that
 * type used a currency with no stored FX rate against the default currency
 * (the total would be incomplete). [isCurrent] marks the month that contains
 * "today", regardless of which window is being viewed.
 */
data class MonthTotal(
    val year: Int,
    val monthNumber: Int,
    val expenseMinor: Long?,
    val incomeMinor: Long?,
    val isCurrent: Boolean,
)

/** Index of a month since year zero: January of [year] is `year * 12`. */
internal fun monthIndexOf(year: Int, monthNumber: Int): Int = year * 12 + (monthNumber - 1)

/** Month index of [date]. */
internal fun monthIndexOf(date: LocalDate): Int = monthIndexOf(date.year, date.month.number)

/**
 * Expense and income totals of the [windowMonths] months ending with the month
 * of [windowEnd], one [MonthTotal] per month in ascending order. Transfers
 * never contribute; entries are bucketed by their own
 * [org.sjbtimdan.linden.model.Entry.createdZone] so a month boundary near
 * midnight never misplaces an entry. Months without entries total zero.
 * [isCurrent] compares against [currentMonth], the real "now", so a historical
 * window keeps its current-month marker.
 */
fun monthlyTotals(
    entries: List<Entry>,
    windowEnd: LocalDate,
    currentMonth: LocalDate,
    defaultCurrency: Currency,
    rates: List<FxRate>,
    windowMonths: Int = 12,
): List<MonthTotal> {
    require(windowMonths > 0)
    val anchorIndex = monthIndexOf(windowEnd)
    val firstIndex = anchorIndex - windowMonths + 1

    // Per month, per type, per source currency: the summed amounts.
    val sums = Array(windowMonths) { monthSums() }
    for (entry in entries) {
        val monthKey = entry.createdAt.toLocalDateTime(entry.createdZone).date
        val offset = monthIndexOf(monthKey) - firstIndex
        if (offset !in sums.indices) continue
        val type = entry.type
        if (type == EntryType.Transfer) continue
        val series = if (type == EntryType.Income) sums[offset].income else sums[offset].expense
        series.merge(entry.account.currency, entry.amount, Long::plus)
    }

    val ratesByQuote = rates
        .filter { it.baseCurrency == defaultCurrency }
        .associate { it.quoteCurrency to it.rate }
    val currentIndex = monthIndexOf(currentMonth)
    return List(windowMonths) { offset ->
        val index = firstIndex + offset
        val month = sums[offset]
        MonthTotal(
            year = index / 12,
            monthNumber = index % 12 + 1,
            expenseMinor = convertedTotal(month.expense, defaultCurrency, ratesByQuote),
            incomeMinor = convertedTotal(month.income, defaultCurrency, ratesByQuote),
            isCurrent = index == currentIndex,
        )
    }
}

private fun monthSums() = MonthSums(mutableMapOf(), mutableMapOf())

private class MonthSums(
    val expense: MutableMap<Currency, Long>,
    val income: MutableMap<Currency, Long>,
)

/**
 * Sums one series' per-currency amounts in the default currency; null when a
 * currency has no stored rate (the series total would be incomplete). An
 * empty series totals zero.
 */
private fun convertedTotal(
    sums: Map<Currency, Long>,
    defaultCurrency: Currency,
    ratesByQuote: Map<Currency, Double>,
): Long? {
    if (sums.isEmpty()) return 0L
    var total = 0L
    for ((currency, amount) in sums) {
        val converted = toDefaultMinorUnits(amount, currency, defaultCurrency, ratesByQuote)
            ?: return null
        total += converted
    }
    return total
}

/**
 * Amount difference of [current]'s expense against its predecessor's, in
 * default minor units; null when either month has no usable expense total.
 */
internal fun deltaToPrevious(current: MonthTotal?, previous: MonthTotal?): Long? {
    val currentTotal = current?.expenseMinor ?: return null
    val previousTotal = previous?.expenseMinor ?: return null
    return currentTotal - previousTotal
}

/**
 * Converts the [MonthlyTrendChart] inputs: each month contributes one value
 * per series, income first (it renders as the left bar of the pair); a null
 * [MonthTotal] series value charts as no bar (zero), since the chart has no
 * "unknown" value. January bars carry the year, so a window crossing a year
 * boundary never shows two bare "Jan" labels.
 */
internal fun monthlyTrendBars(months: List<MonthTotal>, language: DateLanguage): List<MonthlyTrendBar> =
    months.map { month ->
        MonthlyTrendBar(
            label = monthBarLabel(month.year, month.monthNumber, language),
            values = listOf(month.incomeMinor ?: 0L, month.expenseMinor ?: 0L),
            isCurrent = month.isCurrent,
        )
    }

/**
 * Short label under a bar, e.g. "Jul"; January gets the year appended so
 * months from different years stay distinguishable ("Jan '26", "2026年1月").
 */
internal fun monthBarLabel(year: Int, monthNumber: Int, language: DateLanguage): String {
    if (monthNumber != 1) return language.monthShort(monthNumber)
    return when (language) {
        DateLanguage.English, DateLanguage.Italian ->
            "${language.monthShort(1)} '${(year % 100).toString().padStart(2, '0')}"

        DateLanguage.Chinese -> language.monthYearText(1, year)
    }
}

/**
 * Mean of the window's complete monthly expense totals, rounded to the
 * nearest minor unit; null when no month of the window has a usable total.
 */
internal fun averageMonthlyExpense(months: List<MonthTotal>): Long? {
    val totals = months.mapNotNull { it.expenseMinor }
    if (totals.isEmpty()) return null
    return (totals.sum().toDouble() / totals.size).roundToLong()
}

/**
 * Converts [amount] from [from] into [defaultCurrency] minor units via
 * [ratesByQuote] (rates from the default currency into each quote currency).
 * A missing rate yields null; same-currency amounts pass through unchanged.
 */
private fun toDefaultMinorUnits(
    amount: Long,
    from: Currency,
    defaultCurrency: Currency,
    ratesByQuote: Map<Currency, Double>,
): Long? {
    if (from == defaultCurrency) return amount
    val rate = ratesByQuote[from] ?: return null
    return (amount.toDouble() / rate).roundToLong()
}
