package org.sjbtimdan.linden.ui.accounts

import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.ui.ledger.ratesByQuote
import org.sjbtimdan.linden.ui.ledger.toDefaultMinor

/** An account paired with its current balance in the account's own currency (minor units). */
data class AccountWithBalance(
    val account: Account,
    val balance: Long,
)

/**
 * Net change per account in the account's own currency (minor units): income adds,
 * expenses and transfers subtract from the source; the transfer target gains the
 * received amount (or the sent amount for same-currency transfers). Mirrors the
 * accountDeltas SQL aggregate so balances can be computed over a date range.
 */
fun entryDeltas(entries: List<Entry>): Map<Long, Long> = buildMap {
    for (entry in entries) {
        when (entry) {
            is IncomeEntry -> merge(entry.account.id, entry.amount, Long::plus)

            is ExpenseEntry -> merge(entry.account.id, -entry.amount, Long::plus)

            is TransferEntry -> {
                merge(entry.account.id, -entry.amount, Long::plus)
                merge(entry.toAccount.id, entry.toAmount ?: entry.amount, Long::plus)
            }
        }
    }
}

/**
 * Current balance of each account in its own currency (minor units): the initial
 * balance plus the net change [deltas] computed by the accountDeltas aggregate.
 */
fun accountBalancesMinor(deltas: Map<Long, Long>, accounts: List<Account>): Map<Long, Long> =
    accounts.associate { account -> account.id to account.initialBalance + (deltas[account.id] ?: 0) }

/**
 * Net total of all account balances converted to [defaultCurrency] minor units.
 * Returns null when an account in a foreign currency has no stored rate against
 * the default currency, since the total would be incomplete.
 */
fun accountTotalMinor(accounts: List<AccountWithBalance>, defaultCurrency: Currency, rates: List<FxRate>): Long? {
    val ratesByQuote = ratesByQuote(rates, defaultCurrency)
    var total = 0L
    for (item in accounts) {
        val converted = toDefaultMinor(item.balance, item.account.currency, defaultCurrency, ratesByQuote)
            ?: return null
        total += converted
    }
    return total
}
