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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.entry.EntryForm
import org.sjbtimdan.linden.ui.entry.displayName
import org.sjbtimdan.linden.ui.entry.icon

private val entryTypes = listOf(EntryType.Expense, EntryType.Income, EntryType.Transfer)

@Composable
fun LedgerScreen(viewModel: LedgerViewModel, onNavigateToSettings: () -> Unit = {}) {
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val draft by viewModel.draft.collectAsState()
    val descriptionSuggestions by viewModel.descriptionSuggestions.collectAsState()
    val accountSuggestions by viewModel.accountSuggestions.collectAsState()
    val categorySuggestions by viewModel.categorySuggestions.collectAsState()
    val quickEntries by viewModel.quickEntries.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Seeds the draft from the last entry of the selected type on first show.
    // The ViewModel keeps the draft across configuration changes, so this only
    // runs when the screen (and thus the draft) is created fresh.
    LaunchedEffect(Unit) {
        viewModel.seedDraft()
    }

    var fieldFocused by remember { mutableStateOf(false) }

    // While a field is focused the form collapses to just that field and its
    // keyboard. The back arrow exits that state: it drops focus (which closes
    // text fields and dropdowns) and bumps [editEpoch] so EntryForm closes its
    // calculators. The draft is preserved — Clear is the full reset.
    var editEpoch by remember { mutableStateOf(0) }
    val exitEditing: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
        editEpoch++
    }
    val cancelEditing: () -> Unit = {
        if (fieldFocused) exitEditing() else viewModel.clearDraft()
    }

    BackHandler(enabled = draft != null) {
        cancelEditing()
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
            },
    ) {
        // While a field is focused the form collapses to just that field and its
        // keyboard, so the type selector hides too and only a visible cancel remains.
        if (!fieldFocused) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                entryTypes.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = { viewModel.selectType(type) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = entryTypes.size),
                        icon = {
                            Icon(
                                imageVector = type.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        label = { Text(type.displayName()) },
                    )
                }
            }
        }
        if (fieldFocused) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = cancelEditing) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
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
                    onAmountChange = viewModel::onAmountChange,
                    onCategoryChange = viewModel::onCategoryChange,
                    onAccountChange = viewModel::onAccountChange,
                    onToAccountChange = viewModel::onToAccountChange,
                    onToAmountChange = viewModel::onToAmountChange,
                    onDescriptionChange = viewModel::onDescriptionChange,
                    onCreatedAtChange = viewModel::onCreatedAtChange,
                    onNavigateToSettings = onNavigateToSettings,
                    // The form collapses to the focused field + its options; hide the
                    // action buttons too so everything fits above the keyboard.
                    onFieldFocusChange = { fieldFocused = it },
                    // Bumped by the back arrow so the form closes its calculators.
                    editEpoch = editEpoch,
                    descriptionSuggestions = descriptionSuggestions,
                    accountSuggestions = accountSuggestions,
                    categorySuggestions = categorySuggestions,
                    quickEntries = quickEntries,
                    onQuickEntry = viewModel::applyQuickEntry,
                )
            }
        }

        if (!fieldFocused) {
            Spacer(modifier = Modifier.height(8.dp))

            SnackbarHost(hostState = snackbarHostState)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        if (viewModel.saveDraft()) {
                            scope.launch { snackbarHostState.showSnackbar("Added") }
                        }
                    },
                    enabled = draft?.isValid(accounts) == true,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add")
                }

                OutlinedButton(
                    onClick = viewModel::clearDraft,
                    enabled = draft != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Clear")
                }
            }
        }
    }
}
