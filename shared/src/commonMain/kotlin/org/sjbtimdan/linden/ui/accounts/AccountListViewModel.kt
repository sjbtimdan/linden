package org.sjbtimdan.linden.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.FxRatesRepository
import org.sjbtimdan.linden.data.RatesFlowProvider
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Currency

class AccountListViewModel(
    private val accountDao: AccountDao,
    entryDao: EntryDao,
    settingsDao: SettingsDao,
    fxRatesRepository: FxRatesRepository,
) : ViewModel() {
    private val ratesFlow = RatesFlowProvider(settingsDao, fxRatesRepository, viewModelScope)

    val accounts: StateFlow<List<AccountWithBalance>> = combine(
        accountDao.getAll(),
        entryDao.accountDeltas(),
    ) { accounts, deltas ->
        val balances = accountBalancesMinor(deltas, accounts)
        accounts.map { account -> AccountWithBalance(account, balances.getValue(account.id)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    val defaultCurrency: StateFlow<Currency> = ratesFlow.defaultCurrency

    /** Net total of all balances in the default currency; null when a rate is missing. */
    val totalMinor: StateFlow<Long?> = combine(
        accounts,
        ratesFlow.defaultCurrency,
        ratesFlow.rates,
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
