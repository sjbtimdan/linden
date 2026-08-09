package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry

private data class EntryDialogState(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDialog(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownField(
    label: String,
    selected: T?,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected?.let(optionLabel).orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun MissingFieldLink(
    label: String,
    text: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(role = Role.Button, onClick = onClick)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.extraSmall,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
            ),
        )
    }
}

@Composable
private fun SortDropdown(
    current: SortOrder,
    onChange: (SortOrder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text("Sort: ${current.label()}")
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.label()) },
                    onClick = {
                        onChange(order)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: Entry,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title(),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = entry.subtitle(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = entry.amountLabel(),
            style = MaterialTheme.typography.bodyLarge,
            color = entry.amountColor(),
        )
    }
    HorizontalDivider()
}

private fun Entry.title(): String {
    val description = description?.takeIf { it.isNotBlank() }
    return when (this) {
        is TransferEntry -> description ?: "Transfer"
        else -> description ?: requireNotNull(category).name
    }
}

private fun Entry.subtitle(): String = when (this) {
    is TransferEntry -> "${account.name} → ${toAccount.name}"
    else -> account.name
}

private fun Entry.amountLabel(): String = when (type) {
    EntryType.Expense -> "− ${formatAmount(amount)}"
    EntryType.Income -> "+ ${formatAmount(amount)}"
    EntryType.Transfer -> formatAmount(amount)
}

@Composable
private fun Entry.amountColor(): Color = when (type) {
    EntryType.Expense -> MaterialTheme.colorScheme.error
    EntryType.Income -> Color(0xFF43A047)
    EntryType.Transfer -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun EntryType.displayName(): String = when (this) {
    EntryType.Expense -> "Expense"
    EntryType.Income -> "Income"
    EntryType.Transfer -> "Transfer"
}

private fun SortOrder.label(): String = when (this) {
    SortOrder.NewestFirst -> "Newest first"
    SortOrder.OldestFirst -> "Oldest first"
    SortOrder.AmountHighToLow -> "Amount high to low"
    SortOrder.AmountLowToHigh -> "Amount low to high"
}
