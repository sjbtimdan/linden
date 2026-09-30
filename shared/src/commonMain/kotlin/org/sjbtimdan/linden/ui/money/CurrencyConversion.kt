package org.sjbtimdan.linden.ui.money

import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRate
import kotlin.math.roundToLong

/**
 * Rates that convert from [base] to each quote currency, indexed by quote
 * currency. A rate from [base] maps the quote's value into the base currency.
 */
internal fun ratesByQuote(rates: List<FxRate>, base: Currency): Map<Currency, Double> =
    rates.filter { it.baseCurrency == base }.associate { it.quoteCurrency to it.rate }

/**
 * Converts [amount] from [from] into [defaultCurrency] minor units using
 * [ratesByQuote]. The division is performed in [Double] and rounds to the nearest
 * minor unit; display totals accept sub-minor rounding. Same-currency amounts
 * pass through unchanged.
 */
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

/**
 * Sums [groups] (amounts in their source currencies) converted to [defaultCurrency]
 * minor units. Every group is converted separately, so its rounding happens once.
 * Returns null when a source currency has no stored rate against the default
 * currency, since the total would be incomplete.
 */
internal fun sumInDefaultMinor(
    groups: Collection<Pair<Currency, Long>>,
    defaultCurrency: Currency,
    rates: List<FxRate>,
): Long? {
    val ratesByQuote = ratesByQuote(rates, defaultCurrency)
    var total = 0L
    for ((from, amount) in groups) {
        val converted = toDefaultMinor(amount, from, defaultCurrency, ratesByQuote)
            ?: return null
        total += converted
    }
    return total
}

/** [sumInDefaultMinor] over currency-keyed sums, converting each currency once. */
internal fun sumInDefaultMinor(sums: Map<Currency, Long>, defaultCurrency: Currency, rates: List<FxRate>): Long? =
    sumInDefaultMinor(sums.toList(), defaultCurrency, rates)
