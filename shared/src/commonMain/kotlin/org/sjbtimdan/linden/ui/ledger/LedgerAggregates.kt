package org.sjbtimdan.linden.ui.ledger

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.data.BudgetDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.model.dayIn
import org.sjbtimdan.linden.time.AppClock
import org.sjbtimdan.linden.ui.accounts.AccountWithBalance
import org.sjbtimdan.linden.ui.accounts.accountBalancesAtEnd
import org.sjbtimdan.linden.ui.accounts.accountConversionsMinor
import org.sjbtimdan.linden.ui.accounts.accountTotalMinor
import org.sjbtimdan.linden.ui.budget.budgetsByCategoryName
import org.sjbtimdan.linden.ui.insights.PaceForecast
import org.sjbtimdan.linden.util.stateFlow
import kotlin.math.abs
import org.sjbtimdan.linden.ui.insights.paceForecast as computePaceForecast

/**
 * The ledger's derived rows, totals and balances. Kept out of
 * [LedgerViewModel] so the filter inputs, the entry window and every
 * aggregation stay in one testable place, while the ViewModel keeps the
 * mutation surface (filters, dialog, balance adjustment).
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class LedgerAggregates(
    private val entryDao: EntryDao,
    private val budgetDao: BudgetDao,
    private val allEntries: StateFlow<List<Entry>>,
    private val filters: LedgerFilterState,
    private val accounts: StateFlow<List<Account>>,
    private val visibleAccounts: StateFlow<List<Account>>,
    private val defaultCurrency: StateFlow<Currency>,
    private val rates: StateFlow<List<FxRate>>,
    private val clock: AppClock,
    private val zone: TimeZone,
    private val scope: CoroutineScope,
) {
    private fun today(): LocalDate = clock.todayIn(zone)

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
        .stateFlow(scope, emptyList())

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
    }.stateFlow(scope, emptyList())

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
    }.stateFlow(scope, emptyList())

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
    }.stateFlow(scope, 0L)

    /** Current-month expense pace in the default currency; null until enough history. */
    val paceForecast: StateFlow<PaceForecast?> = combine(
        allEntries,
        defaultCurrency,
        rates,
    ) { entries, currency, rates ->
        computePaceForecast(entries, today(), currency, rates, zone)
    }.stateFlow(scope, null)

    /** Whether any entry exists at all, regardless of period or filters — drives the
     * guided empty state for a new app vs the plain message for an empty match. */
    val hasAnyEntries: StateFlow<Boolean> = entryDao.entryExists().stateFlow(scope, false)

    /** Last day of the selected period (inclusive); null for [LedgerPeriod.All]. */
    private val periodEnd: StateFlow<LocalDate?> = filters.periodWindow
        .map { it?.end }
        .stateFlow(scope, null)

    /** Entries dated up to the period end, fetched with a safety margin. */
    private val entriesUpToPeriodEnd: StateFlow<List<Entry>> = periodEnd
        .flatMapLatest { end ->
            val source = end?.let { entryDao.getUpTo(it.sqlUpperBound()) } ?: allEntries
            source
        }
        .stateFlow(scope, emptyList())

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
    }.stateFlow(scope, 0)

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
    }.stateFlow(scope, emptyList())

    /** Net total of all period-end balances in the default currency; null when a rate is missing. */
    val accountTotalAtPeriodEnd: StateFlow<Long?> = combine(
        accountBalancesAtPeriodEnd,
        defaultCurrency,
        rates,
    ) { balances, currency, rates ->
        accountTotalMinor(balances, currency, rates)
    }.stateFlow(scope, null)

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
    }.stateFlow(scope, emptyMap())

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
        accountBalancesAtEnd(entries, today(), accounts, zone).associate { it.account.id to it.balance }
    }.stateFlow(scope, emptyMap())

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
    }.stateFlow(scope, emptyList())

    /** Net total of all categories in the default currency; null when a rate is missing. */
    val categoryTotal: StateFlow<Long?> = combine(
        categoryEntries,
        defaultCurrency,
        rates,
    ) { entries, currency, rates ->
        periodTotalMinor(entries, currency, rates)
    }.stateFlow(scope, null)
}
