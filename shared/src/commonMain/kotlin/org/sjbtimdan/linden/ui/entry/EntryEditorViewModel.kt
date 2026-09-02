package org.sjbtimdan.linden.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.FxRatesRepository
import org.sjbtimdan.linden.data.RatesFlowProvider
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
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
    accountDao: AccountDao,
    categoryDao: CategoryDao,
    private val settingsDao: SettingsDao,
    fxRatesRepository: FxRatesRepository,
    initialHideTotal: Boolean = false,
) : ViewModel() {
    private val ratesFlow = RatesFlowProvider(settingsDao, fxRatesRepository, viewModelScope)

    /** The currency totals are displayed in, from the settings. */
    val defaultCurrency: StateFlow<Currency> = ratesFlow.defaultCurrency

    /** FX rates that convert the default currency into each quote currency. */
    protected val rates: StateFlow<List<FxRate>> get() = ratesFlow.rates

    val accounts: StateFlow<List<Account>> = accountDao.getAll().stateFlow(emptyList())

    val categories: StateFlow<List<Category>> = categoryDao.getAll().stateFlow(emptyList())

    /**
     * Whether the totals shown by the screen are masked. Mirrors the "Hide totals"
     * setting via the database flow; seeded with [initialHideTotal] so a masked
     * total never flashes at startup.
     */
    val hideTotal: StateFlow<Boolean> = settingsDao.hideEntryTotalFlow().stateFlow(initialHideTotal)

    /** Persists the masked-total setting; the database flow propagates it back. */
    fun setHideTotal(hidden: Boolean) {
        viewModelScope.launch {
            settingsDao.setHideEntryTotal(hidden)
        }
    }

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
