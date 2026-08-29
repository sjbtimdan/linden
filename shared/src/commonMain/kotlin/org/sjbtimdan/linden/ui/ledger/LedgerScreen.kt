package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.ScreenMaxWidth
import org.sjbtimdan.linden.ui.ScreenPadding
import org.sjbtimdan.linden.ui.entry.EntryDialog
import org.sjbtimdan.linden.ui.entry.EntryRow
import org.sjbtimdan.linden.ui.entry.displayName
import org.sjbtimdan.linden.ui.entry.formatDate
import org.sjbtimdan.linden.ui.screenInsets
import org.sjbtimdan.linden.ui.theme.accentColor
import org.sjbtimdan.linden.ui.theme.lindenColors

@Composable
fun LedgerScreen(viewModel: LedgerViewModel, onNavigateToSettings: () -> Unit = {}) {
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val showFuture by viewModel.showFuture.collectAsState()
    val periodSelection by viewModel.periodSelection.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val totalMinor by viewModel.totalMinor.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val accountBalances by viewModel.accountBalancesAtPeriodEnd.collectAsState()
    val accountTotal by viewModel.accountTotalAtPeriodEnd.collectAsState()
    val categoryTotals by viewModel.categoryTotals.collectAsState()
    val categoryTotal by viewModel.categoryTotal.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()
    val accountFilter by viewModel.accountFilter.collectAsState()
    val displayedEntries by viewModel.displayedEntries.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()

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
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { viewModel.setShowFuture(!showFuture) }
                    .testTag("showFutureToggle"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = showFuture,
                    onCheckedChange = null,
                )
                Text(
                    text = "Show future\nentries",
                    style = MaterialTheme.typography.labelSmall,
                    lineHeight = MaterialTheme.typography.labelSmall.lineHeight,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // The type filter only applies to entries and category totals, so it
            // is disabled while the accounts view is showing period-end balances.
            ChipDropdown(
                selected = typeFilter,
                options = typeFilterOptions,
                optionLabel = { it?.displayName() ?: "All" },
                onSelect = viewModel::setTypeFilter,
                modifier = Modifier.testTag("typeFilterDropdown"),
                enabled = viewMode != LedgerViewMode.Accounts,
            )
            ChipDropdown(
                selected = viewMode,
                options = LedgerViewMode.entries,
                optionLabel = { it.displayName() },
                onSelect = viewModel::setViewMode,
                modifier = Modifier.testTag("viewModeDropdown"),
            )
            if (viewMode == LedgerViewMode.Entries) {
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
                )
            }
            TotalLabel(
                total = when (viewMode) {
                    LedgerViewMode.Accounts -> accountTotal
                    LedgerViewMode.Categories -> categoryTotal
                    LedgerViewMode.Entries -> totalMinor
                },
                currency = defaultCurrency,
            )
        }

        if (viewMode == LedgerViewMode.Entries && (categoryFilter != null || accountFilter != null)) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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

private fun LedgerViewMode.displayName(): String = when (this) {
    LedgerViewMode.Entries -> "Entries"
    LedgerViewMode.Accounts -> "Accounts"
    LedgerViewMode.Categories -> "Categories"
}

@Composable
private fun TotalLabel(total: Long?, currency: Currency) {
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
                text = total?.let { formatTotal(it, currency) } ?: "–",
                style = MaterialTheme.typography.labelLarge,
                color = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
