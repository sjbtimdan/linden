package org.sjbtimdan.linden.ui.ledger

import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.ui.accounts.entryDeltas
import org.sjbtimdan.linden.ui.entry.formatAmountCompact
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Net total of [entries] (income minus expenses, transfers excluded) converted to
 * [defaultCurrency] minor units. Returns null when a foreign currency has no stored
 * rate against the default currency, since the total would be incomplete.
 */
fun periodTotalMinor(entries: List<Entry>, defaultCurrency: Currency, rates: List<FxRate>): Long? =
    entriesNetInDefaultMinor(entries.filter { it.type != EntryType.Transfer }, defaultCurrency, rates)

/**
 * Net change to a single [account] over [entries] in [defaultCurrency] minor units:
 * income adds and expenses subtract as usual, and transfers count in the direction
 * they move money — out of the account at the sent amount (negative) or into it at
 * the received amount (positive). This is what the total shows while an account
 * filter is active in the ledger, where a transfer is a real change to that
 * account's balance. Returns null when the account's currency has no stored rate
 * against [defaultCurrency], since the total would be incomplete.
 */
fun accountNetInDefaultMinor(
    account: Account,
    entries: List<Entry>,
    defaultCurrency: Currency,
    rates: List<FxRate>,
): Long? {
    val delta = entryDeltas(entries)[account.id] ?: return 0L
    return toDefaultMinor(delta, account.currency, defaultCurrency, ratesByQuote(rates, defaultCurrency))
}

/**
 * Net of [entries] (transfers already excluded) in [defaultCurrency] minor units.
 * Income and expense amounts are summed per currency and converted separately, so the
 * net is exactly the income total plus the expense total, whatever the rounding: the
 * period total, the category totals and the sum of income and expense totals all
 * reconcile. Returns null when a foreign currency has no stored rate against the
 * default currency, since the total would be incomplete.
 */
internal fun entriesNetInDefaultMinor(entries: List<Entry>, defaultCurrency: Currency, rates: List<FxRate>): Long? {
    val (income, expenses) = entries.partition { it.type == EntryType.Income }
    val incomeTotal = sumInDefaultMinor(sumByCurrency(income), defaultCurrency, rates) ?: return null
    val expenseTotal = sumInDefaultMinor(
        sumByCurrency(expenses).map { (currency, amount) -> currency to -amount },
        defaultCurrency,
        rates,
    ) ?: return null
    return incomeTotal + expenseTotal
}

/**
 * Rates that convert from the default currency to each quote currency, indexed by
 * quote currency. A rate from [base] maps the quote's value into the default currency.
 */
internal fun ratesByQuote(rates: List<FxRate>, base: Currency): Map<Currency, Double> =
    rates.filter { it.baseCurrency == base }.associate { it.quoteCurrency to it.rate }

/**
 * Converts [amount] from [from] into [defaultCurrency] minor units using [ratesByQuote].
 * The division is performed in [Double] and rounds to the nearest minor unit; period
 * totals are display-only, so sub-minor rounding is acceptable.
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

/** [entries] grouped by their entry currency, each group's amounts summed in that currency. */
internal fun sumByCurrency(entries: List<Entry>): List<Pair<Currency, Long>> = entries
    .groupBy { it.account.currency }
    .map { (currency, group) -> currency to group.sumOf { it.amount } }

/**
 * Sums [groups] (amounts in their source currencies) converted to [defaultCurrency]
 * minor units. Returns null when a source currency has no stored rate against the
 * default currency, since the total would be incomplete.
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

/** Formats [minor] as a signed amount with currency symbol, e.g. "− 12.30 CHF". */
fun formatTotal(minor: Long, currency: Currency): String {
    val sign = when {
        minor < 0 -> "− "
        minor > 0 -> "+ "
        else -> ""
    }
    return "$sign${formatAmountCompact(abs(minor))} ${currency.symbol}"
}
