package org.sjbtimdan.linden.ui.history

import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.FxRatesRepository
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.ui.entry.EntryDraft
import org.sjbtimdan.linden.ui.entry.EntryEditorViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    entryDao: EntryDao,
    accountDao: AccountDao,
    categoryDao: CategoryDao,
    settingsDao: SettingsDao,
    fxRatesRepository: FxRatesRepository,
    today: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) : EntryEditorViewModel(entryDao, accountDao, categoryDao) {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow<EntryType?>(null)
    val typeFilter: StateFlow<EntryType?> = _typeFilter.asStateFlow()

    private val _period = MutableStateFlow(HistoryPeriod.All)
    val period: StateFlow<HistoryPeriod> = _period.asStateFlow()

    private val _periodAnchor = MutableStateFlow(today())
    val periodAnchor: StateFlow<LocalDate> = _periodAnchor.asStateFlow()

    private val periodEntries: StateFlow<List<SearchableEntry>> = combine(
        _period,
        _periodAnchor,
    ) { period, anchor -> period to anchor }
        .flatMapLatest { (period, anchor) ->
            val start = period.windowStart(anchor)
            val end = period.windowEnd(anchor)
            // Only rows at or after a safe lower bound are fetched from the database.
            // The exact window and the "nothing in the future" rule are still enforced
            // here at emission time so a stale "today" never hides new entries.
            val source = start?.let { entryDao.getSince(it.sqlLowerBound()) } ?: entryDao.getAll()
            source.map { rows ->
                val now = today()
                rows
                    .filter { entry -> entry.isInWindow(start, end, now) }
                    .sortedWith(compareByDescending<Entry> { it.createdAt }.thenByDescending { it.id })
                    .map(::SearchableEntry)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    val entries: StateFlow<List<Entry>> = combine(
        periodEntries,
        _searchQuery,
        _typeFilter,
    ) { periodEntries, query, type ->
        val normalized = query.trim().lowercase()
        periodEntries.asSequence()
            .filter { searchable -> type == null || searchable.entry.type == type }
            .filter { searchable -> normalized.isEmpty() || searchable.matches(normalized) }
            .map { it.entry }
            .toList()
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
        .flatMapLatest { currency -> fxRatesRepository.ratesFor(currency) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    /** Net total of the displayed entries in the default currency; null when a rate is missing. */
    val totalMinor: StateFlow<Long?> = combine(
        entries,
        defaultCurrency,
        rates,
    ) { entries, currency, rates ->
        periodTotalMinor(entries, currency, rates)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0L,
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: EntryType?) {
        _typeFilter.value = type
    }

    fun setPeriod(period: HistoryPeriod) {
        _period.value = period
    }

    fun goToPreviousPeriod() {
        _periodAnchor.value = _period.value.previousAnchor(_periodAnchor.value)
    }

    fun goToNextPeriod() {
        _periodAnchor.value = _period.value.nextAnchor(_periodAnchor.value)
    }

    /** Draft currently being edited in the entry dialog, or null when it is closed. */
    val dialogState: StateFlow<EntryDraft?> get() = draft

    fun openEditDialog(entry: Entry) {
        _draft.value = EntryDraft.forEdit(entry)
    }

    /** Saves the dialog draft and closes the dialog. */
    fun saveDialog(): Boolean {
        val state = _draft.value ?: return false
        val entry = state.toEntry(accounts.value, categories.value) ?: return false
        updateEntry(entry)
        _draft.value = null
        return true
    }

    /** Deletes the entry being edited and closes the dialog. */
    fun deleteDialogEntry() {
        _draft.value?.editing?.let { deleteEntry(it.id) }
        _draft.value = null
    }

    fun dismissDialog() {
        _draft.value = null
    }
}

/** An entry with its searchable fields pre-lowercased, so filtering on a keystroke does not re-lowercase every field. */
private class SearchableEntry(val entry: Entry) {
    private val description = entry.description?.lowercase()
    private val categoryName = entry.category?.name?.lowercase()
    private val accountName = entry.account.name.lowercase()
    private val toAccountName = (entry as? TransferEntry)?.toAccount?.name?.lowercase()

    fun matches(query: String): Boolean =
        description?.contains(query) == true ||
            categoryName?.contains(query) == true ||
            accountName.contains(query) ||
            toAccountName?.contains(query) == true
}

private fun Entry.isInWindow(start: LocalDate?, end: LocalDate?, today: LocalDate): Boolean {
    val date = createdAt.toLocalDateTime(createdZone).date
    return date <= today &&
        (start == null || date >= start) &&
        (end == null || date <= end)
}

/**
 * Epoch-millis lower bound for a period query. Entries are re-filtered in memory
 * afterwards, so this only needs to be safe: entries in zones ahead of UTC can
 * fall up to 14 hours before UTC midnight of [this], hence the one-day margin.
 */
private fun LocalDate.sqlLowerBound(): Long =
    minus(1, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
