package org.sjbtimdan.linden.ui.entry

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.FxRatesRepository
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.predictions.QuickEntry
import org.sjbtimdan.linden.ui.accounts.accountTotalMinor
import org.sjbtimdan.linden.ui.ledger.accountBalancesAtEnd
import kotlin.time.Clock

class EntryPointViewModel(
    entryDao: EntryDao,
    accountDao: AccountDao,
    categoryDao: CategoryDao,
    settingsDao: SettingsDao,
    fxRatesRepository: FxRatesRepository,
    initialHideEntryTotal: Boolean = false,
    today: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) : EntryEditorViewModel(
    entryDao,
    accountDao,
    categoryDao,
    settingsDao,
    fxRatesRepository,
    initialHideTotal = initialHideEntryTotal,
) {
    private val suggestions = EntrySuggestionsProvider(entryDao, draft, viewModelScope)

    /**
     * Total across all visible accounts in the default currency: initial balances plus
     * the net of all entries dated on or before today (entries in the future
     * never count). Hidden accounts never count. Null while a foreign currency
     * has no stored rate.
     */
    val totalMinor: StateFlow<Long?> = combine(
        entryDao.getAll(),
        visibleAccounts,
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
        state?.let {
            EntryDraft.forEdit(quickEntry.entry).copy(
                editing = null,
                createdAt = it.createdAt,
                createdZone = it.createdZone,
            )
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

    /**
     * Saves the current draft and resets the form prefilled from the saved entry.
     * Drafts only resolve against visible accounts: an account hidden while a
     * draft referenced it simply cannot be saved.
     */
    fun saveDraft(): Boolean {
        val state = draftState.value ?: return false
        val entry = state.toEntry(visibleAccounts.value, categories.value) ?: return false
        createEntry(entry)
        draftState.value = EntryDraft.forNew(entry.type, entry)
        return true
    }
}
