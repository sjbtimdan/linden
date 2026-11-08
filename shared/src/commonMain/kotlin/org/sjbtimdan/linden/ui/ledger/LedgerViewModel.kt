package org.sjbtimdan.linden.ui.ledger

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
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
import org.sjbtimdan.linden.time.AppClock
import org.sjbtimdan.linden.ui.accounts.AccountWithBalance
import org.sjbtimdan.linden.ui.accounts.adjustmentEntry
import org.sjbtimdan.linden.ui.accounts.balanceAdjustment
import org.sjbtimdan.linden.ui.entry.EntryDraft
import org.sjbtimdan.linden.ui.entry.EntryEditorViewModel
import org.sjbtimdan.linden.ui.insights.PaceForecast
import kotlin.time.Instant

class LedgerViewModel(
    entryDao: EntryDao,
    accountDao: AccountDao,
    categoryDao: CategoryDao,
    settingsDao: SettingsDao,
    budgetDao: BudgetDao,
    ratesProvider: RatesFlowProvider,
    allEntries: StateFlow<List<Entry>>,
    private val clock: AppClock,
    searchDebounceMillis: Long = SEARCH_DEBOUNCE_MILLIS,
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

    private val filters = LedgerFilterState(viewModelScope, today(), searchDebounceMillis)

    /** Derived rows, totals and balances; see [LedgerAggregates]. */
    private val aggregates = LedgerAggregates(
        entryDao = entryDao,
        budgetDao = budgetDao,
        allEntries = allEntries,
        filters = filters,
        accounts = accounts,
        visibleAccounts = visibleAccounts,
        defaultCurrency = defaultCurrency,
        rates = rates,
        clock = clock,
        zone = zone,
        scope = viewModelScope,
    )

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

    val entries: StateFlow<List<Entry>> get() = aggregates.entries

    val categoryEntries: StateFlow<List<Entry>> get() = aggregates.categoryEntries

    val displayedEntries: StateFlow<List<Entry>> get() = aggregates.displayedEntries

    /** Net total of the displayed entries in the default currency; null when a rate is missing. */
    val totalMinor: StateFlow<Long?> get() = aggregates.totalMinor

    /** Current-month expense pace in the default currency; null until enough history. */
    val paceForecast: StateFlow<PaceForecast?> get() = aggregates.paceForecast

    /** Whether any entry exists at all, regardless of period or filters. */
    val hasAnyEntries: StateFlow<Boolean> get() = aggregates.hasAnyEntries

    /** Entries dated after today inside the selected window, which the show-future toggle reveals. */
    val upcomingCount: StateFlow<Int> get() = aggregates.upcomingCount

    /** Balance of each visible account at period end, in its own currency. */
    val accountBalancesAtPeriodEnd: StateFlow<List<AccountWithBalance>> get() =
        aggregates.accountBalancesAtPeriodEnd

    /** Net total of all period-end balances in the default currency; null when a rate is missing. */
    val accountTotalAtPeriodEnd: StateFlow<Long?> get() = aggregates.accountTotalAtPeriodEnd

    /** Each period-end balance converted into the default currency, keyed by account id. */
    val accountConversionsAtPeriodEnd: StateFlow<Map<Long, Long?>> get() =
        aggregates.accountConversionsAtPeriodEnd

    /** Current balance of each visible account, used for balance adjustments. */
    val currentAccountBalances: StateFlow<Map<Long, Long>> get() = aggregates.currentAccountBalances

    /** Net total per category in the default currency derived from the filtered entries. */
    val categoryTotals: StateFlow<List<CategoryWithTotal>> get() = aggregates.categoryTotals

    /** Net total of all categories in the default currency; null when a rate is missing. */
    val categoryTotal: StateFlow<Long?> get() = aggregates.categoryTotal

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
