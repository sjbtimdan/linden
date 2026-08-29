package org.sjbtimdan.linden.ui.ledger

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.ui.accounts.AccountWithBalance
import org.sjbtimdan.linden.ui.accounts.accountBalancesMinor
import org.sjbtimdan.linden.ui.accounts.entryDeltas

/**
 * Balance of each account at the end of [cutoff] (inclusive) in the account's own
 * currency: the initial balance plus the net of all [entries] dated on or before
 * [cutoff]. Accounts with no entries keep their initial balance.
 */
fun accountBalancesAtEnd(entries: List<Entry>, cutoff: LocalDate, accounts: List<Account>): List<AccountWithBalance> {
    val relevant = entries.filter { entry ->
        entry.createdAt.toLocalDateTime(entry.createdZone).date <= cutoff
    }
    val balances = accountBalancesMinor(entryDeltas(relevant), accounts)
    return accounts.map { account -> AccountWithBalance(account, balances.getValue(account.id)) }
}
