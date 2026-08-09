package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sjbtimdan.linden.model.EntryType

@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel,
    onNavigateToSettings: () -> Unit = {},
) {
    val entries by viewModel.entries.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    var dialogState by remember { mutableStateOf<EntryDialogState?>(null) }

    Column(
        modifier = Modifier
            .safeContentPadding()
            .fillMaxSize()
            .padding(16.dp)
            .widthIn(max = 480.dp)
    ) {
        Text(
            text = "Ledger",
            fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 16.dp),
        )

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
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = typeFilter == null,
                onClick = { viewModel.setTypeFilter(null) },
                label = { Text("All") },
            )
            EntryType.entries.forEach { type ->
                FilterChip(
                    selected = typeFilter == type,
                    onClick = { viewModel.setTypeFilter(type) },
                    label = { Text(type.displayName()) },
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        SortDropdown(
            current = sortOrder,
            onChange = viewModel::setSortOrder,
        )

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (searchQuery.isBlank() && typeFilter == null) {
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
            ) {
                items(entries, key = { it.id }) { entry ->
                    EntryRow(
                        entry = entry,
                        onClick = { dialogState = EntryDialogState.forEdit(entry) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        FilledTonalButton(
            onClick = { dialogState = EntryDialogState.forNew() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("+ New Entry")
        }
    }

    dialogState?.let { state ->
        EntryDialog(
            state = state,
            accounts = accounts,
            categories = categories,
            onTypeChange = { dialogState = state.copy(type = it) },
            onAmountChange = { dialogState = state.copy(amountText = it) },
            onCategoryChange = { dialogState = state.copy(categoryId = it) },
            onAccountChange = { dialogState = state.copy(accountId = it) },
            onToAccountChange = { dialogState = state.copy(toAccountId = it) },
            onToAmountChange = { dialogState = state.copy(toAmountText = it) },
            onDescriptionChange = { dialogState = state.copy(description = it) },
            onSave = {
                state.toEntry(accounts, categories)?.let { entry ->
                    if (state.editing != null) {
                        viewModel.updateEntry(entry)
                    } else {
                        viewModel.createEntry(entry)
                    }
                    dialogState = null
                }
            },
            onDelete = state.editing?.let { editing ->
                {
                    viewModel.deleteEntry(editing.id)
                    dialogState = null
                }
            },
            onNavigateToSettings = onNavigateToSettings,
            onDismiss = { dialogState = null },
        )
    }
}
