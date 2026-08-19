package org.sjbtimdan.linden.ui.history

import kotlin.math.abs
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
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.FxRatesRepository
import org.sjbtimdan.linden.data.RatesFlowProvider
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.ui.accounts.AccountWithBalance
import org.sjbtimdan.linden.ui.accounts.accountTotalMinor
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
    private val ratesFlow = RatesFlowProvider(settingsDao, fxRatesRepository, viewModelScope)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow<EntryType?>(null)
    val typeFilter: StateFlow<EntryType?> = _typeFilter.asStateFlow()

    private val _period = MutableStateFlow(HistoryPeriod.All)
    val period: StateFlow<HistoryPeriod> = _period.asStateFlow()

    private val _periodAnchor = MutableStateFlow(today())
    val periodAnchor: StateFlow<LocalDate> = _periodAnchor.asStateFlow()

    private val _viewMode = MutableStateFlow(HistoryViewMode.Entries)
    val viewMode: StateFlow<HistoryViewMode> = _viewMode.asStateFlow()

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

    val defaultCurrency: StateFlow<Currency> = ratesFlow.defaultCurrency

    private val rates: StateFlow<List<FxRate>> get() = ratesFlow.rates

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

    /** Last day of the selected period (inclusive); null for [HistoryPeriod.All]. */
    private val periodEnd: StateFlow<LocalDate?> = combine(
        _period,
        _periodAnchor,
    ) { period, anchor -> period to anchor }
        .map { (period, anchor) -> period.windowEnd(anchor) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    /** All entries created at or before the end of the selected period (safety margin included). */
    private val entriesUpToPeriodEnd: StateFlow<List<Entry>> = periodEnd
        .flatMapLatest { end ->
            val source = end?.let { entryDao.getUpTo(it.sqlUpperBound()) } ?: entryDao.getAll()
            source
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    /**
     * Balance of each account at the end of the selected period in the account's own
     * currency: initial balance plus the net of entries dated on or before the last
     * day of the period (or today, whichever is earlier — entries in the future never
     * count). For [HistoryPeriod.All] this is the current balance.
     */
    val accountBalancesAtPeriodEnd: StateFlow<List<AccountWithBalance>> = combine(
        entriesUpToPeriodEnd,
        periodEnd,
        accounts,
    ) { entries, end, accounts ->
        val now = today()
        val cutoff = end?.let { minOf(it, now) } ?: now
        accountBalancesAtEnd(entries, cutoff, accounts)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    /** Net total of all period-end balances in the default currency; null when a rate is missing. */
    val accountTotalAtPeriodEnd: StateFlow<Long?> = combine(
        accountBalancesAtPeriodEnd,
        defaultCurrency,
        rates,
    ) { balances, currency, rates ->
        accountTotalMinor(balances, currency, rates)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    /** Net total per category in the default currency derived from the filtered entries. */
    val categoryTotals: StateFlow<List<CategoryWithTotal>> = combine(
        entries,
        defaultCurrency,
        rates,
    ) { entries, currency, rates ->
        entries
            .filter { it.type != EntryType.Transfer }
            .groupBy { it.category }
            .mapNotNull { (category, catEntries) ->
                val groups = catEntries.groupBy { it.account.currency }
                    .map { (c, e) -> c to e.sumOf { if (it.type == EntryType.Income) it.amount else -it.amount } }
                val total = sumInDefaultMinor(groups, currency, rates) ?: return@mapNotNull null
                CategoryWithTotal(category, total, catEntries.size)
            }
            .sortedByDescending { abs(it.total) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    /** Net total of all categories in the default currency; null when a rate is missing. */
    val categoryTotal: StateFlow<Long?> = combine(
        entries,
        defaultCurrency,
        rates,
    ) { entries, currency, rates ->
        periodTotalMinor(entries, currency, rates)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
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

    fun setViewMode(mode: HistoryViewMode) {
        _viewMode.value = mode
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

/**
 * Epoch-millis upper bound for a period query. Entries are re-filtered in memory
 * afterwards, so this only needs to be safe: entries in zones behind UTC can fall
 * up to ~14 hours after UTC midnight of the day after [this], hence the two-day margin.
 */
private fun LocalDate.sqlUpperBound(): Long =
    plus(2, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

/** What the history screen shows: the entry list, period-end account balances, or category totals. */
enum class HistoryViewMode {
    Entries,
    Accounts,
    Categories,
}

/** A category paired with its net total in the default currency (minor units) and entry count. */
data class CategoryWithTotal(
    val category: Category?,
    val total: Long,
    val count: Int,
)
