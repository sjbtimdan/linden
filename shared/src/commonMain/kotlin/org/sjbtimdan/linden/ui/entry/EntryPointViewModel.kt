package org.sjbtimdan.linden.ui.entry

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.FxRatesRepository
import org.sjbtimdan.linden.data.RatesFlowProvider
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.predictions.QuickEntry
import org.sjbtimdan.linden.ui.accounts.accountTotalMinor
import org.sjbtimdan.linden.ui.ledger.accountBalancesAtEnd
import kotlin.time.Clock

class EntryPointViewModel(
    entryDao: EntryDao,
    accountDao: AccountDao,
    categoryDao: CategoryDao,
    private val settingsDao: SettingsDao,
    fxRatesRepository: FxRatesRepository,
    initialHideEntryTotal: Boolean = false,
    today: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) : EntryEditorViewModel(entryDao, accountDao, categoryDao) {
    private val suggestions = EntrySuggestionsProvider(entryDao, draft, viewModelScope)
    private val ratesFlow = RatesFlowProvider(settingsDao, fxRatesRepository, viewModelScope)

    val defaultCurrency: StateFlow<Currency> = ratesFlow.defaultCurrency

    private val rates: StateFlow<List<FxRate>> get() = ratesFlow.rates

    /**
     * Whether the hero card masks the total across all accounts. Seeded from the
     * stored setting (never flashes the amount at startup) and kept in sync with
     * the settings screen via the database flow.
     */
    val hideEntryTotal: StateFlow<Boolean> = settingsDao.hideEntryTotalFlow()
        .stateFlow(initialHideEntryTotal)

    /** Persists the hero-card visibility; the database flow propagates it back. */
    fun setHideEntryTotal(hidden: Boolean) {
        viewModelScope.launch {
            settingsDao.setHideEntryTotal(hidden)
        }
    }

    /**
     * Total across all accounts in the default currency: initial balances plus
     * the net of all entries dated on or before today (entries in the future
     * never count). Null while a foreign currency has no stored rate.
     */
    val totalMinor: StateFlow<Long?> = combine(
        entryDao.getAll(),
        accounts,
        defaultCurrency,
        rates,
    ) { entries, accounts, currency, rates ->
        accountTotalMinor(accountBalancesAtEnd(entries, today(), accounts), currency, rates)
    }.stateFlow(null)

    /** Most likely account ids for the current draft; only for new entries. */
    val accountSuggestions: StateFlow<List<Long>> get() = suggestions.accountSuggestions

    /** Most likely category ids for the current draft; only for new entries. */
    val categorySuggestions: StateFlow<List<Long>> get() = suggestions.categorySuggestions

    /** Most likely descriptions for the current draft; only for new entries. */
    val descriptionSuggestions: StateFlow<List<String>> get() = suggestions.descriptionSuggestions

    /** Whole entries the user is likely to repeat right now, ranked time first. */
    val quickEntries: StateFlow<List<QuickEntry>> get() = suggestions.quickEntries

    /** Fills the draft from a quick-entry chip, keeping the current date and time. */
    fun applyQuickEntry(quickEntry: QuickEntry) = draftState.update { state ->
        if (state == null) {
            null
        } else {
            when (val entry = quickEntry.entry) {
                is TransferEntry -> state.copy(
                    type = EntryType.Transfer,
                    amountText = formatAmount(entry.amount),
                    categoryId = entry.category?.id,
                    accountId = entry.account.id,
                    toAccountId = entry.toAccount.id,
                    toAmountText = formatAmount(entry.toAmount ?: entry.amount),
                    description = entry.description.orEmpty(),
                )

                is ExpenseEntry -> state.copy(
                    type = EntryType.Expense,
                    amountText = formatAmount(entry.amount),
                    categoryId = entry.category.id,
                    accountId = entry.account.id,
                    toAccountId = null,
                    toAmountText = "",
                    description = entry.description.orEmpty(),
                )

                is IncomeEntry -> state.copy(
                    type = EntryType.Income,
                    amountText = formatAmount(entry.amount),
                    categoryId = entry.category.id,
                    accountId = entry.account.id,
                    toAccountId = null,
                    toAmountText = "",
                    description = entry.description.orEmpty(),
                )
            }
        }
    }

    private val _selectedType = MutableStateFlow(EntryType.Expense)
    val selectedType: StateFlow<EntryType> = _selectedType.asStateFlow()

    /** Switches the entry type, carrying over the fields shared across types. */
    fun selectType(type: EntryType) {
        if (type == _selectedType.value) return
        _selectedType.value = type
        viewModelScope.launch {
            val previous = draftState.value
            val fresh = newEntryState(type)
            draftState.value = if (previous != null && previous.type != type) {
                fresh.carryOverCommonFields(previous)
            } else {
                fresh
            }
        }
    }

    /** Seeds the draft from the latest entry of the selected type, unless one already exists. */
    fun seedDraft() {
        viewModelScope.launch {
            if (draftState.value == null) {
                draftState.value = newEntryState(_selectedType.value)
            }
        }
    }

    /** A new draft of [type] prefilled from the latest entry of that type. */
    internal suspend fun newEntryState(type: EntryType): EntryDraft = EntryDraft.forNew(type, entryDao.latest(type))

    /** Resets the form to an empty draft of the selected type. */
    fun clearDraft() {
        draftState.value = EntryDraft.forNew(_selectedType.value)
    }

    /** Saves the current draft and resets the form prefilled from the saved entry. */
    fun saveDraft(): Boolean {
        val state = draftState.value ?: return false
        val entry = state.toEntry(accounts.value, categories.value) ?: return false
        createEntry(entry)
        draftState.value = EntryDraft.forNew(entry.type, entry)
        return true
    }

    /** Collects this flow eagerly into a [StateFlow] owned by the ViewModel scope. */
    private fun <T> Flow<T>.stateFlow(initial: T): StateFlow<T> = stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = initial,
    )
}
