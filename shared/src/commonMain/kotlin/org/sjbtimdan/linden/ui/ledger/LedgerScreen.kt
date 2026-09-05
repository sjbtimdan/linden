package org.sjbtimdan.linden.ui.ledger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.ScreenMaxWidth
import org.sjbtimdan.linden.ui.ScreenPadding
import org.sjbtimdan.linden.ui.accounts.AccountWithBalance
import org.sjbtimdan.linden.ui.accounts.balanceAdjustment
import org.sjbtimdan.linden.ui.entry.EntryDialog
import org.sjbtimdan.linden.ui.entry.EntryRow
import org.sjbtimdan.linden.ui.entry.displayName
import org.sjbtimdan.linden.ui.entry.formatAmount
import org.sjbtimdan.linden.ui.entry.formatDate
import org.sjbtimdan.linden.ui.entry.parseAmount
import org.sjbtimdan.linden.ui.screenInsets
import org.sjbtimdan.linden.ui.theme.DialogShape
import org.sjbtimdan.linden.ui.theme.accentColor
import org.sjbtimdan.linden.ui.theme.lindenColors

private data class AdjustBalanceDialogState(
    val account: AccountWithBalance,
    val currentBalance: Long,
    val targetBalanceText: String,
    val categoryQuery: String = "",
)

@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToEntry: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
) {
    val accounts by viewModel.accounts.collectAsState()
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
    val currentAccountBalances by viewModel.currentAccountBalances.collectAsState()

    var adjustState by remember { mutableStateOf<AdjustBalanceDialogState?>(null) }
    // Starts collapsed so the tabs, period bar and list lead; active filters stay
    // visible as removable chips below the period bar.
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    var filtersOpen by remember { mutableStateOf(false) }
    // Search narrows entry text in the entries view and names in the other two — the label says which.
    val searchLabel = when (viewMode) {
        LedgerViewMode.Entries -> "Search entries"
        LedgerViewMode.Accounts -> "Filter accounts"
        LedgerViewMode.Categories -> "Filter categories"
    }
    // The Filters control only highlights while a chip filter applies; the dialog shows which are set.
    val hasActiveChipFilters = when (viewMode) {
        LedgerViewMode.Accounts -> false

        LedgerViewMode.Categories -> typeFilter != null

        LedgerViewMode.Entries ->
            typeFilter != null || categoryFilter != null || accountFilter != null || amountFilter != null
    }
    val searchFocusRequester = remember { FocusRequester() }
    var requestSearchFocus by remember { mutableStateOf(false) }
    LaunchedEffect(requestSearchFocus) {
        if (requestSearchFocus) {
            searchFocusRequester.requestFocus()
            requestSearchFocus = false
        }
    }

    val listItems = remember(displayedEntries) {
        ledgerListItems(entries = displayedEntries)
    }

    BackHandler(enabled = dialogState == null && (categoryFilter != null || accountFilter != null)) {
        viewModel.clearCategoryFilter()
        viewModel.clearAccountFilter()
    }

    BackHandler(enabled = dialogState != null) {
        viewModel.dismissDialog()
    }

    BackHandler(enabled = filtersOpen) {
        filtersOpen = false
    }

    Column(
        modifier = Modifier
            .screenInsets()
            .fillMaxSize()
            .imePadding()
            .padding(ScreenPadding)
            .widthIn(max = ScreenMaxWidth),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        label = { Text(searchLabel) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                            )
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(
                                    onClick = { viewModel.setSearchQuery("") },
                                ) { Icon(Icons.Default.Close, contentDescription = "Clear") }
                            }
                        } else {
                            null
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(searchFocusRequester)
                            .testTag("searchField"),
                    )
                }

                // The accounts view has no chip filters, so its Filters control is omitted.
                if (viewMode != LedgerViewMode.Accounts) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FilterChip(
                        selected = hasActiveChipFilters,
                        onClick = { filtersOpen = true },
                        modifier = Modifier.testTag("filtersButton"),
                        label = {
                            Text(
                                text = "Filters",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                            )
                        },
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
                    onToggleShowFuture = { viewModel.setShowFuture(!showFuture) },
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
        // explains what the calendar toggle did and offers to undo it.
        if (showFuture) {
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
        val activeTypeFilter = typeFilter
        val activeAmountFilter = amountFilter
        val activeFilterChips = listOfNotNull(
            searchQuery.takeIf { it.isNotBlank() },
            activeTypeFilter?.displayName(),
            categoryFilter?.let { id ->
                categories.firstOrNull { it.id == id }?.name ?: "Uncategorized"
            },
            accountFilter?.let { id ->
                accounts.firstOrNull { it.id == id }?.name ?: "Unknown account"
            },
            activeAmountFilter?.displayLabel(),
        )
        if (viewMode == LedgerViewMode.Entries && activeFilterChips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (searchQuery.isNotBlank()) {
                    EntryFilterChip(
                        name = searchQuery.trim(),
                        onClick = { viewModel.setSearchQuery("") },
                        modifier = Modifier.testTag("activeSearchFilterChip"),
                    )
                }
                if (activeTypeFilter != null) {
                    EntryFilterChip(
                        name = activeTypeFilter.displayName(),
                        onClick = { viewModel.setTypeFilter(null) },
                        modifier = Modifier.testTag("activeTypeFilterChip"),
                    )
                }
                categoryFilter?.let { id ->
                    val name = categories.firstOrNull { it.id == id }?.name ?: "Uncategorized"
                    EntryFilterChip(
                        name = name,
                        onClick = viewModel::clearCategoryFilter,
                        leadingColor = accentColor(name),
                        modifier = Modifier.testTag("categoryFilterChip"),
                    )
                }
                accountFilter?.let { id ->
                    EntryFilterChip(
                        name = accounts.firstOrNull { it.id == id }?.name ?: "Unknown account",
                        onClick = viewModel::clearAccountFilter,
                        modifier = Modifier.testTag("accountFilterChip"),
                    )
                }
                if (activeAmountFilter != null) {
                    EntryFilterChip(
                        name = activeAmountFilter.displayLabel(),
                        onClick = viewModel::clearAmountFilter,
                        modifier = Modifier.testTag("activeAmountFilterChip"),
                    )
                }
                if (activeFilterChips.size > 1) {
                    TextButton(
                        onClick = {
                            viewModel.setSearchQuery("")
                            viewModel.setTypeFilter(null)
                            viewModel.clearCategoryFilter()
                            viewModel.clearAccountFilter()
                            viewModel.clearAmountFilter()
                        },
                        modifier = Modifier.testTag("clearAllFilters"),
                    ) {
                        Text("Clear all")
                    }
                }
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
            // Adjust Balance targets today's balance; for a historical period the list
            // shows a period-end balance, so the action is disabled there.
            val canAdjustBalance = periodSelection.period.includes(viewModel.today(), periodSelection.anchor)
            AccountsList(
                balances = shownBalances,
                emptyMessage =
                if (accountFilter.isNotEmpty() && accountBalances.isNotEmpty()) {
                    "No accounts match."
                } else {
                    "No accounts yet."
                },
                emptyActionLabel = if (accountFilter.isEmpty()) "Create an account" else null,
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
                emptyMessage = when {
                    categoryFilter.isNotEmpty() && categoryTotals.isNotEmpty() -> "No categories match."

                    categories.isEmpty() -> "No categories yet."

                    // Spending exists but only after today, hidden by the show-future rule.
                    !showFuture && upcomingCount > 0 -> "Spending after today is hidden."

                    // Categories exist but none of them was used in the period.
                    else -> "No spending yet."
                },
                emptyActionLabel =
                if (categoryFilter.isEmpty() && categories.isEmpty()) "Add categories" else null,
                onEmptyAction = onNavigateToCategories,
                onCategoryClick = { viewModel.openCategory(it.category?.id ?: 0L) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else if (displayedEntries.isEmpty()) {
            // Guided empty state with a call to action for a brand-new dataset;
            // once entries exist anywhere the plain message explains the narrowing.
            val nothingFiltered = searchQuery.isBlank() &&
                typeFilter == null &&
                categoryFilter == null &&
                accountFilter == null &&
                amountFilter == null
            val guided = nothingFiltered && !hasAnyEntries
            // An empty list can hide upcoming entries: they exist in the window
            // but are dated after today, so the empty state must say so and
            // offer the show-future toggle instead of reporting no matches.
            val upcomingHidden = !showFuture && upcomingCount > 0 && nothingFiltered
            // Guided and upcoming-hidden states are mutually exclusive: guided
            // needs an empty database, upcoming-hidden needs upcoming entries.
            val revealUpcoming: () -> Unit = { viewModel.setShowFuture(true) }
            val emptyStateAction: (() -> Unit)? = when {
                guided -> onNavigateToEntry
                upcomingHidden -> revealUpcoming
                else -> null
            }
            EmptyState(
                message = when {
                    upcomingHidden -> "Entries after today are hidden."

                    guided -> "No entries yet."

                    (categoryFilter != null || accountFilter != null) &&
                        searchQuery.isBlank() &&
                        typeFilter == null &&
                        periodSelection.period == LedgerPeriod.All -> "No entries match this filter."

                    searchQuery.isBlank() && typeFilter == null && periodSelection.period == LedgerPeriod.All ->
                        "No entries yet."

                    else -> "No entries match."
                },
                actionLabel = when {
                    guided -> "Add your first entry"
                    upcomingHidden -> "Show entries after today"
                    else -> null
                },
                onAction = emptyStateAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("entryList"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listItems.forEach { item ->
                    when (item) {
                        is DayHeaderItem -> stickyHeader(item.key) {
                            DayHeader(label = item.label)
                        }

                        is EntryListItem -> item(item.key) {
                            EntryRow(
                                entry = item.entry,
                                onClick = { viewModel.openEditDialog(item.entry) },
                                // Day headers already show the date; only Day rows need a
                                // timestamp to tell entries apart.
                                showTimestamp = periodSelection.period == LedgerPeriod.Day,
                            )
                        }
                    }
                }
            }
        }
    }

    dialogState?.let { state ->
        EntryDialog(
            state = state,
            accounts = accounts,
            categories = categories,
            onAmountChange = viewModel::onAmountChange,
            onCategoryChange = viewModel::onCategoryChange,
            onAccountChange = viewModel::onAccountChange,
            onToAccountChange = viewModel::onToAccountChange,
            onToAmountChange = viewModel::onToAmountChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onCreatedAtChange = viewModel::onCreatedAtChange,
            onSave = { viewModel.saveDialog() },
            onDelete = if (state.editing != null) viewModel::deleteDialogEntry else null,
            onNavigateToSettings = onNavigateToSettings,
            onDismiss = viewModel::dismissDialog,
        )
    }

    adjustState?.let { state ->
        val targetBalance = parseAmount(state.targetBalanceText)
        val allCategories by viewModel.categories.collectAsState()
        var usedCategories by remember(state.account.account.id) { mutableStateOf<List<Category>>(emptyList()) }
        LaunchedEffect(state.account.account.id) {
            usedCategories = viewModel.usedCategories(state.account.account.id)
        }
        val usedIds = usedCategories.map { it.id }.toSet()
        val orderedCategories = usedCategories +
            allCategories.filterNot { it.id in usedIds }.sortedBy { it.name }
        val query = state.categoryQuery.trim()
        val visibleCategories = if (query.isEmpty()) {
            orderedCategories
        } else {
            orderedCategories.filter { it.name.contains(query, ignoreCase = true) }
        }
        // The category is selected by an exact (case-insensitive) match on the text field.
        val selectedCategory = orderedCategories.firstOrNull { it.name.equals(query, ignoreCase = true) }
        AdjustBalanceDialog(
            account = state.account.account,
            currentBalance = state.currentBalance,
            targetBalanceText = state.targetBalanceText,
            categoryQuery = state.categoryQuery,
            categories = visibleCategories,
            selectedCategoryId = selectedCategory?.id,
            onCategoryQueryChange = { adjustState = state.copy(categoryQuery = it) },
            onCategorySelect = { id ->
                val name = orderedCategories.firstOrNull { it.id == id }?.name ?: return@AdjustBalanceDialog
                adjustState = state.copy(categoryQuery = name)
            },
            onTargetBalanceChange = { adjustState = state.copy(targetBalanceText = it) },
            onSave = {
                if (targetBalance != null && selectedCategory != null) {
                    viewModel.adjustBalance(state.account.account, targetBalance, selectedCategory)
                    adjustState = null
                }
            },
            onDismiss = { adjustState = null },
        )
    }

    if (filtersOpen) {
        EntryFiltersDialog(
            showCategoryAndAccountFilters = viewMode == LedgerViewMode.Entries,
            typeFilter = typeFilter,
            onTypeFilterChange = viewModel::setTypeFilter,
            categories = categories,
            categoryFilter = categoryFilter,
            onCategoryFilterChange = viewModel::setCategoryFilter,
            accounts = accounts,
            accountFilter = accountFilter,
            onAccountFilterChange = viewModel::setAccountFilter,
            amountFilter = amountFilter,
            onAmountFilterChange = viewModel::setAmountFilter,
            onClearAmountFilter = viewModel::clearAmountFilter,
            onClearAll = {
                viewModel.setTypeFilter(null)
                viewModel.clearCategoryFilter()
                viewModel.clearAccountFilter()
                viewModel.clearAmountFilter()
            },
            onDismiss = { filtersOpen = false },
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
            contentDescription = if (expanded) "Collapse filters" else "Expand filters",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Filters & search",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!expanded && onSearchClick != null) {
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AdjustBalanceDialog(
    account: Account,
    currentBalance: Long,
    targetBalanceText: String,
    categoryQuery: String,
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategoryQueryChange: (String) -> Unit,
    onCategorySelect: (Long) -> Unit,
    onTargetBalanceChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val targetBalance = parseAmount(targetBalanceText)
    val adjustment = targetBalance?.let { balanceAdjustment(currentBalance, it) }
    val canSave = adjustment != null && !adjustment.isZero && selectedCategoryId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        title = { Text("Adjust Balance") },
        text = {
            Column {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Current balance: ${formatAmount(currentBalance)} ${account.currency.symbol}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = targetBalanceText,
                    onValueChange = onTargetBalanceChange,
                    label = { Text("Bank balance") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text(account.currency.symbol) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = categoryQuery,
                    onValueChange = onCategoryQueryChange,
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = category.id == selectedCategoryId,
                            onClick = { onCategorySelect(category.id) },
                            label = { Text(category.name) },
                        )
                    }
                }
                if (adjustment != null && !adjustment.isZero) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val direction = if (adjustment.delta > 0) "income" else "expense"
                    Text(
                        text = "Will add ${formatAmount(adjustment.delta)} as $direction.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = canSave) {
                Text("Adjust")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

internal sealed interface LedgerListItem {
    val key: Any
}

internal data class DayHeaderItem(
    override val key: Any,
    val label: String,
) : LedgerListItem

internal data class EntryListItem(val entry: Entry) : LedgerListItem {
    override val key: Any get() = entry.id
}

/** Builds the flat list of headers and entries shown by the ledger list. */
internal fun ledgerListItems(entries: List<Entry>): List<LedgerListItem> = buildList {
    var previousDay: LocalDate? = null
    entries.forEach { entry ->
        val day = entry.createdAt.toLocalDateTime(entry.createdZone).date
        if (day != previousDay) {
            add(DayHeaderItem("day-$day", formatDate(entry.createdAt, entry.createdZone)))
            previousDay = day
        }
        add(EntryListItem(entry))
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
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    hidden -> "***"
                    total != null -> formatTotal(total, currency)
                    else -> "–"
                },
                style = MaterialTheme.typography.labelLarge,
                color = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
