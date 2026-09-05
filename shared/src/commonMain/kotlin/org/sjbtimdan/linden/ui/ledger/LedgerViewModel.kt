package org.sjbtimdan.linden.ui.ledger

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.BudgetDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.FxRatesRepository
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.ui.accounts.AccountWithBalance
import org.sjbtimdan.linden.ui.accounts.accountBalancesMinor
import org.sjbtimdan.linden.ui.accounts.accountTotalMinor
import org.sjbtimdan.linden.ui.accounts.adjustmentEntry
import org.sjbtimdan.linden.ui.accounts.balanceAdjustment
import org.sjbtimdan.linden.ui.budget.computeCategoryBudgets
import org.sjbtimdan.linden.ui.entry.EntryDraft
import org.sjbtimdan.linden.ui.entry.EntryEditorViewModel
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerViewModel(
    entryDao: EntryDao,
    accountDao: AccountDao,
    categoryDao: CategoryDao,
    settingsDao: SettingsDao,
    fxRatesRepository: FxRatesRepository,
    budgetDao: BudgetDao,
    val today: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) : EntryEditorViewModel(entryDao, accountDao, categoryDao, settingsDao, fxRatesRepository) {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow<EntryType?>(null)
    val typeFilter: StateFlow<EntryType?> = _typeFilter.asStateFlow()

    private val _amountFilter = MutableStateFlow<AmountFilter?>(null)
    val amountFilter: StateFlow<AmountFilter?> = _amountFilter.asStateFlow()

    /** Show future-dated entries in every view mode. */
    private val _showFuture = MutableStateFlow(false)
    val showFuture: StateFlow<Boolean> = _showFuture.asStateFlow()

    private val _periodSelection = MutableStateFlow(PeriodSelection(LedgerPeriod.Month, today()))
    val periodSelection: StateFlow<PeriodSelection> = _periodSelection.asStateFlow()

    /** Selected period's start/end, derived once and shared by every period flow. */
    private val periodWindow: StateFlow<PeriodWindow?> = _periodSelection
        .map { selection ->
            val start = selection.period.windowStart(selection.anchor) ?: return@map null
            val end = selection.period.windowEnd(selection.anchor) ?: return@map null
            PeriodWindow(start, end)
        }
        .stateFlow(null)

    private val _viewMode = MutableStateFlow(LedgerViewMode.Entries)
    val viewMode: StateFlow<LedgerViewMode> = _viewMode.asStateFlow()

    /** Category filter id; 0 means "Uncategorized". */
    private val _categoryFilter = MutableStateFlow<Long?>(null)
    val categoryFilter: StateFlow<Long?> = _categoryFilter.asStateFlow()

    private val _accountFilter = MutableStateFlow<Long?>(null)
    val accountFilter: StateFlow<Long?> = _accountFilter.asStateFlow()

    private val periodEntries: StateFlow<List<SearchableEntry>> = combine(
        periodWindow,
        _showFuture,
    ) { window, showFuture -> window to showFuture }
        .flatMapLatest { (window, showFuture) ->
            // Only a safe lower bound is queried; the window rules are enforced here with a fresh "today".
            val source = window?.start?.let { entryDao.getSince(it.sqlLowerBound()) } ?: entryDao.getAll()
            source.map { rows ->
                val now = today()
                rows
                    .filter { entry -> entry.isInWindow(window?.start, window?.end, now, showFuture) }
                    .sortedWith(compareByDescending<Entry> { it.createdAt }.thenByDescending { it.id })
                    .map(::SearchableEntry)
            }
        }
        .stateFlow(emptyList())

    val entries: StateFlow<List<Entry>> = combine(
        periodEntries,
        _searchQuery,
        _typeFilter,
        _amountFilter,
    ) { periodEntries, query, type, amountFilter ->
        val normalized = query.trim().lowercase()
        periodEntries.asSequence()
            .filter { searchable -> type == null || searchable.entry.type == type }
            .filter { searchable -> amountFilter == null || searchable.matchesAmount(amountFilter) }
            .filter { searchable -> normalized.isEmpty() || searchable.matches(normalized) }
            .map { it.entry }
            .toList()
    }.stateFlow(emptyList())

    /**
     * Month-to-date window and the same day-range of the previous month; null
     * unless the period is [LedgerPeriod.Month] with at least one elapsed day.
     */
    private val insightWindows: StateFlow<Pair<InsightWindow, InsightWindow>?> = _periodSelection
        .map { selection ->
            if (selection.period != LedgerPeriod.Month) return@map null
            monthInsightWindows(selection.anchor, today())
        }
        .stateFlow(null)

    /** Entries of both insight windows, fetched with one query from the earlier start. */
    private val insightEntries: StateFlow<Pair<List<Entry>, List<Entry>>> = insightWindows
        .flatMapLatest { windows ->
            if (windows == null) return@flatMapLatest flowOf(emptyList<Entry>() to emptyList())
            val (current, previous) = windows
            val source = entryDao.getSince(minOf(current.start, previous.start).sqlLowerBound())
            source.map { rows ->
                val now = today()
                rows.inInsightWindow(current, now) to rows.inInsightWindow(previous, now)
            }
        }
        .stateFlow(emptyList<Entry>() to emptyList())

    /**
     * Spending insights for the selected month: month-to-date expenses vs the same
     * day-range of last month, plus the top expense categories. Null outside a month.
     */
    val spendingInsights: StateFlow<SpendingInsights?> = combine(
        insightEntries,
        defaultCurrency,
        rates,
    ) { (current, previous), currency, rates ->
        computeSpendingInsights(current, previous, currency, rates)
            ?.takeIf { it.currentSpent != 0L || it.previousSpent != 0L }
    }.stateFlow(null)

    /**
     * Entries shown by the entries view, narrowed to a category and/or account
     * while those filters are set. A category filter drops transfers — they never
     * contribute to category totals — while an account filter keeps transfers to
     * or from that account.
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
                val matchesAccount = accountId == null ||
                    entry.account.id == accountId ||
                    (entry is TransferEntry && entry.toAccount.id == accountId)
                matchesCategory && matchesAccount
            }
        }
    }.stateFlow(emptyList())

    /**
     * Net total of the displayed entries in the default currency; null when a rate
     * is missing. Transfers cancel out unless an account filter counts them toward
     * that account's balance.
     */
    val totalMinor: StateFlow<Long?> = combine(
        displayedEntries,
        _accountFilter,
        accounts,
        defaultCurrency,
        rates,
    ) { entries, accountId, accounts, currency, rates ->
        if (accountId == null) {
            periodTotalMinor(entries, currency, rates)
        } else {
            val account = accounts.firstOrNull { it.id == accountId } ?: return@combine 0L
            accountNetInDefaultMinor(account, entries, currency, rates)
        }
    }.stateFlow(0L)

    /** Whether any entry exists at all, regardless of period or filters — drives the
     * guided empty state for a new app vs the plain message for an empty match. */
    val hasAnyEntries: StateFlow<Boolean> = entryDao.getAll().map { it.isNotEmpty() }.stateFlow(false)

    /** Last day of the selected period (inclusive); null for [LedgerPeriod.All]. */
    private val periodEnd: StateFlow<LocalDate?> = periodWindow
        .map { it?.end }
        .stateFlow(null)

    /** Entries dated up to the period end, fetched with a safety margin. */
    private val entriesUpToPeriodEnd: StateFlow<List<Entry>> = periodEnd
        .flatMapLatest { end ->
            val source = end?.let { entryDao.getUpTo(it.sqlUpperBound()) } ?: entryDao.getAll()
            source
        }
        .stateFlow(emptyList())

    /**
     * Entries dated after today inside the selected window — the rows the
     * show-future toggle would reveal. A null window ([LedgerPeriod.All])
     * counts every future-dated entry; rows beyond the window end never count,
     * matching [periodEntries].
     */
    val upcomingCount: StateFlow<Int> = combine(
        entriesUpToPeriodEnd,
        periodEnd,
    ) { rows, end ->
        val now = today()
        rows.count { entry ->
            val day = entry.dayInZone()
            day > now && (end == null || day <= end)
        }
    }.stateFlow(0)

    /**
     * Balance of each account at period end in its own currency: initial balance
     * plus entries through the period end, stopping at today unless future entries
     * are shown.
     */
    val accountBalancesAtPeriodEnd: StateFlow<List<AccountWithBalance>> = combine(
        entriesUpToPeriodEnd,
        periodEnd,
        accounts,
        _showFuture,
    ) { entries, end, accounts, showFuture ->
        val now = today()
        // Entries dated after today count only when future entries are shown.
        val cutoff = when {
            showFuture -> end
            else -> end?.let { minOf(it, now) } ?: now
        }
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

    /**
     * Current balance of each account in its own currency (minor units), used for balance
     * adjustments. Entries dated in the future never count, matching [accountBalancesAtPeriodEnd].
     */
    val currentAccountBalances: StateFlow<Map<Long, Long>> = combine(
        accounts,
        entryDao.accountDeltasUpTo(today().sqlUpperBound()),
    ) { accounts, deltas ->
        accountBalancesMinor(deltas, accounts)
    }.stateFlow(emptyMap())

    /** Net total per category in the default currency derived from the filtered entries. */
    val categoryTotals: StateFlow<List<CategoryWithTotal>> = combine(
        entries,
        defaultCurrency,
        rates,
        budgetDao.budgetsFlow(),
        _periodSelection,
    ) { entries, currency, rates, budgets, selection ->
        val totals = entries
            .filter { it.type != EntryType.Transfer }
            .groupBy { it.category }
            .mapNotNull { (category, catEntries) ->
                val total = entriesNetInDefaultMinor(catEntries, currency, rates) ?: return@mapNotNull null
                CategoryWithTotal(category, total, catEntries.size)
            }
            .sortedByDescending { abs(it.total) }
        // Budgets are monthly limits, so they only make sense against a month of spending.
        if (selection.period != LedgerPeriod.Month) return@combine totals
        val budgetByName = computeCategoryBudgets(totals, budgets)
            .mapNotNull { it.category?.let { category -> category.name.lowercase() to it.limit } }
            .toMap()
        totals.map { total ->
            total.copy(budget = total.category?.let { budgetByName[it.name.lowercase()] })
        }
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

    /** Sets the amount filter on the entries view; null clears it. */
    fun setAmountFilter(filter: AmountFilter?) {
        _amountFilter.value = filter
    }

    /** Removes the amount filter, restoring all amounts. */
    fun clearAmountFilter() {
        _amountFilter.value = null
    }

    fun setShowFuture(show: Boolean) {
        _showFuture.value = show
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

    /** Drills into an account from the accounts view: narrows the entries to it and switches to the entries view. */
    fun openAccount(accountId: Long) {
        setAccountFilter(accountId)
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

    /**
     * Records a balance adjustment for [account] so its current balance reaches
     * [targetBalance] (in the account's own minor units), attaching [category] to
     * the resulting entry. Each adjustment creates a fresh income/expense entry
     * dated now; a positive delta becomes an income entry, a negative delta an
     * expense entry. When the delta is zero nothing is created.
     */
    fun adjustBalance(account: Account, targetBalance: Long, category: Category, now: Instant = Clock.System.now()) {
        viewModelScope.launch {
            val zone = TimeZone.currentSystemDefault()
            val current = currentAccountBalances.value[account.id] ?: account.initialBalance
            val adjustment = balanceAdjustment(current, targetBalance)
            val entry = adjustmentEntry(adjustment, account, category, now, zone)
                ?: return@launch
            entryDao.create(entry)
        }
    }

    /** Categories used on entries in [accountId], most-used first. */
    suspend fun usedCategories(accountId: Long): List<Category> = entryDao.categoriesForAccount(accountId)
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

    /** Whether the entry's amount (or, for transfers, its to-amount) satisfies [filter]. */
    fun matchesAmount(filter: AmountFilter): Boolean =
        filter.matches(entry.amount) || (entry as? TransferEntry)?.toAmount?.let(filter::matches) == true
}

/** How an amount filter compares an entry's amount against a target value. */
enum class AmountOperator {
    GreaterThan,
    LessThan,
    Approximately,
}

/** A filter on entry amounts: [operator] applied to [minor] (minor units). */
data class AmountFilter(
    val operator: AmountOperator,
    val minor: Long,
) {
    fun matches(amount: Long): Boolean = when (operator) {
        AmountOperator.GreaterThan -> amount > minor
        AmountOperator.LessThan -> amount < minor
        AmountOperator.Approximately -> abs(amount - minor) <= (minor * 5 + 50) / 100
    }
}

/** The calendar day an entry is dated, in the zone it was created in. */
private fun Entry.dayInZone(): LocalDate = createdAt.toLocalDateTime(createdZone).date

private fun Entry.isInWindow(start: LocalDate?, end: LocalDate?, today: LocalDate, showFuture: Boolean): Boolean {
    val date = dayInZone()
    return (showFuture || date <= today) &&
        (start == null || date >= start) &&
        (end == null || date <= end)
}

/** Rows that fall inside [window]; insight comparisons never count future entries. */
private fun List<Entry>.inInsightWindow(window: InsightWindow, today: LocalDate): List<Entry> =
    filter { entry -> entry.isInWindow(window.start, window.end, today, showFuture = false) }

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
    /** Monthly budget limit in the default currency's minor units; null when none is set or the period is not a month. */
    val budget: Long? = null,
)
