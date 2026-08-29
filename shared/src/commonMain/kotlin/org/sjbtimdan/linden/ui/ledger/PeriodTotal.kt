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
    val ratesByQuote = rates
        .filter { it.baseCurrency == defaultCurrency }
        .associate { it.quoteCurrency to it.rate }
    return toDefaultMinor(delta, account.currency, defaultCurrency, ratesByQuote)
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
    fun groupsOf(entries: List<Entry>) = entries
        .groupBy { it.account.currency }
        .map { (currency, group) -> currency to group.sumOf { it.amount } }
    val incomeTotal = sumInDefaultMinor(groupsOf(income), defaultCurrency, rates) ?: return null
    val expenseTotal = sumInDefaultMinor(
        groupsOf(expenses).map { (currency, amount) -> currency to -amount },
        defaultCurrency,
        rates,
    ) ?: return null
    return incomeTotal + expenseTotal
}

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
    val ratesByQuote = rates
        .filter { it.baseCurrency == defaultCurrency }
        .associate { it.quoteCurrency to it.rate }
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
