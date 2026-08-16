package org.sjbtimdan.linden.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.FxRatesRepository
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRate

@OptIn(ExperimentalCoroutinesApi::class)
class AccountListViewModel(
    private val accountDao: AccountDao,
    entryDao: EntryDao,
    settingsDao: SettingsDao,
    fxRatesRepository: FxRatesRepository,
) : ViewModel() {
    val accounts: StateFlow<List<AccountWithBalance>> = combine(
        accountDao.getAll(),
        entryDao.getAll(),
    ) { accounts, entries ->
        val balances = accountBalancesMinor(entries, accounts)
        accounts.map { account -> AccountWithBalance(account, balances.getValue(account.id)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    val defaultCurrency: StateFlow<Currency> = settingsDao.defaultCurrencyFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Currency.CHF,
        )

    private val rates: StateFlow<List<FxRate>> = defaultCurrency
        .flatMapLatest { fxRatesRepository.ratesFor(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    /** Net total of all balances in the default currency; null when a rate is missing. */
    val totalMinor: StateFlow<Long?> = combine(
        accounts,
        defaultCurrency,
        rates,
    ) { accounts, currency, rates ->
        accountTotalMinor(accounts, currency, rates)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    fun createAccount(name: String, currency: Currency, initialBalance: Long = 0) {
        viewModelScope.launch {
            accountDao.create(name, currency, initialBalance)
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            accountDao.update(account)
        }
    }
}
