package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.ErrorSnackbar
import org.sjbtimdan.linden.ui.LocalBottomBarContentPadding
import org.sjbtimdan.linden.ui.insights.PaceConfidence
import org.sjbtimdan.linden.ui.screenContainerWithIme

@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel,
    onNavigateToEntry: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
) {
    val accounts by viewModel.accounts.collectAsState()
    val visibleAccounts by viewModel.visibleAccounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val amountFilter by viewModel.amountFilter.collectAsState()
    val showFuture by viewModel.showFuture.collectAsState()
    val upcomingCount by viewModel.upcomingCount.collectAsState()
    val periodSelection by viewModel.periodSelection.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val fxRates by viewModel.fxRates.collectAsState()
    val totalMinor by viewModel.totalMinor.collectAsState()
    val hideTotal by viewModel.hideTotal.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val accountBalances by viewModel.accountBalancesAtPeriodEnd.collectAsState()
    val accountTotal by viewModel.accountTotalAtPeriodEnd.collectAsState()
    val accountConversions by viewModel.accountConversionsAtPeriodEnd.collectAsState()
    val categoryTotals by viewModel.categoryTotals.collectAsState()
    val categoryTotal by viewModel.categoryTotal.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()
    val accountFilter by viewModel.accountFilter.collectAsState()
    val displayedEntries by viewModel.displayedEntries.collectAsState()
    val hasAnyEntries by viewModel.hasAnyEntries.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val currentAccountBalances by viewModel.currentAccountBalances.collectAsState()
    val paceForecast by viewModel.paceForecast.collectAsState()

    val todayDate = viewModel.today()
    val periodStart = periodSelection.period.windowStart(periodSelection.anchor)
    val periodEnd = periodSelection.period.windowEnd(periodSelection.anchor)

    // Adjust Balance targets today's balance, so it is only offered while the shown
    // window contains today.
    val periodSpansToday = periodSelection.period.includes(todayDate, periodSelection.anchor)
    // The show-future toggle only matters while the shown window still holds days
    // after today to reveal: its end lies strictly after today (or it is the
    // unbounded All). A window closing today — a Week on its last day, a Day view,
    // a period seen on its final day — never hides anything, so the toggle would be
    // a no-op there.
    val showFutureRelevant = periodEnd == null ||
        (periodStart != null && periodStart <= todayDate && todayDate < periodEnd)

    var adjustState by remember { mutableStateOf<AdjustBalanceDialogState?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    // Starts collapsed so the tabs, period bar and list lead; active filters stay
    // visible as removable chips below the period bar.
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    var requestSearchFocus by remember { mutableStateOf(false) }

    // The one-line Pace strip belongs to the default current-month entries view:
    // a filtered or future-inclusive list no longer describes the whole month.
    val showPace = viewMode == LedgerViewMode.Entries &&
        periodSelection.period == LedgerPeriod.Month &&
        periodSelection.period.includes(todayDate, periodSelection.anchor) &&
        !hideTotal &&
        !showFuture &&
        !filtersExpanded &&
        searchQuery.isBlank() &&
        typeFilter == null &&
        amountFilter == null &&
        categoryFilter == null &&
        accountFilter == null &&
        paceForecast?.confidence != PaceConfidence.Low &&
        paceForecast?.missingCurrencies?.isEmpty() == true
    val paceHeader: (@Composable () -> Unit)? = if (showPace) {
        paceForecast?.let { forecast ->
            { PaceStrip(forecast = forecast, currency = defaultCurrency) }
        }
    } else {
        null
    }

    LaunchedEffect(requestSearchFocus) {
        if (requestSearchFocus) {
            searchFocusRequester.requestFocus()
            requestSearchFocus = false
        }
    }

    BackHandler(enabled = dialogState == null && (categoryFilter != null || accountFilter != null)) {
        viewModel.exitDrillDown()
    }

    BackHandler(enabled = dialogState != null) {
        viewModel.dismissDialog()
    }

    viewModel.ErrorSnackbar(snackbarHostState)

    Column(
        modifier = Modifier.screenContainerWithIme(),
    ) {
        LedgerViewModeTabs(
            viewMode = viewMode,
            onSelect = viewModel::setViewMode,
        )

        Spacer(modifier = Modifier.height(8.dp))

        LedgerFilterPanel(
            expanded = filtersExpanded,
            onToggle = { filtersExpanded = !filtersExpanded },
            onSearchClick = {
                filtersExpanded = true
                requestSearchFocus = true
            },
            viewMode = viewMode,
            searchQuery = searchQuery,
            onSearchQueryChange = viewModel::setSearchQuery,
            categories = categories,
            accounts = visibleAccounts,
            activeCategoryId = categoryFilter,
            activeAccountId = accountFilter,
            onCategorySelected = viewModel::setCategoryFilter,
            onAccountSelected = viewModel::setAccountFilter,
            focusRequester = searchFocusRequester,
            typeFilter = typeFilter,
            onTypeFilterChange = viewModel::setTypeFilter,
            amountFilter = amountFilter,
            onAmountFilterChange = viewModel::setAmountFilter,
            onClearAmountFilter = viewModel::clearAmountFilter,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                PeriodNavigator(
                    period = periodSelection.period,
                    anchor = periodSelection.anchor,
                    onPeriodChange = viewModel::setPeriod,
                    onPrevious = viewModel::goToPreviousPeriod,
                    onNext = viewModel::goToNextPeriod,
                    showFuture = showFuture,
                    onToggleShowFuture = if (showFutureRelevant) {
                        { viewModel.setShowFuture(!showFuture) }
                    } else {
                        null
                    },
                    today = todayDate,
                    onToday = viewModel::goToToday,
                )
            }
            TotalLabel(
                total = when (viewMode) {
                    LedgerViewMode.Accounts -> accountTotal
                    LedgerViewMode.Categories -> categoryTotal
                    LedgerViewMode.Entries -> totalMinor
                },
                currency = defaultCurrency,
                hidden = hideTotal,
            )
        }

        // While future entries are shown, the notice next to the period bar
        // explains what the calendar toggle did and offers to undo it. It only
        // appears while the window still holds days after today to reveal —
        // elsewhere the toggle is hidden and no entry is kept out of view.
        if (showFuture && showFutureRelevant) {
            Spacer(modifier = Modifier.height(8.dp))
            FutureEntriesNotice(
                label = futureEntriesNoticeLabel(
                    viewMode = viewMode,
                    upcoming = upcomingCount,
                    bounded = periodSelection.period != LedgerPeriod.All,
                ),
                onClick = { viewModel.setShowFuture(false) },
            )
        }

        LedgerActiveFilters(
            viewMode = viewMode,
            searchQuery = searchQuery,
            typeFilter = typeFilter,
            categoryFilter = categoryFilter,
            accountFilter = accountFilter,
            amountFilter = amountFilter,
            categories = categories,
            accounts = accounts,
            onClearSearch = { viewModel.setSearchQuery("") },
            onClearType = { viewModel.setTypeFilter(null) },
            onClearCategory = viewModel::clearCategoryFilter,
            onClearAccount = viewModel::clearAccountFilter,
            onClearAmount = viewModel::clearAmountFilter,
            onClearAll = {
                viewModel.setSearchQuery("")
                viewModel.setTypeFilter(null)
                viewModel.clearCategoryFilter()
                viewModel.clearAccountFilter()
                viewModel.clearAmountFilter()
            },
        )

        when (viewMode) {
            LedgerViewMode.Accounts -> LedgerAccountsView(
                accounts = accounts,
                balances = accountBalances,
                conversions = accountConversions,
                defaultCurrency = defaultCurrency,
                hideAmounts = hideTotal,
                searchQuery = searchQuery,
                canAdjustBalance = periodSpansToday,
                currentBalances = currentAccountBalances,
                onEmptyAction = onNavigateToAccounts,
                onAccountClick = { viewModel.openAccount(it.account.id) },
                onAdjustBalance = { adjustState = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            LedgerViewMode.Categories -> LedgerCategoriesView(
                categories = categories,
                totals = categoryTotals,
                defaultCurrency = defaultCurrency,
                hideAmounts = hideTotal,
                searchQuery = searchQuery,
                showFuture = showFuture,
                upcomingCount = upcomingCount,
                onEmptyAction = onNavigateToCategories,
                onCategoryClick = { viewModel.openCategory(it.category?.id ?: 0L) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            LedgerViewMode.Entries -> LedgerEntriesView(
                entries = displayedEntries,
                zone = viewModel.zone,
                defaultCurrency = defaultCurrency,
                rates = fxRates,
                hideAmounts = hideTotal,
                searchQuery = searchQuery,
                typeFilter = typeFilter,
                categoryFilter = categoryFilter,
                accountFilter = accountFilter,
                amountFilter = amountFilter,
                periodIsAll = periodSelection.period == LedgerPeriod.All,
                showFuture = showFuture,
                upcomingCount = upcomingCount,
                hasAnyEntries = hasAnyEntries,
                paceHeader = paceHeader,
                onEntryClick = viewModel::openEditDialog,
                onAddFirstEntry = onNavigateToEntry,
                onRevealUpcoming = { viewModel.setShowFuture(true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.offset(y = -LocalBottomBarContentPadding.current),
        )
        viewModel.UndoDeleteBar(
            modifier = Modifier.offset(y = -LocalBottomBarContentPadding.current),
        )
    }

    LedgerDialogHost(
        viewModel = viewModel,
        accounts = accounts,
        categories = categories,
        dialogState = dialogState,
        saving = saving,
        adjustState = adjustState,
        hideAmounts = hideTotal,
        onAdjustStateChange = { adjustState = it },
    )
}
