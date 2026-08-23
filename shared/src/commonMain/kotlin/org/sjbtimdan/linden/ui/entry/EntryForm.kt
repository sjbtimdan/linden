package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.BackHandler
import kotlin.time.Instant

/**
 * The field section of the entry editor, shared between the inline form on the
 * ledger screen and the edit dialog on the history screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    descriptionSuggestions: List<String> = emptyList(),
    accountSuggestions: List<Long> = emptyList(),
    categorySuggestions: List<Long> = emptyList(),
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

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // While the calculator is open it replaces the amount field and the rest of
    // the form collapses, so the keypad gets the whole area above the keyboard.
    var calculatorMode by remember { mutableStateOf(false) }
    var amountWarning by remember { mutableStateOf<String?>(null) }

    // While a field with inline options (description, category, accounts) is focused
    // the rest of the form collapses so that field and its options get the whole
    // area above the keyboard. The focused field itself must stay mounted —
    // unmounting it would drop focus — so each section only hides while a
    // *different* one is active.
    var descriptionFocused by remember { mutableStateOf(false) }
    var focusedDropdown by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(descriptionFocused, focusedDropdown, calculatorMode) {
        onFieldFocusChange(descriptionFocused || focusedDropdown != null || calculatorMode)
    }
    val editing = descriptionFocused || focusedDropdown != null || calculatorMode

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    fun openCalculator() {
        calculatorMode = true
        amountWarning = null
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    BackHandler(enabled = focusedDropdown != null) {
        focusedDropdown = null
    }
    BackHandler(enabled = calculatorMode) {
        calculatorMode = false
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (!editing) {
            OutlinedTextField(
                value = state.amountText,
                onValueChange = {
                    onAmountChange(it)
                    amountWarning = null
                },
                label = { Text(if (state.type == EntryType.Transfer) "Amount (sent)" else "Amount") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amountWarning != null,
                supportingText = amountWarning?.let { warning ->
                    { Text(warning) }
                },
                trailingIcon = if (state.amountText.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { onAmountChange("") },
                        ) { Icon(Icons.Default.Close, contentDescription = "Clear") }
                    }
                } else {
                    null
                },
                suffix = fromAccount?.let { account ->
                    { Text(account.currency.symbol) }
                },
                modifier = Modifier
                    .onFocusChanged { if (it.isFocused) openCalculator() }
                    .testTag("amountField")
                    .fillMaxWidth(),
            )
        }

        if (state.type == EntryType.Transfer) {
            if (!editing || focusedDropdown == "from") {
                Spacer(modifier = Modifier.height(16.dp))
                if (accounts.isEmpty()) {
                    MissingFieldLink(
                        label = "From account",
                        text = "Please enter account",
                        onClick = onNavigateToSettings,
                    )
                } else {
                    DropdownField(
                        label = "From account",
                        selected = accounts.firstOrNull { it.id == state.accountId },
                        options = accounts,
                        optionLabel = { it.name },
                        onSelect = { onAccountChange(it.id) },
                        onFocusChange = { focusedDropdown = if (it) "from" else null },
                    )
                }
            }
            if (!editing || focusedDropdown == "to") {
                Spacer(modifier = Modifier.height(16.dp))
                if (accounts.size < 2) {
                    MissingFieldLink(
                        label = "To account",
                        text = if (accounts.isEmpty()) {
                            "Please enter account"
                        } else {
                            "Please add a second account"
                        },
                        onClick = onNavigateToSettings,
                    )
                } else {
                    DropdownField(
                        label = "To account",
                        selected = accounts.firstOrNull { it.id == state.toAccountId },
                        options = accounts.filter { it.id != state.accountId },
                        optionLabel = { it.name },
                        onSelect = { onToAccountChange(it.id) },
                        onFocusChange = { focusedDropdown = if (it) "to" else null },
                    )
                }
            }
            if (!editing && showReceivedAmount) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.toAmountText,
                    onValueChange = onToAmountChange,
                    label = { Text("Amount (received)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    trailingIcon = if (state.toAmountText.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { onToAmountChange("") },
                            ) { Icon(Icons.Default.Close, contentDescription = "Clear") }
                        }
                    } else {
                        null
                    },
                    suffix = toAccount.let { account ->
                        { Text(account.currency.symbol) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            if (!editing || focusedDropdown == "category") {
                Spacer(modifier = Modifier.height(16.dp))
                if (visibleCategories.isEmpty()) {
                    MissingFieldLink(
                        label = "Category",
                        text = "Please enter category",
                        onClick = onNavigateToSettings,
                    )
                } else {
                    DropdownField(
                        label = "Category",
                        selected = visibleCategories.firstOrNull { it.id == state.categoryId },
                        options = visibleCategories,
                        optionLabel = { it.name },
                        onSelect = { onCategoryChange(it.id) },
                        onFocusChange = { focusedDropdown = if (it) "category" else null },
                        predictedOptions = categorySuggestions
                            .filterNot { it == state.categoryId }
                            .mapNotNull { id -> visibleCategories.firstOrNull { it.id == id } },
                    )
                }
            }
            if (!editing || focusedDropdown == "account") {
                Spacer(modifier = Modifier.height(16.dp))
                if (accounts.isEmpty()) {
                    MissingFieldLink(
                        label = "Account",
                        text = "Please enter account",
                        onClick = onNavigateToSettings,
                    )
                } else {
                    DropdownField(
                        label = "Account",
                        selected = accounts.firstOrNull { it.id == state.accountId },
                        options = accounts,
                        optionLabel = { it.name },
                        onSelect = { onAccountChange(it.id) },
                        onFocusChange = { focusedDropdown = if (it) "account" else null },
                        predictedOptions = accountSuggestions
                            .filterNot { it == state.accountId }
                            .mapNotNull { id -> accounts.firstOrNull { it.id == id } },
                    )
                }
            }
        }

        if (!editing) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // While an account/category picker is expanded only that picker and its
        // chips show; the description would just be dead space beneath them.
        // Same while the amount calculator replaces the form.
        if (!calculatorMode && focusedDropdown == null) {
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
                    .onFocusChanged { descriptionFocused = it.isFocused }
                    .fillMaxWidth(),
            )
            // Chips live in the layout instead of a popup so the keyboard can never cover them;
            // they narrow as you type and disappear when focus moves elsewhere.
            if (descriptionFocused && visibleSuggestions.isNotEmpty()) {
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
        }

        if (!editing) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Date & time",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(formatDate(state.createdAt, state.createdZone))
                }
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text(formatTime(state.createdAt, state.createdZone))
                }
            }
        }
    }

    // The scrollable column above collapses while the calculator is open, so the
    // keypad replaces the whole form and gets the available space.
    if (calculatorMode) {
        AmountCalculator(
            initialMinor = state.amount,
            currencySymbol = fromAccount?.currency?.symbol,
            onEnter = { value ->
                onAmountChange(value)
                calculatorMode = false
            },
            onInvalid = {
                amountWarning = "Amount must be greater than zero"
                calculatorMode = false
            },
            onCancel = { calculatorMode = false },
        )
    }

    if (showDatePicker) {
        val local = state.createdAt.toLocalDateTime(state.createdZone)
        val initialUtcMillis = LocalDateTime(local.year, local.month.number, local.day, 0, 0)
            .toInstant(TimeZone.UTC)
            .toEpochMilliseconds()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val utcMillis = datePickerState.selectedDateMillis
                    if (utcMillis != null) {
                        onCreatedAtChange(
                            combineDateAndTime(utcMillis, local.hour, local.minute, state.createdZone),
                        )
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val local = state.createdAt.toLocalDateTime(state.createdZone)
        val timePickerState = rememberTimePickerState(
            initialHour = local.hour,
            initialMinute = local.minute,
            is24Hour = true,
        )
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val newInstant = LocalDateTime(
                        local.year,
                        local.month.number,
                        local.day,
                        timePickerState.hour,
                        timePickerState.minute,
                    ).toInstant(state.createdZone)
                    onCreatedAtChange(newInstant)
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            title = {
                Text("Select time")
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            TimePicker(state = timePickerState)
        }
    }
}
