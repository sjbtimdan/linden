package org.sjbtimdan.linden.ui.accounts

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.uniqueName
import org.sjbtimdan.linden.ui.SearchableListViewModel

class AccountListViewModel(
    private val accountDao: AccountDao,
    entryDao: EntryDao,
    settingsDao: SettingsDao,
    allEntries: StateFlow<List<Entry>>,
) : SearchableListViewModel() {
    val defaultCurrency: StateFlow<Currency> = settingsDao.defaultCurrencyFlow().stateFlow(Currency.CHF)

    val accounts: StateFlow<List<Account>> = accountDao.getAll().filteredBySearch()

    /** Accounts referenced by at least one entry; their currency must not be changed. */
    val accountsWithEntries: StateFlow<Set<Long>> = entryDao.accountsWithEntries().stateFlow(emptySet())

    /**
     * Total value of each account in its own currency (minor units) across all
     * time, including future-dated entries: the initial balance plus the net of
     * every entry. The amount a hidden account would remove from the totals.
     */
    val allTimeBalances: StateFlow<Map<Long, Long>> = combine(
        accounts,
        allEntries,
    ) { accounts, entries ->
        val deltas = entryDeltas(entries)
        accounts.associate { account -> account.id to account.initialBalance + (deltas[account.id] ?: 0) }
    }.stateFlow(emptyMap())

    /** Hides or reveals [id]; hidden accounts stay in history but leave the ledger, pickers and filters. */
    fun setHidden(id: Long, hidden: Boolean) {
        viewModelScope.launch {
            accountDao.setHidden(id, hidden)
        }
    }

    /** Creates an account; returns false when the name is empty or already taken (case-insensitive). */
    fun createAccount(name: String, currency: Currency, initialBalance: Long = 0): Boolean {
        val trimmed = uniqueName(accounts.value, name) ?: return false
        viewModelScope.launch {
            accountDao.create(trimmed, currency, initialBalance)
        }
        return true
    }

    /** Updates an account; returns false when the name is empty or taken by another account (case-insensitive). */
    fun updateAccount(account: Account): Boolean {
        val trimmed = uniqueName(accounts.value, account.name, excludingId = account.id) ?: return false
        viewModelScope.launch {
            val current = accounts.value.firstOrNull { it.id == account.id }
            val currencyChanged = current != null && current.currency != account.currency
            // Changing the currency of an account with entries would reinterpret
            // every historical entry in the new currency, so it is refused.
            if (currencyChanged && account.id in accountsWithEntries.value) return@launch
            accountDao.update(account.copy(name = trimmed))
        }
        return true
    }

    /** Deletes an account; ignored when the account still has entries on it. */
    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            if (id in accountsWithEntries.value) return@launch
            accountDao.delete(id)
        }
    }
}
