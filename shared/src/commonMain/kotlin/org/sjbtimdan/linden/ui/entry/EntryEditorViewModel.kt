package org.sjbtimdan.linden.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.HideEntryTotalSetting
import org.sjbtimdan.linden.data.RatesFlowProvider
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.FxRate
import kotlin.time.Instant

/**
 * ViewModel base for screens that create or edit entries. Shares the in-progress
 * draft and its persistence, the account/category lookups, and the currency
 * state ([defaultCurrency], [rates], [hideTotal]) that entry screens convert
 * their totals into.
 */
abstract class EntryEditorViewModel(
    protected val entryDao: EntryDao,
    protected val accountDao: AccountDao,
    protected val categoryDao: CategoryDao,
    protected val settingsDao: SettingsDao,
    private val ratesProvider: RatesFlowProvider,
    initialHideTotal: Boolean = false,
) : ViewModel() {
    /** The currency totals are displayed in, from the settings. */
    val defaultCurrency: StateFlow<Currency> = ratesProvider.defaultCurrency

    /** FX rates that convert the default currency into each quote currency. */
    protected val rates: StateFlow<List<FxRate>> get() = ratesProvider.rates

    /** Every account, hidden or not — resolves names, currency and edited entries. */
    val accounts: StateFlow<List<Account>> = accountDao.getAll().stateFlow(emptyList())

    /** Accounts that are not hidden — the ones pickers, filters and balances may use. */
    val visibleAccounts: StateFlow<List<Account>> = accounts
        .map { list -> list.filterNot { it.hidden } }
        .stateFlow(emptyList())

    val categories: StateFlow<List<Category>> = categoryDao.getAll().stateFlow(emptyList())

    private val hideEntryTotalSetting = HideEntryTotalSetting(settingsDao, initialHideTotal, viewModelScope)

    /**
     * Whether the totals shown by the screen are masked. Mirrors the "Hide totals"
     * setting; seeded with [initialHideTotal] so a masked total never flashes at startup.
     */
    val hideTotal: StateFlow<Boolean> get() = hideEntryTotalSetting.state

    /** Persists the masked-total setting; the database flow propagates it back. */
    fun setHideTotal(hidden: Boolean) = hideEntryTotalSetting.set(hidden)

    /** In-progress entry being created or edited, or null when no editor is shown. */
    protected val draftState = MutableStateFlow<EntryDraft?>(null)
    val draft: StateFlow<EntryDraft?> = draftState.asStateFlow()

    fun onAmountChange(text: String) = draftState.update { it?.copy(amountText = text) }
    fun onCategoryChange(id: Long?) = draftState.update { it?.copy(categoryId = id) }
    fun onAccountChange(id: Long?) = draftState.update { it?.copy(accountId = id) }
    fun onToAccountChange(id: Long?) = draftState.update { it?.copy(toAccountId = id) }
    fun onToAmountChange(text: String) = draftState.update { it?.copy(toAmountText = text) }
    fun onDescriptionChange(text: String) = draftState.update { it?.copy(description = text) }
    fun onCreatedAtChange(instant: Instant) = draftState.update { it?.copy(createdAt = instant) }

    fun createEntry(entry: Entry) {
        viewModelScope.launch {
            entryDao.create(entry)
        }
    }

    /**
     * Creates a category and selects it in the draft; false when the name is
     * empty or already taken (case-insensitive). The draft is never cleared.
     */
    fun createCategory(name: String, type: CategoryType, icon: CategoryIcon? = null): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (categories.value.any { it.name.equals(trimmed, ignoreCase = true) }) return false
        viewModelScope.launch {
            categoryDao.create(trimmed, type, icon)
            val created = categoryDao.getAll().first { list -> list.any { it.name == trimmed } }
                .first { it.name == trimmed }
            draftState.update { it?.copy(categoryId = created.id) }
        }
        return true
    }

    /**
     * Creates an account and selects it in the draft; false when the name is
     * empty or already taken (case-insensitive). [selectAsTo] selects the new
     * account as the transfer destination instead of the source.
     */
    fun createAccount(
        name: String,
        currency: Currency,
        initialBalance: Long = 0,
        selectAsTo: Boolean = false,
    ): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (accounts.value.any { it.name.equals(trimmed, ignoreCase = true) }) return false
        viewModelScope.launch {
            accountDao.create(trimmed, currency, initialBalance)
            val created = accountDao.getAll().first { list -> list.any { it.name == trimmed } }
                .first { it.name == trimmed }
            draftState.update { state ->
                state?.let {
                    if (selectAsTo) it.copy(toAccountId = created.id) else it.copy(accountId = created.id)
                }
            }
        }
        return true
    }

    fun updateEntry(entry: Entry) {
        viewModelScope.launch {
            entryDao.update(entry)
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            entryDao.delete(id)
        }
    }

    /** Collects this flow eagerly into a [StateFlow] owned by the ViewModel scope. */
    protected fun <T> Flow<T>.stateFlow(initial: T): StateFlow<T> = stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = initial,
    )
}
