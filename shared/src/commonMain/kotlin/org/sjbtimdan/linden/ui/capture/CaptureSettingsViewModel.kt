package org.sjbtimdan.linden.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.capture.CaptureRepository
import org.sjbtimdan.linden.capture.CaptureRule
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.util.stateFlow

/**
 * Backs the payment-capture settings screen: the app allowlist with its
 * account mappings and the accounts to choose from.
 */
class CaptureSettingsViewModel(
    private val repository: CaptureRepository,
    accountDao: AccountDao,
) : ViewModel() {
    val rules: StateFlow<List<CaptureRule>> = repository.rulesFlow().stateFlow(viewModelScope, emptyList())

    val accounts: StateFlow<List<Account>> = accountDao.getAll().stateFlow(viewModelScope, emptyList())

    fun setRule(packageName: String, accountId: Long) {
        viewModelScope.launch { repository.setRule(CaptureRule(packageName, accountId)) }
    }

    fun setRuleEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch { repository.setRuleEnabled(packageName, enabled) }
    }

    fun removeRule(packageName: String) {
        viewModelScope.launch { repository.removeRule(packageName) }
    }
}
