package org.sjbtimdan.linden.ui.ledger

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.accounts_empty_none
import org.sjbtimdan.linden.resources.categories_empty_none
import org.sjbtimdan.linden.resources.common_search
import org.sjbtimdan.linden.resources.ledger_accounts_all_hidden
import org.sjbtimdan.linden.resources.ledger_accounts_no_match
import org.sjbtimdan.linden.resources.ledger_action_add_categories
import org.sjbtimdan.linden.resources.ledger_action_create_account
import org.sjbtimdan.linden.resources.ledger_action_manage_accounts
import org.sjbtimdan.linden.resources.ledger_categories_future_hidden
import org.sjbtimdan.linden.resources.ledger_categories_no_match
import org.sjbtimdan.linden.resources.ledger_categories_no_spending
import org.sjbtimdan.linden.resources.ledger_collapse_filters
import org.sjbtimdan.linden.resources.ledger_expand_filters
import org.sjbtimdan.linden.resources.ledger_filters_header
import org.sjbtimdan.linden.resources.ledger_uncategorized
import org.sjbtimdan.linden.resources.ledger_unknown_account
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.ErrorSnackbar
import org.sjbtimdan.linden.ui.entry.EntryDialog
import org.sjbtimdan.linden.ui.entry.displayName
import org.sjbtimdan.linden.ui.entry.formatAmount
import org.sjbtimdan.linden.ui.screenContainerWithIme
import org.sjbtimdan.linden.ui.theme.accentColor
import org.sjbtimdan.linden.ui.theme.lindenColors

/**
 * Type options offered by the categories view: transfers never have a category,
 * so a Transfer type filter is not offered there (the ViewModel treats one as
 * All anyway).
 */
private val categoryTypeOptions = listOf(EntryType.Expense, EntryType.Income)

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
    val totalMinor by viewModel.totalMinor.collectAsState()
    val hideTotal by viewModel.hideTotal.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val accountBalances by viewModel.accountBalancesAtPeriodEnd.collectAsState()
    val accountTotal by viewModel.accountTotalAtPeriodEnd.collectAsState()
    val categoryTotals by viewModel.categoryTotals.collectAsState()
    val categoryTotal by viewModel.categoryTotal.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()
    val accountFilter by viewModel.accountFilter.collectAsState()
    val displayedEntries by viewModel.displayedEntries.collectAsState()
    val hasAnyEntries by viewModel.hasAnyEntries.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val currentAccountBalances by viewModel.currentAccountBalances.collectAsState()

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
    LaunchedEffect(requestSearchFocus) {
        if (requestSearchFocus) {
            searchFocusRequester.requestFocus()
            requestSearchFocus = false
        }
    }

    val listItems = remember(displayedEntries) {
        ledgerListItems(entries = displayedEntries, zone = viewModel.zone)
    }

    BackHandler(enabled = dialogState == null && (categoryFilter != null || accountFilter != null)) {
        viewModel.clearCategoryFilter()
        viewModel.clearAccountFilter()
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

        FiltersHeader(
            expanded = filtersExpanded,
            onToggle = { filtersExpanded = !filtersExpanded },
            onSearchClick = {
                filtersExpanded = true
                requestSearchFocus = true
            },
        )

        AnimatedVisibility(visible = filtersExpanded) {
            Column {
                LedgerSearchPanel(
                    query = searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    viewMode = viewMode,
                    categories = categories,
                    accounts = visibleAccounts,
                    activeCategoryId = categoryFilter,
                    activeAccountId = accountFilter,
                    onCategorySelected = viewModel::setCategoryFilter,
                    onAccountSelected = viewModel::setAccountFilter,
                    focusRequester = searchFocusRequester,
                )

                // The accounts view has no chip filters: a balance mixes every
                // entry type, so its panel only holds the search field. The other
                // views edit their filters inline — no dialog.
                if (viewMode != LedgerViewMode.Accounts) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LedgerFilterControls(
                        typeFilter = typeFilter,
                        onTypeFilterChange = viewModel::setTypeFilter,
                        typeOptions = if (viewMode == LedgerViewMode.Categories) {
                            categoryTypeOptions
                        } else {
                            typeOrder
                        },
                        showAmountFilter = viewMode == LedgerViewMode.Entries,
                        amountFilter = amountFilter,
                        onAmountFilterChange = viewModel::setAmountFilter,
                        onClearAmountFilter = viewModel::clearAmountFilter,
                    )
                }
            }
        }

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

        // Active filters as removable chips — the passive signal that the list is
        // narrowed, whether the filter panel is expanded or collapsed.
        if (viewMode == LedgerViewMode.Entries) {
            val activeFilters = buildList {
                if (searchQuery.isNotBlank()) {
                    add(
                        ActiveFilter(
                            name = searchQuery.trim(),
                            testTag = "activeSearchFilterChip",
                            onClear = { viewModel.setSearchQuery("") },
                        ),
                    )
                }
                typeFilter?.let { type ->
                    add(
                        ActiveFilter(
                            name = type.displayName(),
                            testTag = "activeTypeFilterChip",
                            onClear = { viewModel.setTypeFilter(null) },
                        ),
                    )
                }
                categoryFilter?.let { id ->
                    val name = categories.firstOrNull { it.id == id }?.name
                        ?: stringResource(Res.string.ledger_uncategorized)
                    add(
                        ActiveFilter(
                            name = name,
                            testTag = "categoryFilterChip",
                            onClear = viewModel::clearCategoryFilter,
                            leadingColor = accentColor(name),
                        ),
                    )
                }
                accountFilter?.let { id ->
                    add(
                        ActiveFilter(
                            name = accounts.firstOrNull { it.id == id }?.name
                                ?: stringResource(Res.string.ledger_unknown_account),
                            testTag = "accountFilterChip",
                            onClear = viewModel::clearAccountFilter,
                        ),
                    )
                }
                amountFilter?.let { filter ->
                    add(
                        ActiveFilter(
                            name = filter.displayLabel(),
                            testTag = "activeAmountFilterChip",
                            onClear = viewModel::clearAmountFilter,
                        ),
                    )
                }
            }
            if (activeFilters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                ActiveFilterChips(
                    filters = activeFilters,
                    onClearAll = {
                        viewModel.setSearchQuery("")
                        viewModel.setTypeFilter(null)
                        viewModel.clearCategoryFilter()
                        viewModel.clearAccountFilter()
                        viewModel.clearAmountFilter()
                    },
                )
            }
        }

        if (viewMode == LedgerViewMode.Accounts) {
            val accountFilter = searchQuery.trim()
            val shownBalances =
                if (accountFilter.isEmpty()) {
                    accountBalances
                } else {
                    accountBalances.filter { it.account.name.contains(accountFilter, ignoreCase = true) }
                }
            // Adjust Balance targets today's balance; for a period that does not
            // include today the list shows a period-end balance, so the action is
            // disabled there.
            val canAdjustBalance = periodSpansToday
            AccountsList(
                balances = shownBalances,
                hideAmounts = hideTotal,
                emptyMessage = when {
                    accountFilter.isNotEmpty() && accountBalances.isNotEmpty() ->
                        stringResource(Res.string.ledger_accounts_no_match)

                    // No visible accounts at all: distinguish a brand-new app from
                    // an app whose every account is hidden.
                    accounts.isEmpty() -> stringResource(Res.string.accounts_empty_none)

                    else -> stringResource(Res.string.ledger_accounts_all_hidden)
                },
                emptyActionLabel = when {
                    accountFilter.isNotEmpty() && accountBalances.isNotEmpty() -> null

                    accountFilter.isEmpty() && accounts.isEmpty() ->
                        stringResource(Res.string.ledger_action_create_account)

                    accountFilter.isEmpty() -> stringResource(Res.string.ledger_action_manage_accounts)

                    else -> null
                },
                onEmptyAction = onNavigateToAccounts,
                canAdjustBalance = canAdjustBalance,
                onAccountClick = { viewModel.openAccount(it.account.id) },
                onAdjustBalance = { item ->
                    val current = currentAccountBalances[item.account.id] ?: item.account.initialBalance
                    adjustState = AdjustBalanceDialogState(
                        account = item,
                        currentBalance = current,
                        targetBalanceText = formatAmount(current),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else if (viewMode == LedgerViewMode.Categories) {
            val categoryFilter = searchQuery.trim()
            val shownCategories =
                if (categoryFilter.isEmpty()) {
                    categoryTotals
                } else {
                    categoryTotals.filter {
                        it.category?.name?.contains(categoryFilter, ignoreCase = true) == true
                    }
                }
            CategoryTotalsList(
                categories = shownCategories,
                currency = defaultCurrency,
                hideAmounts = hideTotal,
                emptyMessage = when {
                    categoryFilter.isNotEmpty() && categoryTotals.isNotEmpty() ->
                        stringResource(Res.string.ledger_categories_no_match)

                    categories.isEmpty() -> stringResource(Res.string.categories_empty_none)

                    // Spending exists but only after today, hidden by the show-future rule.
                    !showFuture && upcomingCount > 0 ->
                        stringResource(Res.string.ledger_categories_future_hidden)

                    // Categories exist but none of them was used in the period.
                    else -> stringResource(Res.string.ledger_categories_no_spending)
                },
                emptyActionLabel =
                if (categoryFilter.isEmpty() && categories.isEmpty()) {
                    stringResource(Res.string.ledger_action_add_categories)
                } else {
                    null
                },
                onEmptyAction = onNavigateToCategories,
                onCategoryClick = { viewModel.openCategory(it.category?.id ?: 0L) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else if (displayedEntries.isEmpty()) {
            LedgerEntriesEmpty(
                state = ledgerEntriesEmptyState(
                    searchQuery = searchQuery,
                    typeFilter = typeFilter,
                    categoryFilter = categoryFilter,
                    accountFilter = accountFilter,
                    amountFilter = amountFilter,
                    periodIsAll = periodSelection.period == LedgerPeriod.All,
                    showFuture = showFuture,
                    upcomingCount = upcomingCount,
                    hasAnyEntries = hasAnyEntries,
                ),
                onAddFirstEntry = onNavigateToEntry,
                onRevealUpcoming = { viewModel.setShowFuture(true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else {
            LedgerEntriesList(
                items = listItems,
                hideAmounts = hideTotal,
                zone = viewModel.zone,
                onEntryClick = viewModel::openEditDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }

        SnackbarHost(hostState = snackbarHostState)
        viewModel.UndoDeleteBar()
    }

    dialogState?.let { state ->
        // Editing may open an entry that lives on a hidden account (its history is
        // kept), so the account pickers also offer exactly the hidden accounts the
        // draft references — switching onto any other hidden account stays impossible.
        val dialogAccounts = accounts.filter { account ->
            !account.hidden || account.id == state.accountId || account.id == state.toAccountId
        }
        EntryDialog(
            state = state,
            accounts = dialogAccounts,
            categories = categories,
            saving = saving,
            onAmountChange = viewModel::onAmountChange,
            onCategoryChange = viewModel::onCategoryChange,
            onAccountChange = viewModel::onAccountChange,
            onToAccountChange = viewModel::onToAccountChange,
            onToAmountChange = viewModel::onToAmountChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onCreatedAtChange = viewModel::onCreatedAtChange,
            onTypeChange = viewModel::changeDialogType,
            onSave = { viewModel.saveDialog() },
            onDelete = if (state.editing != null) viewModel::deleteDialogEntry else null,
            onDuplicate = viewModel::duplicateDialogEntry,
            onDismiss = viewModel::dismissDialog,
        )
    }

    adjustState?.let { state ->
        AdjustBalanceHost(
            viewModel = viewModel,
            state = state,
            hideAmounts = hideTotal,
            onStateChange = { adjustState = it },
            onDismiss = { adjustState = null },
        )
    }
}

/**
 * Always-visible header toggling the search/filter panel; while collapsed, its
 * trailing search icon expands the panel and focuses the field in one tap.
 */
@Composable
private fun FiltersHeader(expanded: Boolean, onToggle: () -> Unit, onSearchClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag("filtersHeader"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (expanded) {
                stringResource(Res.string.ledger_collapse_filters)
            } else {
                stringResource(Res.string.ledger_expand_filters)
            },
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.ledger_filters_header),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!expanded && onSearchClick != null) {
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(Res.string.common_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TotalLabel(total: Long?, currency: Currency, hidden: Boolean = false) {
    val colors = lindenColors()
    val tint = when {
        total != null && total < 0 -> colors.expense
        total != null && total > 0 -> colors.income
        else -> null
    }
    val container = when {
        total != null && total < 0 -> colors.expenseContainer
        total != null && total > 0 -> colors.incomeContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val animatedContainer by animateColorAsState(container, label = "totalContainer")
    val animatedTint by animateColorAsState(
        tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
        label = "totalTint",
    )
    Surface(
        shape = RoundedCornerShape(50),
        color = animatedContainer,
    ) {
        AnimatedContent(
            targetState = when {
                hidden -> "***"
                total != null -> formatTotal(total, currency)
                else -> "–"
            },
            transitionSpec = {
                (slideInVertically { height -> height / 2 } + fadeIn()) togetherWith
                    (slideOutVertically { height -> -height / 2 } + fadeOut())
            },
            label = "totalValue",
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = animatedTint,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
