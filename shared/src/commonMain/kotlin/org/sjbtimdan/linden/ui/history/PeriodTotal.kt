package org.sjbtimdan.linden.ui.history

import kotlin.math.abs
import kotlin.math.roundToLong
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.ui.entry.formatAmount

/**
 * Net total of [entries] (income minus expenses, transfers excluded) converted to
 * [defaultCurrency] minor units. Returns null when an entry in a foreign currency has
 * no stored rate against the default currency, since the total would be incomplete.
 */
fun periodTotalMinor(entries: List<Entry>, defaultCurrency: Currency, rates: List<FxRate>): Long? {
    val ratesByQuote = rates
        .filter { it.baseCurrency == defaultCurrency }
        .associate { it.quoteCurrency to it.rate }
    var total = 0L
    for (entry in entries) {
        if (entry.type == EntryType.Transfer) continue
        val signed = if (entry.type == EntryType.Income) entry.amount else -entry.amount
        val converted = toDefaultMinor(signed, entry.account.currency, defaultCurrency, ratesByQuote)
            ?: return null
        total += converted
    }
    return total
}

internal fun toDefaultMinor(
    amount: Long,
    from: Currency,
    defaultCurrency: Currency,
    ratesByQuote: Map<Currency, Double>,
): Long? {
    if (from == defaultCurrency) return amount
    val rate = ratesByQuote[from] ?: return null
    return (amount.toDouble() / rate).roundToLong()
}

/** Formats [minor] as a signed amount with currency symbol, e.g. "− 12.30 CHF". */
fun formatTotal(minor: Long, currency: Currency): String {
    val sign = when {
        minor < 0 -> "− "
        minor > 0 -> "+ "
        else -> ""
    }
    return "$sign${formatAmount(abs(minor))} ${currency.symbol}"
}
