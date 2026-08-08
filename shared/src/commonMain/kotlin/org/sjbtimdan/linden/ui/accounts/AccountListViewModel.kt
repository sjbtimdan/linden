package org.sjbtimdan.linden.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Currency

class AccountListViewModel(
    private val accountDao: AccountDao,
) : ViewModel() {
    val accounts: StateFlow<List<Account>> = accountDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    fun createAccount(name: String, currency: Currency) {
        viewModelScope.launch {
            accountDao.create(name, currency)
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            accountDao.update(account)
        }
    }
}
