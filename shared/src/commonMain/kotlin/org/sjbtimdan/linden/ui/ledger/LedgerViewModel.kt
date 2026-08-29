package org.sjbtimdan.linden.ui.ledger

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
import kotlin.math.abs
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerViewModel(
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

    private val _periodSelection = MutableStateFlow(PeriodSelection(LedgerPeriod.All, today()))
    val periodSelection: StateFlow<PeriodSelection> = _periodSelection.asStateFlow()

    /** Start/end of the selected period's window, derived once and reused by every period flow. */
    private val periodWindow: StateFlow<PeriodWindow?> = _periodSelection
        .map { selection ->
            val start = selection.period.windowStart(selection.anchor) ?: return@map null
            val end = selection.period.windowEnd(selection.anchor) ?: return@map null
            PeriodWindow(start, end)
        }
        .stateFlow(null)

    private val _viewMode = MutableStateFlow(LedgerViewMode.Entries)
    val viewMode: StateFlow<LedgerViewMode> = _viewMode.asStateFlow()

    /** Id of the category the entries view is narrowed to, or null for all entries. 0 means "Uncategorized". */
    private val _categoryFilter = MutableStateFlow<Long?>(null)
    val categoryFilter: StateFlow<Long?> = _categoryFilter.asStateFlow()

    /** Id of the account the entries view is narrowed to, or null for all accounts. */
    private val _accountFilter = MutableStateFlow<Long?>(null)
    val accountFilter: StateFlow<Long?> = _accountFilter.asStateFlow()

    private val periodEntries: StateFlow<List<SearchableEntry>> = periodWindow
        .flatMapLatest { window ->
            // Only rows at or after a safe lower bound are fetched from the database.
            // The exact window and the "nothing in the future" rule are still enforced
            // here at emission time so a stale "today" never hides new entries.
            val source = window?.start?.let { entryDao.getSince(it.sqlLowerBound()) } ?: entryDao.getAll()
            source.map { rows ->
                val now = today()
                rows
                    .filter { entry -> entry.isInWindow(window?.start, window?.end, now) }
                    .sortedWith(compareByDescending<Entry> { it.createdAt }.thenByDescending { it.id })
                    .map(::SearchableEntry)
            }
        }
        .stateFlow(emptyList())

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
    }.stateFlow(emptyList())

    val defaultCurrency: StateFlow<Currency> = ratesFlow.defaultCurrency

    private val rates: StateFlow<List<FxRate>> get() = ratesFlow.rates

    /**
     * Entries shown by the entries view: the period/search/type filtered entries,
     * narrowed to a single category and/or account while those filters are set.
     * Transfers never contribute to category totals, so they are excluded from a
     * category-filtered view to keep the list consistent with the category total
     * it drills into. The account filter keeps transfers of that account.
     */
    val displayedEntries: StateFlow<List<Entry>> = combine(
        entries,
        _categoryFilter,
        _accountFilter,
    ) { entries, categoryId, accountId ->
        if (categoryId == null && accountId == null) {
            entries
        } else {
            entries.filter { entry ->
                val matchesCategory = categoryId == null ||
                    (entry.type != EntryType.Transfer && (entry.category?.id ?: 0L) == categoryId)
                val matchesAccount = accountId == null || entry.account.id == accountId
                matchesCategory && matchesAccount
            }
        }
    }.stateFlow(emptyList())

    /** Net total of the displayed entries in the default currency; null when a rate is missing. */
    val totalMinor: StateFlow<Long?> = combine(
        displayedEntries,
        defaultCurrency,
        rates,
    ) { entries, currency, rates ->
        periodTotalMinor(entries, currency, rates)
    }.stateFlow(0L)

    /** Last day of the selected period (inclusive); null for [LedgerPeriod.All]. */
    private val periodEnd: StateFlow<LocalDate?> = periodWindow
        .map { it?.end }
        .stateFlow(null)

    /** All entries created at or before the end of the selected period (safety margin included). */
    private val entriesUpToPeriodEnd: StateFlow<List<Entry>> = periodEnd
        .flatMapLatest { end ->
            val source = end?.let { entryDao.getUpTo(it.sqlUpperBound()) } ?: entryDao.getAll()
            source
        }
        .stateFlow(emptyList())

    /**
     * Balance of each account at the end of the selected period in the account's own
     * currency: initial balance plus the net of entries dated on or before the last
     * day of the period (or today, whichever is earlier — entries in the future never
     * count). For [LedgerPeriod.All] this is the current balance.
     */
    val accountBalancesAtPeriodEnd: StateFlow<List<AccountWithBalance>> = combine(
        entriesUpToPeriodEnd,
        periodEnd,
        accounts,
    ) { entries, end, accounts ->
        val now = today()
        val cutoff = end?.let { minOf(it, now) } ?: now
        accountBalancesAtEnd(entries, cutoff, accounts)
    }.stateFlow(emptyList())

    /** Net total of all period-end balances in the default currency; null when a rate is missing. */
    val accountTotalAtPeriodEnd: StateFlow<Long?> = combine(
        accountBalancesAtPeriodEnd,
        defaultCurrency,
        rates,
    ) { balances, currency, rates ->
        accountTotalMinor(balances, currency, rates)
    }.stateFlow(null)

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
                val total = entriesNetInDefaultMinor(catEntries, currency, rates) ?: return@mapNotNull null
                CategoryWithTotal(category, total, catEntries.size)
            }
            .sortedByDescending { abs(it.total) }
    }.stateFlow(emptyList())

    /** Net total of all categories in the default currency; null when a rate is missing. */
    val categoryTotal: StateFlow<Long?> = combine(
        entries,
        defaultCurrency,
        rates,
    ) { entries, currency, rates ->
        periodTotalMinor(entries, currency, rates)
    }.stateFlow(null)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: EntryType?) {
        _typeFilter.value = type
    }

    fun setPeriod(period: LedgerPeriod) {
        _periodSelection.update { it.copy(period = period) }
    }

    fun setViewMode(mode: LedgerViewMode) {
        // The entry filters only apply to the entries view; leaving it clears
        // them so no stale chip can sit over the categories or accounts list.
        if (mode != LedgerViewMode.Entries) {
            _categoryFilter.value = null
            _accountFilter.value = null
        }
        _viewMode.value = mode
    }

    /** Drills into a category from the categories view: narrows the entries to it and switches to the entries view. */
    fun openCategory(categoryId: Long) {
        setCategoryFilter(categoryId)
        _viewMode.value = LedgerViewMode.Entries
    }

    /** Narrows the entries view to a single category; null restores all entries. */
    fun setCategoryFilter(categoryId: Long?) {
        _categoryFilter.value = categoryId
    }

    /** Removes the category filter, restoring the full entries list. */
    fun clearCategoryFilter() {
        _categoryFilter.value = null
    }

    /** Narrows the entries view to a single account; null restores all entries. */
    fun setAccountFilter(accountId: Long?) {
        _accountFilter.value = accountId
    }

    /** Removes the account filter, restoring the full entries list. */
    fun clearAccountFilter() {
        _accountFilter.value = null
    }

    fun goToPreviousPeriod() {
        _periodSelection.update { it.copy(anchor = it.period.previousAnchor(it.anchor)) }
    }

    fun goToNextPeriod() {
        _periodSelection.update { it.copy(anchor = it.period.nextAnchor(it.anchor)) }
    }

    /** Draft currently being edited in the entry dialog, or null when it is closed. */
    val dialogState: StateFlow<EntryDraft?> get() = draft

    fun openEditDialog(entry: Entry) {
        draftState.value = EntryDraft.forEdit(entry)
    }

    /** Saves the dialog draft and closes the dialog. */
    fun saveDialog(): Boolean {
        val state = draftState.value ?: return false
        val entry = state.toEntry(accounts.value, categories.value) ?: return false
        updateEntry(entry)
        draftState.value = null
        return true
    }

    /** Deletes the entry being edited and closes the dialog. */
    fun deleteDialogEntry() {
        draftState.value?.editing?.let { deleteEntry(it.id) }
        draftState.value = null
    }

    fun dismissDialog() {
        draftState.value = null
    }

    /** Collects this flow eagerly into a [StateFlow] owned by the ViewModel scope. */
    private fun <T> Flow<T>.stateFlow(initial: T): StateFlow<T> = stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = initial,
    )
}

/** An entry with its searchable fields pre-lowercased, so filtering on a keystroke does not re-lowercase every field. */
private class SearchableEntry(val entry: Entry) {
    private val description = entry.description?.lowercase()
    private val categoryName = entry.category?.name?.lowercase()
    private val accountName = entry.account.name.lowercase()
    private val toAccountName = (entry as? TransferEntry)?.toAccount?.name?.lowercase()

    fun matches(query: String): Boolean = description?.contains(query) == true ||
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

/** What the ledger screen shows: the entry list, period-end account balances, or category totals. */
enum class LedgerViewMode {
    Entries,
    Accounts,
    Categories,
}

/** The selected [LedgerPeriod] together with the anchor date its window is centered on. */
data class PeriodSelection(
    val period: LedgerPeriod,
    val anchor: LocalDate,
)

/** First and last day of the calendar window of a period selection; null for [LedgerPeriod.All]. */
private data class PeriodWindow(
    val start: LocalDate,
    val end: LocalDate,
)

/** A category paired with its net total in the default currency (minor units) and entry count. */
data class CategoryWithTotal(
    val category: Category?,
    val total: Long,
    val count: Int,
)
