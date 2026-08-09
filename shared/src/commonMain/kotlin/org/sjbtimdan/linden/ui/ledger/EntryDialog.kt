package org.sjbtimdan.linden.ui.ledger

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
) {
    val amount: Long? get() = parseAmount(amountText)
    val toAmount: Long? get() = parseAmount(toAmountText)

    // TODO: Move to a helper that can be tested
    fun isValid(): Boolean {
        val amountValue = amount ?: return false
        if (amountValue <= 0) return false
        if (accountId == null) return false
        return when (type) {
            EntryType.Expense, EntryType.Income -> categoryId != null
            EntryType.Transfer -> {
                val toValue = toAmount
                toValue != null && toValue > 0 && toAccountId != null && toAccountId != accountId
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
                ExpenseEntry(id, category, description, account, amountValue, account.currency)
            }

            EntryType.Income -> {
                val category = categories.firstOrNull { it.id == categoryId } ?: return null
                IncomeEntry(id, category, description, account, amountValue, account.currency)
            }

            EntryType.Transfer -> {
                val toAccount = accounts.firstOrNull { it.id == toAccountId } ?: return null
                val toValue = toAmount ?: return null
                TransferEntry(
                    id = id,
                    category = null,
                    description = description,
                    account = account,
                    amount = amountValue,
                    currency = account.currency,
                    toAccount = toAccount,
                    toAmount = toValue,
                    toCurrency = toAccount.currency,
                )
            }
        }
    }

    companion object {
        fun forNew(): EntryDialogState = EntryDialogState(
            editing = null,
            type = EntryType.Expense,
            amountText = "",
            categoryId = null,
            accountId = null,
            toAccountId = null,
            toAmountText = "",
            description = "",
        )

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
            )

            is TransferEntry -> EntryDialogState(
                editing = entry,
                type = EntryType.Transfer,
                amountText = formatAmount(entry.amount),
                categoryId = null,
                accountId = entry.account.id,
                toAccountId = entry.toAccount.id,
                toAmountText = formatAmount(entry.toAmount),
                description = entry.description.orEmpty(),
            )
        }
    }
}

@Composable
fun EntryDialog(
    state: EntryDialogState,
    accounts: List<Account>,
    categories: List<Category>,
    onTypeChange: (EntryType) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onAccountChange: (Long?) -> Unit,
    onToAccountChange: (Long?) -> Unit,
    onToAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    onNavigateToSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val visibleCategories = when (state.type) {
        EntryType.Expense -> categories.filter { it.type != CategoryType.Income }
        EntryType.Income -> categories.filter { it.type != CategoryType.Expense }
        EntryType.Transfer -> emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (state.editing != null) "Edit Entry" else "New Entry")
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Type",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    EntryType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = state.type == type,
                            onClick = { onTypeChange(type) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = EntryType.entries.size,
                            ),
                        ) {
                            Text(type.displayName())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = onAmountChange,
                    label = { Text(if (state.type == EntryType.Transfer) "Amount (sent)" else "Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.toAmountText,
                        onValueChange = onToAmountChange,
                        label = { Text("Amount (received)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
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

                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = state.isValid(),
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
}
