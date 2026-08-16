package org.sjbtimdan.linden.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.entry.EntryDialog
import org.sjbtimdan.linden.ui.entry.EntryRow
import org.sjbtimdan.linden.ui.entry.displayName
import org.sjbtimdan.linden.ui.entry.formatDate
import org.sjbtimdan.linden.ui.theme.lindenColors

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToSettings: () -> Unit = {},
) {
    val entries by viewModel.entries.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val period by viewModel.period.collectAsState()
    val periodAnchor by viewModel.periodAnchor.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val totalMinor by viewModel.totalMinor.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()

    val listItems = remember(entries) {
        historyListItems(entries = entries)
    }

    Column(
        modifier = Modifier
            .safeContentPadding()
            .fillMaxSize()
            .padding(16.dp)
            .widthIn(max = 480.dp)
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
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = typeFilter == null,
                onClick = { viewModel.setTypeFilter(null) },
                label = { Text("All", style = MaterialTheme.typography.labelMedium) },
            )
            EntryType.entries.forEach { type ->
                FilterChip(
                    selected = typeFilter == type,
                    onClick = { viewModel.setTypeFilter(type) },
                    label = { Text(type.displayName(), style = MaterialTheme.typography.labelMedium) },
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
                    period = period,
                    anchor = periodAnchor,
                    onPeriodChange = viewModel::setPeriod,
                    onPrevious = viewModel::goToPreviousPeriod,
                    onNext = viewModel::goToNextPeriod,
                )
            }
            TotalLabel(
                total = totalMinor,
                currency = defaultCurrency,
            )
        }

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (searchQuery.isBlank() && typeFilter == null && period == HistoryPeriod.All) {
                        "No entries yet."
                    } else {
                        "No entries match."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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

internal sealed interface HistoryListItem {
    val key: Any
}

internal data class DayHeaderItem(
    override val key: Any,
    val label: String,
) : HistoryListItem

internal data class EntryListItem(val entry: Entry) : HistoryListItem {
    override val key: Any get() = entry.id
}

/** Builds the flat list of headers and entries shown by the history list. */
internal fun historyListItems(entries: List<Entry>): List<HistoryListItem> =
    buildList {
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
private fun TotalLabel(
    total: Long?,
    currency: Currency,
) {
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = total?.let { formatTotal(it, currency) } ?: "–",
                style = MaterialTheme.typography.titleMedium,
                color = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
