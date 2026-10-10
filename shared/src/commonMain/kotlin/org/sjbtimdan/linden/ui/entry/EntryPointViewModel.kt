package org.sjbtimdan.linden.ui.entry

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.RatesFlowProvider
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.predictions.QuickEntry
import org.sjbtimdan.linden.time.AppClock
import org.sjbtimdan.linden.ui.accounts.accountTotalMinor
import org.sjbtimdan.linden.ui.ledger.accountBalancesAtEnd

class EntryPointViewModel(
    entryDao: EntryDao,
    accountDao: AccountDao,
    categoryDao: CategoryDao,
    settingsDao: SettingsDao,
    ratesProvider: RatesFlowProvider,
    allEntries: StateFlow<List<Entry>>,
    initialHideEntryTotal: Boolean = false,
    private val clock: AppClock,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : EntryEditorViewModel(
    entryDao,
    accountDao,
    categoryDao,
    settingsDao,
    ratesProvider,
    initialHideTotal = initialHideEntryTotal,
) {
    private val suggestions = EntrySuggestionsProvider(
        entryDao,
        categoryDao,
        accountDao,
        draft,
        viewModelScope,
        clock = clock,
    )

    /**
     * Total across all visible accounts in the default currency: initial balances plus
     * the net of all entries dated on or before today (entries in the future
     * never count). Hidden accounts never count. Null while a foreign currency
     * has no stored rate.
     */
    val totalMinor: StateFlow<Long?> = combine(
        allEntries,
        visibleAccounts,
        defaultCurrency,
        rates,
    ) { entries, accounts, currency, rates ->
        accountTotalMinor(
            accountBalancesAtEnd(entries, clock.todayIn(zone), accounts, zone),
            currency,
            rates,
        )
    }.stateFlow(null)

    /** True once at least one entry exists; the hero card stays hidden until then. */
    val hasEntries: StateFlow<Boolean> = allEntries.map { it.isNotEmpty() }.stateFlow(false)

    /**
     * Whether the Entry tab may show the rates warning: the user either has entries
     * spanning 2+ currencies (so FX rates matter) or has opened the Rates screen.
     */
    val showRatesWarning: StateFlow<Boolean> = combine(
        allEntries.map { entries -> entries.flatMap { it.currencies() }.distinct().size > 1 },
        settingsDao.ratesSeenFlow(),
    ) { multiCurrency, ratesSeen -> multiCurrency || ratesSeen }.stateFlow(false)

    /** Most likely account ids for the current draft; only for new entries. */
    val accountSuggestions: StateFlow<List<Long>> get() = suggestions.accountSuggestions

    /** Most likely category ids for the current draft; only for new entries. */
    val categorySuggestions: StateFlow<List<Long>> get() = suggestions.categorySuggestions

    /** Most likely descriptions for the current draft; only for new entries. */
    val descriptionSuggestions: StateFlow<List<String>> get() = suggestions.descriptionSuggestions

    /** Whole entries the user is likely to repeat right now, time first with field matches boosted. */
    val quickEntries: StateFlow<List<QuickEntry>> get() = suggestions.quickEntries

    private val _lastAdded = MutableStateFlow<Entry?>(null)

    /**
     * The entry most recently saved from this screen, shown as a read-only
     * "already added" confirmation with a short-lived undo offer and an edit
     * action that loads it back into the form.
     */
    val lastAdded: StateFlow<Entry?> = _lastAdded.asStateFlow()

    private val _lastAddedWasEdit = MutableStateFlow(false)

    /** True when [lastAdded] came from an edit save, so the receipt reads "Updated". */
    val lastAddedWasEdit: StateFlow<Boolean> = _lastAddedWasEdit.asStateFlow()

    /**
     * Dismisses the last-added receipt without touching the draft or the entry;
     * the add becomes final. Also called when the undo offer times out.
     */
    fun dismissLastAdded() = clearLastAdded()

    /** Clears the receipt together with the edit flag that labels it. */
    private fun clearLastAdded() {
        _lastAdded.value = null
        _lastAddedWasEdit.value = false
    }

    /**
     * Fills the draft from a quick-entry chip, keeping the current date and time
     * and any amount already entered.
     */
    fun applyQuickEntry(quickEntry: QuickEntry) = draftState.update { state ->
        state?.repeatEntry(quickEntry.entry)
    }

    private val _selectedType = MutableStateFlow(EntryType.Expense)
    val selectedType: StateFlow<EntryType> = _selectedType.asStateFlow()

    /** Switches the entry type, carrying over the fields shared across types. */
    fun selectType(type: EntryType) {
        if (type == _selectedType.value) return
        _selectedType.value = type
        viewModelScope.launch {
            val previous = draftState.value
            draftState.value = when {
                previous == null || previous.type == type -> newEntryState(type)

                // A type flip on an edited entry still edits that row, so Add
                // updates it instead of quietly creating a duplicate.
                previous.editing != null -> previous.withType(type, categories.value)

                else -> newEntryState(type).carryOverCommonFields(previous)
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
    internal suspend fun newEntryState(type: EntryType): EntryDraft = EntryDraft.forNew(
        type,
        entryDao.latest(type),
        clock,
    )

    /** Resets the form to an empty draft of the selected type. */
    fun clearDraft() {
        clearLastAdded()
        draftState.value = EntryDraft.forNew(_selectedType.value, clock = clock)
    }

    /**
     * Saves the current draft and resets the form prefilled from the saved entry.
     * A draft carrying an edited entry updates that row; a new draft inserts one.
     * Drafts only resolve against visible accounts: an account hidden while a
     * draft referenced it simply cannot be saved.
     */
    suspend fun saveDraft(): Boolean {
        val state = draftState.value ?: return false
        val entry = state.toEntry(visibleAccounts.value, categories.value) ?: return false
        val updated = state.editing != null
        try {
            if (updated) {
                entryDao.update(entry)
                _lastAdded.value = entry
            } else {
                _lastAdded.value = entry.withId(entryDao.create(entry))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportError(e.message)
            return false
        }
        _lastAddedWasEdit.value = updated
        draftState.value = EntryDraft.forNew(entry.type, entry, clock)
        return true
    }

    /**
     * Pulls the last added entry out of the ledger and back into the form,
     * amount included, so an accidental or mistyped add can be redone. The form
     * then holds the entry as a fresh draft; nothing is written until Add again.
     */
    fun undoLastAdded() {
        val entry = _lastAdded.value ?: return
        deleteEntry(entry.id)
        _selectedType.value = entry.type
        draftState.value = EntryDraft.forEdit(entry).copy(editing = null)
        clearLastAdded()
    }

    /**
     * Loads the last added entry back into the form as an edit of that row, so
     * a typo can be fixed on the spot: the entry stays in the ledger and Add
     * updates it instead of creating a duplicate. Nothing is written until Add.
     */
    fun editLastAdded() {
        val entry = _lastAdded.value ?: return
        _selectedType.value = entry.type
        draftState.value = EntryDraft.forEdit(entry)
        clearLastAdded()
    }
}

/** Copy of an entry with its database-assigned [id], after an insert. */
private fun Entry.withId(id: Long): Entry = when (this) {
    is ExpenseEntry -> copy(id = id)
    is IncomeEntry -> copy(id = id)
    is TransferEntry -> copy(id = id)
}

/** Currencies an entry touches; transfers involve both accounts. */
private fun Entry.currencies(): List<Currency> = when (this) {
    is TransferEntry -> listOf(account.currency, toAccount.currency)
    else -> listOf(account.currency)
}
