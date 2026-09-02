package org.sjbtimdan.linden.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Currency

class AccountListViewModel(
    private val accountDao: AccountDao,
    private val entryDao: EntryDao,
    settingsDao: SettingsDao,
) : ViewModel() {
    val defaultCurrency: StateFlow<Currency> = settingsDao.defaultCurrencyFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Currency.CHF,
        )

    private val _searchQuery = MutableStateFlow("")

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val accounts: StateFlow<List<Account>> = combine(
        accountDao.getAll(),
        _searchQuery,
    ) { accounts, query ->
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) {
            accounts
        } else {
            accounts.filter { it.name.lowercase().contains(normalized) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    /** Accounts referenced by at least one entry; their currency must not be changed. */
    val accountsWithEntries: StateFlow<Set<Long>> = entryDao.accountsWithEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptySet(),
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /** Creates an account; returns false when the name is empty or already taken (case-insensitive). */
    fun createAccount(name: String, currency: Currency, initialBalance: Long = 0): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (accounts.value.any { it.name.equals(trimmed, ignoreCase = true) }) return false
        viewModelScope.launch {
            accountDao.create(trimmed, currency, initialBalance)
        }
        return true
    }

    /** Updates an account; returns false when the name is empty or taken by another account (case-insensitive). */
    fun updateAccount(account: Account): Boolean {
        val trimmed = account.name.trim()
        if (trimmed.isEmpty()) return false
        if (accounts.value.any { it.id != account.id && it.name.equals(trimmed, ignoreCase = true) }) return false
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
