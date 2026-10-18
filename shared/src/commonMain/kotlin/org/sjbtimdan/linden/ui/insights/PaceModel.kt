package org.sjbtimdan.linden.ui.insights

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.dayIn
import org.sjbtimdan.linden.ui.money.ratesByQuote
import org.sjbtimdan.linden.ui.money.toDefaultMinor
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

/** Complete months of history used to learn the remaining-month spending pace. */
internal const val PACE_HISTORY_MONTHS = 6

private const val PACE_MEDIUM_HISTORY_MONTHS = 3
private const val PACE_HIGH_HISTORY_MONTHS = 6
private const val PACE_LOW_QUANTILE = 0.10
private const val PACE_HIGH_QUANTILE = 0.90

/** How much history backs a [PaceForecast]: 6+ months is high, 3+ medium, less is low. */
enum class PaceConfidence {
    Low,
    Medium,
    High,
}

/**
 * A month-to-date expense pace: what has been spent so far, where the month is
 * projected to end up, the likely range, and how much can still be spent per
 * day to stay at the usual monthly total.
 *
 * All amounts are in [Currency] minor units. [dailyAllowanceMinor] already
 * subtracts what was spent this month and floors at zero. [daysRemaining]
 * includes today, so it never reaches zero during the month. A non-empty
 * [missingCurrencies] means those currencies were left out of every total
 * because no FX rate against the default currency is stored.
 */
data class PaceForecast(
    val monthStart: LocalDate,
    val dayOfMonth: Int,
    val daysInMonth: Int,
    val daysRemaining: Int,
    val spentMinor: Long,
    val projectedMinor: Long,
    val likelyLowMinor: Long,
    val likelyHighMinor: Long,
    val typicalMonthMinor: Long,
    val dailyAllowanceMinor: Long,
    val historyMonths: Int,
    val confidence: PaceConfidence,
    val missingCurrencies: Set<Currency> = emptySet(),
)

/**
 * Projects the current month's expense total from the last
 * [historyMonths] complete months that contain expenses. Only [EntryType.Expense]
 * entries dated on or before [today] count; income and transfers are ignored,
 * and entries are bucketed by [zone] so a month boundary near midnight never
 * misplaces one.
 *
 * Each currency is forecast separately (month-to-date plus the median of the
 * historical spend still to come at the same day of month), then converted to
 * [defaultCurrency] once per currency. New currencies with no history fall back
 * to a linear pace. Returns null when no complete prior month has expense data.
 */
fun paceForecast(
    entries: List<Entry>,
    today: LocalDate,
    defaultCurrency: Currency,
    rates: List<FxRate>,
    zone: TimeZone,
    historyMonths: Int = PACE_HISTORY_MONTHS,
): PaceForecast? {
    require(historyMonths > 0) { "historyMonths must be positive" }

    val monthStart = startOfMonth(today)
    val daysInMonth = endOfMonth(today).day
    val dayOfMonth = today.day
    val expenses = entries.filter { it.type == EntryType.Expense && it.dayIn(zone) <= today }
    val current = currentSpend(expenses, monthStart, today, zone)
    val history = historyData(expenses, historyMonths, monthStart, dayOfMonth, zone)
    if (history.monthsWithData.isEmpty()) return null

    val ratesByQuote = ratesByQuote(rates, defaultCurrency)
    val missing = mutableSetOf<Currency>()
    var spent = 0L
    var projected = 0L
    var low = 0L
    var high = 0L
    var typical = 0L
    for (currency in history.currencies + current.keys) {
        val values = currencyValues(currency, current[currency] ?: 0L, history, dayOfMonth, daysInMonth)
        spent += convert(values.spent, currency, defaultCurrency, ratesByQuote, missing)
        projected += convert(values.projected, currency, defaultCurrency, ratesByQuote, missing)
        low += convert(values.low, currency, defaultCurrency, ratesByQuote, missing)
        high += convert(values.high, currency, defaultCurrency, ratesByQuote, missing)
        typical += convert(values.typical, currency, defaultCurrency, ratesByQuote, missing)
    }

    val daysRemaining = daysInMonth - dayOfMonth + 1
    val confidence = when {
        history.monthsWithData.size >= PACE_HIGH_HISTORY_MONTHS -> PaceConfidence.High
        history.monthsWithData.size >= PACE_MEDIUM_HISTORY_MONTHS -> PaceConfidence.Medium
        else -> PaceConfidence.Low
    }
    return PaceForecast(
        monthStart = monthStart,
        dayOfMonth = dayOfMonth,
        daysInMonth = daysInMonth,
        daysRemaining = daysRemaining,
        spentMinor = spent,
        projectedMinor = projected,
        likelyLowMinor = low,
        likelyHighMinor = high,
        typicalMonthMinor = typical,
        dailyAllowanceMinor = (typical - spent).coerceAtLeast(0) / daysRemaining,
        historyMonths = history.monthsWithData.size,
        confidence = confidence,
        missingCurrencies = missing,
    )
}

private data class HistoryData(
    val monthsWithData: List<LocalDate>,
    val totals: Map<Pair<LocalDate, Currency>, Long>,
    val throughDay: Map<Pair<LocalDate, Currency>, Long>,
    val currencies: Set<Currency>,
)

private data class CurrencyValues(
    val spent: Long,
    val projected: Long,
    val low: Long,
    val high: Long,
    val typical: Long,
)

private fun currentSpend(
    expenses: List<Entry>,
    monthStart: LocalDate,
    today: LocalDate,
    zone: TimeZone,
): Map<Currency, Long> {
    val sums = mutableMapOf<Currency, Long>()
    for (entry in expenses) {
        val date = entry.dayIn(zone)
        if (date >= monthStart && date <= today) {
            sums.merge(entry.account.currency, entry.amount, Long::plus)
        }
    }
    return sums
}

private fun historyData(
    expenses: List<Entry>,
    historyMonths: Int,
    currentMonthStart: LocalDate,
    dayOfMonth: Int,
    zone: TimeZone,
): HistoryData {
    val months = (1..historyMonths)
        .map { currentMonthStart.minus(it, DateTimeUnit.MONTH) }
        .toSet()
    val totals = mutableMapOf<Pair<LocalDate, Currency>, Long>()
    val throughDay = mutableMapOf<Pair<LocalDate, Currency>, Long>()
    val monthsWithData = mutableSetOf<LocalDate>()
    val currencies = mutableSetOf<Currency>()
    for (entry in expenses) {
        val date = entry.dayIn(zone)
        val month = startOfMonth(date)
        if (month !in months) continue
        val currency = entry.account.currency
        val key = month to currency
        totals.merge(key, entry.amount, Long::plus)
        monthsWithData += month
        currencies += currency
        val cutoff = minOf(dayOfMonth, endOfMonth(month).day)
        if (date.day <= cutoff) {
            throughDay.merge(key, entry.amount, Long::plus)
        }
    }
    return HistoryData(
        monthsWithData = monthsWithData.sorted(),
        totals = totals,
        throughDay = throughDay,
        currencies = currencies,
    )
}

private fun currencyValues(
    currency: Currency,
    currentSpent: Long,
    history: HistoryData,
    dayOfMonth: Int,
    daysInMonth: Int,
): CurrencyValues {
    if (currency !in history.currencies) {
        val projected = linearProjection(currentSpent, dayOfMonth, daysInMonth)
        return CurrencyValues(currentSpent, projected, projected, projected, projected)
    }
    val totals = history.monthsWithData.map { history.totals[it to currency] ?: 0L }
    val remaining = history.monthsWithData.map { month ->
        (history.totals[month to currency] ?: 0L) - (history.throughDay[month to currency] ?: 0L)
    }
    return CurrencyValues(
        spent = currentSpent,
        projected = currentSpent + median(remaining).roundToLong(),
        low = currentSpent + quantile(remaining, PACE_LOW_QUANTILE).roundToLong(),
        high = currentSpent + quantile(remaining, PACE_HIGH_QUANTILE).roundToLong(),
        typical = median(totals).roundToLong(),
    )
}

private fun convert(
    amount: Long,
    currency: Currency,
    defaultCurrency: Currency,
    ratesByQuote: Map<Currency, Double>,
    missing: MutableSet<Currency>,
): Long {
    if (amount == 0L) return 0L
    val converted = toDefaultMinor(amount, currency, defaultCurrency, ratesByQuote)
    if (converted == null) {
        missing += currency
        return 0L
    }
    return converted
}

private fun linearProjection(spent: Long, dayOfMonth: Int, daysInMonth: Int): Long =
    (spent.toDouble() * daysInMonth / dayOfMonth).roundToLong()

private fun quantile(values: List<Long>, fraction: Double): Double {
    if (values.isEmpty()) return 0.0
    val sorted = values.sorted()
    val position = (sorted.size - 1) * fraction
    val lower = floor(position).toInt()
    val upper = ceil(position).toInt()
    if (lower == upper) return sorted[lower].toDouble()
    val weight = position - lower
    return sorted[lower] * (1.0 - weight) + sorted[upper] * weight
}

private fun median(values: List<Long>): Double = quantile(values, 0.5)

private fun startOfMonth(date: LocalDate): LocalDate = LocalDate(date.year, date.month.number, 1)

private fun endOfMonth(date: LocalDate): LocalDate =
    startOfMonth(date).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
