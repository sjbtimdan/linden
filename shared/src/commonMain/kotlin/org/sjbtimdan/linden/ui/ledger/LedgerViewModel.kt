package org.sjbtimdan.linden.ui.ledger

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.BudgetDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.RatesFlowProvider
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.model.dayIn
import org.sjbtimdan.linden.time.AppClock
import org.sjbtimdan.linden.ui.accounts.AccountWithBalance
import org.sjbtimdan.linden.ui.accounts.accountBalancesMinor
import org.sjbtimdan.linden.ui.accounts.accountConversionsMinor
import org.sjbtimdan.linden.ui.accounts.accountTotalMinor
import org.sjbtimdan.linden.ui.accounts.adjustmentEntry
import org.sjbtimdan.linden.ui.accounts.balanceAdjustment
import org.sjbtimdan.linden.ui.accounts.entryDeltas
import org.sjbtimdan.linden.ui.budget.budgetsByCategoryName
import org.sjbtimdan.linden.ui.entry.EntryDraft
import org.sjbtimdan.linden.ui.entry.EntryEditorViewModel
import org.sjbtimdan.linden.ui.insights.PaceForecast
import kotlin.math.abs
import kotlin.time.Instant
import org.sjbtimdan.linden.ui.insights.paceForecast as computePaceForecast

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerViewModel(
    entryDao: EntryDao,
    accountDao: AccountDao,
    categoryDao: CategoryDao,
    settingsDao: SettingsDao,
    budgetDao: BudgetDao,
    ratesProvider: RatesFlowProvider,
    allEntries: StateFlow<List<Entry>>,
    private val clock: AppClock,
    val zone: TimeZone = TimeZone.currentSystemDefault(),
) : EntryEditorViewModel(
    entryDao,
    accountDao,
    categoryDao,
    settingsDao,
    ratesProvider,
) {
    /** Today in [zone], re-read on every call. */
    fun today(): LocalDate = clock.todayIn(zone)

    /** Stored FX rates, exposed for the entries list's per-day totals. */
    val fxRates: StateFlow<List<FxRate>> get() = rates

    private val filters = LedgerFilterState(viewModelScope, today())

    val searchQuery: StateFlow<String> get() = filters.searchQuery

    val typeFilter: StateFlow<EntryType?> get() = filters.typeFilter

    val amountFilter: StateFlow<AmountFilter?> get() = filters.amountFilter

    /** Show future-dated entries in every view mode. */
    val showFuture: StateFlow<Boolean> get() = filters.showFuture

    val periodSelection: StateFlow<PeriodSelection> get() = filters.periodSelection

    val viewMode: StateFlow<LedgerViewMode> get() = filters.viewMode

    /** Category filter id; 0 means "Uncategorized". */
    val categoryFilter: StateFlow<Long?> get() = filters.categoryFilter

    val accountFilter: StateFlow<Long?> get() = filters.accountFilter

    private val periodEntries: StateFlow<List<SearchableEntry>> = combine(
        filters.periodWindow,
        filters.showFuture,
    ) { window, showFuture -> window to showFuture }
        .flatMapLatest { (window, showFuture) ->
            // Only a safe lower bound is queried; the window rules are enforced here with a fresh "today".
            val source = window?.start?.let { entryDao.getSince(it.sqlLowerBound()) } ?: allEntries
            source.map { rows ->
                val now = today()
                rows
                    .filter { entry -> entry.isInWindow(window?.start, window?.end, now, showFuture, zone) }
                    .sortedWith(compareByDescending<Entry> { it.createdAt }.thenByDescending { it.id })
                    .map(::SearchableEntry)
            }
        }
        .stateFlow(emptyList())

    /**
     * Entries matching the search text and the amount filter, narrowed by the
     * given type filter. The categories view feeds [LedgerFilterState.typeFilterForCategories]
     * here because transfers carry no category and would only empty its totals.
     */
    private fun filteredEntries(typeFilter: StateFlow<EntryType?>): StateFlow<List<Entry>> = combine(
        periodEntries,
        filters.searchQuery,
        typeFilter,
        filters.amountFilter,
    ) { periodEntries, query, type, amountFilter ->
        val normalized = query.trim().lowercase()
        periodEntries.asSequence()
            .filter { searchable -> type == null || searchable.entry.type == type }
            .filter { searchable -> amountFilter == null || searchable.matchesAmount(amountFilter) }
            .filter { searchable -> normalized.isEmpty() || searchable.matches(normalized) }
            .map { it.entry }
            .toList()
    }.stateFlow(emptyList())

    val entries: StateFlow<List<Entry>> = filteredEntries(filters.typeFilter)

    /**
     * Same narrowing as [entries] but a Transfer type filter counts as no filter
     * at all, so switching to the categories view with one set never empties it.
     */
    val categoryEntries: StateFlow<List<Entry>> = filteredEntries(filters.typeFilterForCategories)

    /**
     * Entries shown by the entries view, narrowed to a category and/or account
     * while those filters are set. A category filter drops transfers — they never
     * contribute to category totals — while an account filter keeps transfers to
     * or from that account.
     */
    val displayedEntries: StateFlow<List<Entry>> = combine(
        entries,
        filters.categoryFilter,
        filters.accountFilter,
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
        filters.accountFilter,
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

    /** Current-month expense pace in the default currency; null until enough history. */
    val paceForecast: StateFlow<PaceForecast?> = combine(
        allEntries,
        defaultCurrency,
        rates,
    ) { entries, currency, rates ->
        computePaceForecast(entries, today(), currency, rates, zone)
    }.stateFlow(null)

    /** Whether any entry exists at all, regardless of period or filters — drives the
     * guided empty state for a new app vs the plain message for an empty match. */
    val hasAnyEntries: StateFlow<Boolean> = entryDao.entryExists().stateFlow(false)

    /** Last day of the selected period (inclusive); null for [LedgerPeriod.All]. */
    private val periodEnd: StateFlow<LocalDate?> = filters.periodWindow
        .map { it?.end }
        .stateFlow(null)

    /** Entries dated up to the period end, fetched with a safety margin. */
    private val entriesUpToPeriodEnd: StateFlow<List<Entry>> = periodEnd
        .flatMapLatest { end ->
            val source = end?.let { entryDao.getUpTo(it.sqlUpperBound()) } ?: allEntries
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
            val day = entry.dayIn(zone)
            day > now && (end == null || day <= end)
        }
    }.stateFlow(0)

    /**
     * Balance of each visible account at period end in its own currency: initial balance
     * plus entries through the period end, stopping at today unless the window spans
     * today and future entries are not shown. Hidden accounts never appear.
     */
    val accountBalancesAtPeriodEnd: StateFlow<List<AccountWithBalance>> = combine(
        entriesUpToPeriodEnd,
        filters.periodWindow,
        visibleAccounts,
        filters.showFuture,
    ) { entries, window, accounts, showFuture ->
        val now = today()
        // Entries dated after today count only when the window spans today and the
        // toggle is off; past and wholly-future windows count through their own end.
        val cutoff = if (showFuture) window?.end else hideFutureBound(window?.start, window?.end, now)
        accountBalancesAtEnd(entries, cutoff, accounts, zone)
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
     * Each period-end balance converted into the default currency, keyed by account id;
     * null for an account whose currency has no stored rate. Same-currency balances pass
     * through unchanged, and the conversions sum to [accountTotalAtPeriodEnd] exactly.
     */
    val accountConversionsAtPeriodEnd: StateFlow<Map<Long, Long?>> = combine(
        accountBalancesAtPeriodEnd,
        defaultCurrency,
        rates,
    ) { balances, currency, rates ->
        accountConversionsMinor(balances, currency, rates)
    }.stateFlow(emptyMap())

    /**
     * Current balance of each visible account in its own currency (minor units), used for balance
     * adjustments. Entries dated in the future never count, matching [accountBalancesAtPeriodEnd].
     * Reads the shared all-entries flow instead of a query bounded at construction, so entries
     * created after the ViewModel has been alive for a while still count.
     */
    val currentAccountBalances: StateFlow<Map<Long, Long>> = combine(
        visibleAccounts,
        allEntries,
    ) { accounts, entries ->
        val now = today()
        accountBalancesMinor(entryDeltas(entries.filter { it.dayIn(zone) <= now }), accounts)
    }.stateFlow(emptyMap())

    /** Net total per category in the default currency derived from the filtered entries. */
    val categoryTotals: StateFlow<List<CategoryWithTotal>> = combine(
        categoryEntries,
        defaultCurrency,
        rates,
        budgetDao.budgetsFlow(),
        filters.periodSelection,
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
        val budgetByName = budgetsByCategoryName(budgets)
        totals.map { total ->
            total.copy(budget = total.category?.let { budgetByName[it.name.lowercase()] })
        }
    }.stateFlow(emptyList())

    /** Net total of all categories in the default currency; null when a rate is missing. */
    val categoryTotal: StateFlow<Long?> = combine(
        categoryEntries,
        defaultCurrency,
        rates,
    ) { entries, currency, rates ->
        periodTotalMinor(entries, currency, rates)
    }.stateFlow(null)

    fun setSearchQuery(query: String) = filters.setSearchQuery(query)

    fun setTypeFilter(type: EntryType?) = filters.setTypeFilter(type)

    /** Sets the amount filter on the entries view; null clears it. */
    fun setAmountFilter(filter: AmountFilter?) = filters.setAmountFilter(filter)

    /** Removes the amount filter, restoring all amounts. */
    fun clearAmountFilter() = filters.clearAmountFilter()

    fun setShowFuture(show: Boolean) = filters.setShowFuture(show)

    fun setPeriod(period: LedgerPeriod) = filters.setPeriod(period)

    fun setViewMode(mode: LedgerViewMode) = filters.setViewMode(mode)

    /** Drills into a category from the categories view: narrows the entries to it and switches to the entries view. */
    fun openCategory(categoryId: Long) = filters.openCategory(categoryId)

    /** Drills into an account from the accounts view: narrows the entries to it and switches to the entries view. */
    fun openAccount(accountId: Long) = filters.openAccount(accountId)

    /** Backs out of a drill-down or entry filter: clears it and returns to the view it was opened from. */
    fun exitDrillDown() = filters.exitDrillDown()

    /** Narrows the entries view to a single category; null restores all entries. */
    fun setCategoryFilter(categoryId: Long?) = filters.setCategoryFilter(categoryId)

    /** Removes the category filter, restoring the full entries list. */
    fun clearCategoryFilter() = filters.clearCategoryFilter()

    /** Narrows the entries view to a single account; null restores all entries. */
    fun setAccountFilter(accountId: Long?) = filters.setAccountFilter(accountId)

    /** Removes the account filter, restoring the full entries list. */
    fun clearAccountFilter() = filters.clearAccountFilter()

    fun goToPreviousPeriod() = filters.goToPreviousPeriod()

    fun goToNextPeriod() = filters.goToNextPeriod()

    /** Re-anchors the shown period to the one containing today, keeping its granularity. */
    fun goToToday() = filters.goToToday(today())

    /**
     * Draft currently being edited in the entry dialog, or null when it is closed.
     * The draft outlives the screen with this ViewModel, so the host dismisses it
     * when the ledger stops being the shown screen (`App`), which makes leaving
     * the ledger final instead of the dialog reopening on the next visit.
     */
    val dialogState: StateFlow<EntryDraft?> get() = draft

    /** True while a dialog save, delete or balance adjustment is writing; guards against double-taps. */
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val dialog = LedgerEntryDialogController(
        entryDao = entryDao,
        draftState = draftState,
        accounts = accounts,
        categories = categories,
        savingState = _saving,
        clock = clock,
        zone = zone,
        write = { block -> launchWrite { block() } },
    )

    /** The entry most recently deleted from the edit dialog, held so the delete can be undone. */
    val lastDeleted: StateFlow<Entry?> get() = dialog.lastDeleted

    fun openEditDialog(entry: Entry) = dialog.open(entry)

    fun changeDialogType(type: EntryType) = dialog.changeType(type)

    fun duplicateDialogEntry(now: Instant = clock.now(), zone: TimeZone = this.zone) = dialog.duplicate(now, zone)

    fun saveDialog(): Boolean = dialog.save()

    fun deleteDialogEntry() = dialog.delete()

    fun undoLastDeleted() = dialog.undoLastDeleted()

    fun clearLastDeleted() = dialog.clearLastDeleted()

    fun dismissDialog() = dialog.dismiss()

    /**
     * Records a balance adjustment for [account] so its current balance reaches
     * [targetBalance] (in the account's own minor units), attaching [category] to
     * the resulting entry. Each adjustment creates a fresh income/expense entry
     * dated now; a positive delta becomes an income entry, a negative delta an
     * expense entry. When the delta is zero nothing is created. [onSaved] runs
     * only after the write succeeds, so the dialog can stay open on failure.
     */
    fun adjustBalance(
        account: Account,
        targetBalance: Long,
        category: Category,
        now: Instant = clock.now(),
        onSaved: () -> Unit = {},
    ) {
        if (_saving.value) return
        _saving.value = true
        launchWrite {
            try {
                val current = currentAccountBalances.value[account.id] ?: account.initialBalance
                val adjustment = balanceAdjustment(current, targetBalance)
                val entry = adjustmentEntry(adjustment, account, category, now, zone)
                if (entry != null) {
                    entryDao.create(entry)
                }
                onSaved()
            } finally {
                _saving.value = false
            }
        }
    }

    /** Categories used on entries in [accountId], most-used first. */
    suspend fun usedCategories(accountId: Long): List<Category> = entryDao.categoriesForAccount(accountId)
}
