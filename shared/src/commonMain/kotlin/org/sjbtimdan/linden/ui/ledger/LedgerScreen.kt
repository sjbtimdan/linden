package org.sjbtimdan.linden.ui.ledger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import org.sjbtimdan.linden.model.EntryType
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LedgerScreen(viewModel: LedgerViewModel, onNavigateToSettings: () -> Unit = {}) {
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val amountFilter by viewModel.amountFilter.collectAsState()
    val showFuture by viewModel.showFuture.collectAsState()
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
    val spendingInsights by viewModel.spendingInsights.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()
    val currentAccountBalances by viewModel.currentAccountBalances.collectAsState()

    var adjustState by remember { mutableStateOf<AdjustBalanceDialogState?>(null) }
    var adjustUnavailableAccount by remember { mutableStateOf<Account?>(null) }
    // Content-first: the search/filter panel starts collapsed so new users see
    // the mode tabs, the period bar and the list. Active filters stay visible
    // as removable summary chips below the period bar.
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    // Collapsing the spending insights hides the details behind a slim header for
    // the rest of the session; any period change re-expands them for the new window.
    var insightsCollapsed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(periodSelection) {
        insightsCollapsed = false
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
                        label = { Text("Search") },
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
                            .focusRequester(searchFocusRequester),
                    )
                }

                // The type filter applies to the entries and categories views; it is
                // hidden in the accounts view, where only period-end balances are shown.
                if (viewMode != LedgerViewMode.Accounts) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ChipDropdown(
                            selected = typeFilter,
                            options = typeFilterOptions,
                            optionLabel = { it?.displayName() ?: "Types: All" },
                            onSelect = viewModel::setTypeFilter,
                            modifier = Modifier.testTag("typeFilterDropdown"),
                        )
                    }
                }

                if (viewMode == LedgerViewMode.Entries) {
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ChipDropdown(
                            selected = categoryFilter,
                            options = listOf(null) + categories.map { it.id },
                            optionLabel = { id ->
                                id?.let { cid -> categories.firstOrNull { it.id == cid }?.name }
                                    ?: "Category: All"
                            },
                            onSelect = viewModel::setCategoryFilter,
                            modifier = Modifier.testTag("categoryFilterDropdown"),
                        )
                        ChipDropdown(
                            selected = accountFilter,
                            options = listOf(null) + accounts.map { it.id },
                            optionLabel = { id ->
                                id?.let { aid -> accounts.firstOrNull { it.id == aid }?.name }
                                    ?: "Account: All"
                            },
                            onSelect = viewModel::setAccountFilter,
                            modifier = Modifier.testTag("accountFilterDropdown"),
                        )
                        AmountFilterChip(
                            filter = amountFilter,
                            onApply = viewModel::setAmountFilter,
                            onClear = viewModel::clearAmountFilter,
                        )
                    }
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

        // Spending insights live with the content, not the filters: they sit between
        // the period bar and the list while the entries view shows a month. They can
        // be collapsed to a slim header and re-expanded in one tap (insightsCollapsed).
        val insights = spendingInsights
        if (viewMode == LedgerViewMode.Entries && insights != null) {
            Spacer(modifier = Modifier.height(8.dp))
            SpendingInsightsCard(
                insights = insights,
                currency = defaultCurrency,
                collapsed = insightsCollapsed,
                onToggleCollapsed = { insightsCollapsed = !insightsCollapsed },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Every active filter of the entries view is summarized here as a removable
        // chip, whether the filter panel above is expanded or collapsed. The summary
        // is the passive indicator that the list is narrowed, and "Clear all" resets
        // everything at once.
        val activeTypeFilter = typeFilter
        val activeAmountFilter = amountFilter
        val activeFilterChips = listOfNotNull(
            searchQuery.takeIf { it.isNotBlank() },
            activeTypeFilter?.let { it.displayName() },
            categoryFilter?.let { id ->
                categories.firstOrNull { it.id == id }?.name ?: "Uncategorized"
            },
            accountFilter?.let { id ->
                accounts.firstOrNull { it.id == id }?.name ?: "Unknown account"
            },
            activeAmountFilter?.let { it.displayLabel() },
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
            // Adjust Balance reconciles to the account's current balance, which only matches
            // the balance shown in the list when the selected period includes today. For a
            // historical period the list shows a period-end balance, so adjusting would be
            // confusing — keep the action discoverable but explain why it's unavailable.
            val canAdjustBalance = periodSelection.period.includes(viewModel.today(), periodSelection.anchor)
            AccountsList(
                balances = shownBalances,
                emptyMessage =
                if (accountFilter.isEmpty() ||
                    accountBalances.isEmpty()
                ) {
                    "No accounts yet."
                } else {
                    "No accounts match."
                },
                canAdjustBalance = canAdjustBalance,
                onAdjustBalance = { item ->
                    val current = currentAccountBalances[item.account.id] ?: item.account.initialBalance
                    adjustState = AdjustBalanceDialogState(
                        account = item,
                        currentBalance = current,
                        targetBalanceText = formatAmount(current),
                    )
                },
                onAdjustBalanceUnavailable = { item -> adjustUnavailableAccount = item.account },
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
                emptyMessage =
                if (categoryFilter.isEmpty() ||
                    categoryTotals.isEmpty()
                ) {
                    "No categories yet."
                } else {
                    "No categories match."
                },
                onCategoryClick = { viewModel.openCategory(it.category?.id ?: 0L) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else if (displayedEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when {
                        (categoryFilter != null || accountFilter != null) &&
                            searchQuery.isBlank() &&
                            typeFilter == null &&
                            periodSelection.period == LedgerPeriod.All -> "No entries match this filter."

                        searchQuery.isBlank() && typeFilter == null && periodSelection.period == LedgerPeriod.All ->
                            "No entries yet."

                        else -> "No entries match."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

    adjustUnavailableAccount?.let { account ->
        AlertDialog(
            onDismissRequest = { adjustUnavailableAccount = null },
            shape = DialogShape,
            title = { Text("Adjust Balance") },
            text = {
                Text(
                    text = "Adjust Balance can only be done when the period is the latest.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { adjustUnavailableAccount = null }) {
                    Text("OK")
                }
            },
        )
    }
}

/**
 * Always-visible header that toggles whether the search field and the filter
 * combo boxes are shown. Collapsing reclaims vertical space for the list; while
 * collapsed, the trailing search icon opens the panel and focuses the search
 * field in one tap.
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

/** All options of the type filter dropdown: the "All" (no filter) state plus every entry type. */
private val typeFilterOptions: List<EntryType?> = listOf(null) + EntryType.entries

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
