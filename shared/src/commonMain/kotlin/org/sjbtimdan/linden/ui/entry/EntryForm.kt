package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.predictions.QuickEntry
import org.sjbtimdan.linden.ui.BackHandler
import kotlin.time.Instant

/** Which field is expanded right now; while one is, the rest of the form collapses. */
private enum class ActiveField { Description, From, To, Category, Account, Amount, ToAmount }

/**
 * The field section of the entry editor, shared between the inline form on the
 * entry screen and the edit dialog on the ledger screen.
 */
@Composable
fun EntryForm(
    state: EntryDraft,
    accounts: List<Account>,
    categories: List<Category>,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onAccountChange: (Long?) -> Unit,
    onToAccountChange: (Long?) -> Unit,
    onToAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCreatedAtChange: (Instant) -> Unit,
    onNavigateToSettings: () -> Unit,
    onFieldFocusChange: (Boolean) -> Unit = {},
    editEpoch: Int = 0,
    descriptionSuggestions: List<String> = emptyList(),
    accountSuggestions: List<Long> = emptyList(),
    categorySuggestions: List<Long> = emptyList(),
    quickEntries: List<QuickEntry> = emptyList(),
    onQuickEntry: (QuickEntry) -> Unit = {},
) {
    val visibleCategories = when (state.type) {
        EntryType.Expense -> categories.filter { it.type != CategoryType.Income }
        EntryType.Income -> categories.filter { it.type != CategoryType.Expense }
        EntryType.Transfer -> emptyList()
    }
    val fromAccount = accounts.firstOrNull { it.id == state.accountId }
    val toAccount = accounts.firstOrNull { it.id == state.toAccountId }
    val showReceivedAmount = state.type == EntryType.Transfer &&
        fromAccount != null && toAccount != null &&
        fromAccount.currency != toAccount.currency

    // While a field is focused the rest of the form collapses so that field and
    // its options get the whole area above the keyboard. The focused field itself
    // must stay mounted — unmounting it would drop focus — so each section only
    // hides while a *different* field is active.
    var activeField by remember { mutableStateOf<ActiveField?>(null) }
    var amountWarning by remember { mutableStateOf<String?>(null) }
    var toAmountWarning by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(activeField) {
        onFieldFocusChange(activeField != null)
    }
    val editing = activeField != null

    // The screen's back arrow bumps this to close the calculators from outside;
    // the focused-field states are focus-driven and drop with clearFocus instead.
    LaunchedEffect(editEpoch) {
        if (activeField == ActiveField.Amount || activeField == ActiveField.ToAmount) activeField = null
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    fun openCalculator() {
        activeField = ActiveField.Amount
        amountWarning = null
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    fun openToCalculator() {
        activeField = ActiveField.ToAmount
        toAmountWarning = null
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    // The back key closes an expanded dropdown, calculator, or description field.
    BackHandler(enabled = activeField != null) {
        activeField = null
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (!editing) {
            AmountField(
                value = state.amountText,
                label = if (state.type == EntryType.Transfer) "Amount (sent)" else "Amount",
                suffix = fromAccount?.currency?.symbol,
                warning = amountWarning,
                onValueChange = {
                    onAmountChange(it)
                    amountWarning = null
                },
                onFocus = ::openCalculator,
                modifier = Modifier.testTag("amountField"),
            )
        }

        if (state.type == EntryType.Transfer) {
            if (!editing || activeField == ActiveField.From) {
                Spacer(modifier = Modifier.height(16.dp))
                FieldDropdown(
                    label = "From account",
                    selected = accounts.firstOrNull { it.id == state.accountId },
                    options = accounts,
                    optionLabel = { it.name },
                    onSelect = { onAccountChange(it.id) },
                    onFocusChange = { activeField = if (it) ActiveField.From else null },
                    missing = if (accounts.isEmpty()) "Please enter account" else null,
                    onNavigateToSettings = onNavigateToSettings,
                )
            }
            if (!editing || activeField == ActiveField.To) {
                Spacer(modifier = Modifier.height(16.dp))
                FieldDropdown(
                    label = "To account",
                    selected = accounts.firstOrNull { it.id == state.toAccountId },
                    options = accounts.filter { it.id != state.accountId },
                    optionLabel = { it.name },
                    onSelect = { onToAccountChange(it.id) },
                    onFocusChange = { activeField = if (it) ActiveField.To else null },
                    missing = if (accounts.size < 2) {
                        if (accounts.isEmpty()) "Please enter account" else "Please add a second account"
                    } else {
                        null
                    },
                    onNavigateToSettings = onNavigateToSettings,
                )
            }
            if (!editing && showReceivedAmount) {
                Spacer(modifier = Modifier.height(16.dp))
                AmountField(
                    value = state.toAmountText,
                    label = "Amount (received)",
                    suffix = toAccount.currency.symbol,
                    warning = toAmountWarning,
                    onValueChange = {
                        onToAmountChange(it)
                        toAmountWarning = null
                    },
                    onFocus = ::openToCalculator,
                )
            }
        } else {
            if (!editing || activeField == ActiveField.Category) {
                Spacer(modifier = Modifier.height(16.dp))
                FieldDropdown(
                    label = "Category",
                    selected = visibleCategories.firstOrNull { it.id == state.categoryId },
                    options = visibleCategories,
                    optionLabel = { it.name },
                    onSelect = { onCategoryChange(it.id) },
                    onFocusChange = { activeField = if (it) ActiveField.Category else null },
                    predicted = categorySuggestions
                        .filterNot { it == state.categoryId }
                        .mapNotNull { id -> visibleCategories.firstOrNull { it.id == id } },
                    missing = if (visibleCategories.isEmpty()) "Please enter category" else null,
                    onNavigateToSettings = onNavigateToSettings,
                    optionIcon = { it.icon?.imageVector() },
                )
            }
            if (!editing || activeField == ActiveField.Account) {
                Spacer(modifier = Modifier.height(16.dp))
                FieldDropdown(
                    label = "Account",
                    selected = accounts.firstOrNull { it.id == state.accountId },
                    options = accounts,
                    optionLabel = { it.name },
                    onSelect = { onAccountChange(it.id) },
                    onFocusChange = { activeField = if (it) ActiveField.Account else null },
                    predicted = accountSuggestions
                        .filterNot { it == state.accountId }
                        .mapNotNull { id -> accounts.firstOrNull { it.id == id } },
                    missing = if (accounts.isEmpty()) "Please enter account" else null,
                    onNavigateToSettings = onNavigateToSettings,
                )
            }
        }

        if (!editing) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // While a dropdown or calculator is open only that field and its chips
        // show; the description would just be dead space beneath them.
        if (activeField == null || activeField == ActiveField.Description) {
            val visibleSuggestions = state.description.trim().let { query ->
                val matches = if (query.isEmpty()) {
                    descriptionSuggestions
                } else {
                    descriptionSuggestions.filter { it.contains(query, ignoreCase = true) }
                }
                matches.filterNot { it.equals(query, ignoreCase = true) }
            }
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text("Description (optional)") },
                singleLine = true,
                trailingIcon = if (state.description.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { onDescriptionChange("") },
                        ) { Icon(Icons.Default.Close, contentDescription = "Clear") }
                    }
                } else {
                    null
                },
                modifier = Modifier
                    .onFocusChanged { activeField = if (it.isFocused) ActiveField.Description else null }
                    .fillMaxWidth(),
            )
            // Chips live in the layout instead of a popup so the keyboard can never cover them;
            // they narrow as you type and disappear when focus moves elsewhere.
            if (activeField == ActiveField.Description && visibleSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                OptionChipRow(
                    options = visibleSuggestions,
                    optionLabel = { it },
                    onSelect = { suggestion ->
                        onDescriptionChange(suggestion)
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                )
            }
            // Add button to confirm the description without using the back arrow
            if (activeField == ActiveField.Description) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        },
                    ) {
                        Text("Add")
                    }
                }
            }
        }

        if (!editing) {
            Spacer(modifier = Modifier.height(16.dp))
            DateAndTimeButtons(
                createdAt = state.createdAt,
                createdZone = state.createdZone,
                onChange = onCreatedAtChange,
            )

            // Chips for whole entries likely to be repeated now; picking one fills
            // the form, keeping the current date and time.
            if (quickEntries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Quick entry",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OptionChipRow(
                    options = quickEntries,
                    optionLabel = ::quickEntryLabel,
                    onSelect = onQuickEntry,
                )
            }
        }
    }

    // The scrollable column above collapses while the calculator is open, so the
    // keypad replaces the whole form and gets the available space.
    if (activeField == ActiveField.Amount) {
        AmountCalculator(
            initialMinor = state.amount,
            currencySymbol = fromAccount?.currency?.symbol,
            onEnter = { value ->
                onAmountChange(value)
                activeField = null
            },
            onInvalid = {
                amountWarning = "Amount must be greater than zero"
                activeField = null
            },
            onCancel = { activeField = null },
        )
    }

    if (activeField == ActiveField.ToAmount) {
        AmountCalculator(
            initialMinor = state.toAmount,
            currencySymbol = toAccount?.currency?.symbol,
            onEnter = { value ->
                onToAmountChange(value)
                activeField = null
            },
            onInvalid = {
                toAmountWarning = "Amount must be greater than zero"
                activeField = null
            },
            onCancel = { activeField = null },
        )
    }
}
