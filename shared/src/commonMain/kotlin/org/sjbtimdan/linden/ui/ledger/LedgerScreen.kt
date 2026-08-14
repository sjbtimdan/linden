package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.predictions.DescriptionPredictionInput
import org.sjbtimdan.linden.predictions.PREDICTION_TOP_N
import org.sjbtimdan.linden.predictions.predictDescriptions
import org.sjbtimdan.linden.ui.entry.EntryDialog
import org.sjbtimdan.linden.ui.entry.EntryDialogState
import org.sjbtimdan.linden.ui.entry.EntryRow
import org.sjbtimdan.linden.ui.entry.displayName

@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel,
    onNavigateToSettings: () -> Unit = {},
) {
    val entries by viewModel.recentEntries.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val allEntries by viewModel.allEntries.collectAsState()
    var dialogState by remember { mutableStateOf<EntryDialogState?>(null) }
    val scope = rememberCoroutineScope()

    val descriptionSuggestions = remember(dialogState, allEntries) {
        dialogState?.let { state ->
            if (state.editing != null) {
                emptyList()
            } else {
                predictDescriptions(
                    entries = allEntries,
                    input = DescriptionPredictionInput(
                        type = state.type,
                        categoryId = state.categoryId,
                        accountId = state.accountId,
                        amount = state.amount,
                    ),
                    now = Clock.System.now(),
                    timeZone = TimeZone.currentSystemDefault(),
                    topN = PREDICTION_TOP_N
                )
            }
        } ?: emptyList()
    }

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

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No entries in the last 7 days.",
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                EntryType.Expense,
                EntryType.Income,
                EntryType.Transfer,
            ).forEach { type ->
                FilledTonalButton(
                    onClick = { scope.launch { dialogState = viewModel.newEntryState(type) } },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text("Add ${type.displayName()}")
                }
            }
        }
    }

    dialogState?.let { state ->
        EntryDialog(
            state = state,
            accounts = accounts,
            categories = categories,
            onAmountChange = { dialogState = state.copy(amountText = it) },
            onCategoryChange = { dialogState = state.copy(categoryId = it) },
            onAccountChange = { dialogState = state.copy(accountId = it) },
            onToAccountChange = { dialogState = state.copy(toAccountId = it) },
            onToAmountChange = { dialogState = state.copy(toAmountText = it) },
            onDescriptionChange = { dialogState = state.copy(description = it) },
            onCreatedAtChange = { dialogState = state.copy(createdAt = it) },
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
            descriptionSuggestions = descriptionSuggestions,
        )
    }
}
