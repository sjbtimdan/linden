package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.predictions.DescriptionPredictionInput
import org.sjbtimdan.linden.predictions.PREDICTION_TOP_N
import org.sjbtimdan.linden.predictions.predictDescriptions
import org.sjbtimdan.linden.ui.entry.EntryDraft
import org.sjbtimdan.linden.ui.entry.EntryForm
import org.sjbtimdan.linden.ui.entry.displayName

private val entryTypes = listOf(EntryType.Expense, EntryType.Income, EntryType.Transfer)

@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel,
    onNavigateToSettings: () -> Unit = {},
) {
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val allEntries by viewModel.allEntries.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedType by remember { mutableStateOf(EntryType.Expense) }
    var draft by remember { mutableStateOf<EntryDraft?>(null) }

    // Rebuild the draft when the type changes, carrying over the fields that
    // are shared across types (amount, description, date & time).
    LaunchedEffect(selectedType) {
        val previous = draft
        val fresh = viewModel.newEntryState(selectedType)
        draft = if (previous != null && previous.type != selectedType) {
            fresh.carryOverCommonFields(previous)
        } else {
            fresh
        }
    }

    val descriptionSuggestions = remember(draft, allEntries) {
        draft?.let { state ->
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
            .imePadding()
            .padding(16.dp)
            .widthIn(max = 480.dp)
            // Tapping anywhere outside a field dismisses the keyboard. Focus loss
            // alone doesn't always close the IME on Android, so hide explicitly.
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            }
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            entryTypes.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = entryTypes.size),
                ) {
                    Text(type.displayName())
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            draft?.let { state ->
                EntryForm(
                    state = state,
                    accounts = accounts,
                    categories = categories,
                    onAmountChange = { draft = state.copy(amountText = it) },
                    onCategoryChange = { draft = state.copy(categoryId = it) },
                    onAccountChange = { draft = state.copy(accountId = it) },
                    onToAccountChange = { draft = state.copy(toAccountId = it) },
                    onToAmountChange = { draft = state.copy(toAmountText = it) },
                    onDescriptionChange = { draft = state.copy(description = it) },
                    onCreatedAtChange = { draft = state.copy(createdAt = it) },
                    onNavigateToSettings = onNavigateToSettings,
                    descriptionSuggestions = descriptionSuggestions,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SnackbarHost(hostState = snackbarHostState)

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    val state = draft ?: return@Button
                    state.toEntry(accounts, categories)?.let { entry ->
                        viewModel.createEntry(entry)
                        // Reset to a fresh draft prefilled from the saved entry so
                        // the next entry of the same type can be entered right away.
                        draft = EntryDraft.forNew(entry.type, entry)
                        scope.launch { snackbarHostState.showSnackbar("Added") }
                    }
                },
                enabled = draft?.isValid(accounts) == true,
                modifier = Modifier.weight(1f),
            ) {
                Text("Add")
            }

            OutlinedButton(
                onClick = {
                    draft = EntryDraft.forNew(selectedType)
                },
                enabled = draft != null,
                modifier = Modifier.weight(1f),
            ) {
                Text("Clear")
            }
        }
    }
}
