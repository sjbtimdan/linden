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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry

fun EntryType.displayName(): String = when (this) {
    EntryType.Expense -> "Expense"
    EntryType.Income -> "Income"
    EntryType.Transfer -> "Transfer"
}

data class EntryDialogState(
    val editing: Entry?,
    val type: EntryType,
    val amountText: String,
    val categoryId: Long?,
    val accountId: Long?,
    val toAccountId: Long?,
    val toAmountText: String,
    val description: String,
    val createdAt: Instant,
    val createdZone: TimeZone,
) {
    val amount: Long? get() = parseAmount(amountText)
    val toAmount: Long? get() = parseAmount(toAmountText)

    // TODO: Move to a helper that can be tested
    fun isValid(accounts: List<Account>): Boolean {
        val amountValue = amount ?: return false
        if (amountValue <= 0) return false
        if (accountId == null) return false
        return when (type) {
            EntryType.Expense, EntryType.Income -> categoryId != null
            EntryType.Transfer -> {
                val toAccountIdValue = toAccountId ?: return false
                if (toAccountIdValue == accountId) return false
                val toAccount = accounts.firstOrNull { it.id == toAccountIdValue } ?: return false
                val account = accounts.firstOrNull { it.id == accountId } ?: return false
                if (account.currency == toAccount.currency) {
                    true
                } else {
                    val toValue = toAmount
                    toValue != null && toValue > 0
                }
            }
        }
    }

    // TODO: Move to a helper that can be tested
    fun toEntry(accounts: List<Account>, categories: List<Category>): Entry? {
        val amountValue = amount ?: return null
        val account = accounts.firstOrNull { it.id == accountId } ?: return null
        val id = editing?.id ?: 0L
        val description = description.trim().ifEmpty { null }
        return when (type) {
            EntryType.Expense -> {
                val category = categories.firstOrNull { it.id == categoryId } ?: return null
                ExpenseEntry(
                    id, category, description, account, amountValue,
                    createdAt = createdAt, createdZone = createdZone,
                )
            }

            EntryType.Income -> {
                val category = categories.firstOrNull { it.id == categoryId } ?: return null
                IncomeEntry(
                    id, category, description, account, amountValue,
                    createdAt = createdAt, createdZone = createdZone,
                )
            }

            EntryType.Transfer -> {
                val toAccount = accounts.firstOrNull { it.id == toAccountId } ?: return null
                val sameCurrency = account.currency == toAccount.currency
                val toValue = if (sameCurrency) null else toAmount ?: return null
                TransferEntry(
                    id = id,
                    category = null,
                    description = description,
                    account = account,
                    amount = amountValue,
                    createdAt = createdAt,
                    createdZone = createdZone,
                    toAccount = toAccount,
                    toAmount = toValue,
                )
            }
        }
    }

    companion object {
        fun forNew(type: EntryType = EntryType.Expense, previous: Entry? = null): EntryDialogState {
            val empty = EntryDialogState(
                editing = null,
                type = type,
                amountText = "",
                categoryId = null,
                accountId = null,
                toAccountId = null,
                toAmountText = "",
                description = "",
                createdAt = Clock.System.now(),
                createdZone = TimeZone.currentSystemDefault(),
            )
            if (previous == null || previous.type != type) return empty
            return when (previous) {
                is ExpenseEntry -> empty.copy(
                    categoryId = previous.category.id,
                    accountId = previous.account.id,
                    description = previous.description.orEmpty(),
                )

                is IncomeEntry -> empty.copy(
                    categoryId = previous.category.id,
                    accountId = previous.account.id,
                    description = previous.description.orEmpty(),
                )

                is TransferEntry -> empty.copy(
                    accountId = previous.account.id,
                    toAccountId = previous.toAccount.id,
                    description = previous.description.orEmpty(),
                )
            }
        }

        fun forEdit(entry: Entry): EntryDialogState = when (entry) {
            is ExpenseEntry -> EntryDialogState(
                editing = entry,
                type = EntryType.Expense,
                amountText = formatAmount(entry.amount),
                categoryId = entry.category.id,
                accountId = entry.account.id,
                toAccountId = null,
                toAmountText = "",
                description = entry.description.orEmpty(),
                createdAt = entry.createdAt,
                createdZone = entry.createdZone,
            )

            is IncomeEntry -> EntryDialogState(
                editing = entry,
                type = EntryType.Income,
                amountText = formatAmount(entry.amount),
                categoryId = entry.category.id,
                accountId = entry.account.id,
                toAccountId = null,
                toAmountText = "",
                description = entry.description.orEmpty(),
                createdAt = entry.createdAt,
                createdZone = entry.createdZone,
            )

            is TransferEntry -> EntryDialogState(
                editing = entry,
                type = EntryType.Transfer,
                amountText = formatAmount(entry.amount),
                categoryId = null,
                accountId = entry.account.id,
                toAccountId = entry.toAccount.id,
                toAmountText = formatAmount(entry.toAmount ?: entry.amount),
                description = entry.description.orEmpty(),
                createdAt = entry.createdAt,
                createdZone = entry.createdZone,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDialog(
    state: EntryDialogState,
    accounts: List<Account>,
    categories: List<Category>,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onAccountChange: (Long?) -> Unit,
    onToAccountChange: (Long?) -> Unit,
    onToAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCreatedAtChange: (Instant) -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    onNavigateToSettings: () -> Unit,
    onDismiss: () -> Unit,
    descriptionSuggestions: List<String> = emptyList(),
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

    AlertDialog(
        onDismissRequest = onDismiss,
        // Only Cancel (or the Android back button) closes the dialog; stray taps on
        // the scrim must not discard an in-progress entry.
        properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Text(
                if (state.editing != null) {
                    "Edit ${state.type.displayName()}"
                } else {
                    "New ${state.type.displayName()}"
                },
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = onAmountChange,
                    label = { Text(if (state.type == EntryType.Transfer) "Amount (sent)" else "Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = fromAccount?.let { account ->
                        { Text(account.currency.symbol) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.type == EntryType.Transfer) {
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
                        )
                    }
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
                        )
                    }
                    if (showReceivedAmount) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = state.toAmountText,
                            onValueChange = onToAmountChange,
                            label = { Text("Amount (received)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            suffix = toAccount?.let { account ->
                                { Text(account.currency.symbol) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
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
                        )
                    }
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
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                var descriptionExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = descriptionExpanded && descriptionSuggestions.isNotEmpty(),
                    onExpandedChange = { descriptionExpanded = it },
                ) {
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = onDescriptionChange,
                        label = { Text("Description (optional)") },
                        singleLine = true,
                        trailingIcon = {
                            if (descriptionSuggestions.isNotEmpty()) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = descriptionExpanded)
                            }
                        },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                            .onFocusChanged { if (it.isFocused) descriptionExpanded = true }
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = descriptionExpanded && descriptionSuggestions.isNotEmpty(),
                        onDismissRequest = { descriptionExpanded = false },
                    ) {
                        descriptionSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    onDescriptionChange(suggestion)
                                    descriptionExpanded = false
                                },
                            )
                        }
                    }
                }

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
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = state.isValid(accounts),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                onDelete?.let { delete ->
                    TextButton(onClick = delete) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    )

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
                        local.year, local.month.number, local.day,
                        timePickerState.hour, timePickerState.minute,
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
