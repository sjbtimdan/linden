package org.sjbtimdan.linden.inflation

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.model.dayIn

/**
 * Display-time CPI adjustment over entries. While [enabled], an entry's amount
 * (a transfer's received amount included, in its own currency) is restated in
 * the latest stored month's money for its account currency; entries whose
 * currency has no series pass through unchanged. The kept objects are copies,
 * so editing an entry always works on the original amount.
 */
class EntryInflation(
    val enabled: Boolean,
    private val series: Map<Currency, CpiSeries>,
    private val zone: TimeZone,
) {
    /** The entry's adjusted sent amount, or null when no adjustment applies. */
    fun adjustedAmount(entry: Entry): Long? {
        if (!enabled) return null
        val factor = factorFor(entry, entry.account.currency) ?: return null
        return adjustMinor(entry.amount, factor)
    }

    /** [entries] restated in the latest month's money; unchanged when disabled. */
    fun adjustedEntries(entries: List<Entry>): List<Entry> {
        if (!enabled) return entries
        return entries.map(::adjustedEntry)
    }

    /**
     * [balance] in [currency] restated from [asOf]'s money into the latest stored
     * month's money, or null when the toggle is off or the currency has no series.
     * Used for period-end account balances, whose as-of date is the period end
     * rather than a single entry date.
     */
    fun adjustedBalance(balance: Long, currency: Currency, asOf: LocalDate): Long? {
        if (!enabled) return null
        val factor = inflationFactorPpm(series[currency], asOf) ?: return null
        return adjustMinor(balance, factor)
    }

    private fun adjustedEntry(entry: Entry): Entry {
        val factor = factorFor(entry, entry.account.currency) ?: return entry
        return when (entry) {
            is TransferEntry -> {
                val received = entry.toAmount
                val toFactor = received?.let { factorFor(entry, entry.toAccount.currency) }
                entry.copy(
                    amount = adjustMinor(entry.amount, factor),
                    toAmount = if (received != null && toFactor != null) adjustMinor(received, toFactor) else received,
                )
            }

            is ExpenseEntry -> entry.copy(amount = adjustMinor(entry.amount, factor))

            is IncomeEntry -> entry.copy(amount = adjustMinor(entry.amount, factor))
        }
    }

    private fun factorFor(entry: Entry, currency: Currency): Long? =
        inflationFactorPpm(series[currency], entry.dayIn(zone))
}
