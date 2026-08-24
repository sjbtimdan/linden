package org.sjbtimdan.linden.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.entry.formatAmount
import org.sjbtimdan.linden.ui.entry.formatAmountCompact
import org.sjbtimdan.linden.ui.entry.parseAmount

private data class AccountDialogState(
    val account: Account?,
    val name: String,
    val currency: Currency,
    val initialBalanceText: String,
)

@Composable
fun AccountListScreen(viewModel: AccountListViewModel, onNavigateBack: () -> Unit) {
    val accounts by viewModel.accounts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val accountsWithEntries by viewModel.accountsWithEntries.collectAsState()
    var dialogState by remember { mutableStateOf<AccountDialogState?>(null) }

    BackHandler(enabled = dialogState != null) {
        dialogState = null
    }

    Column(
        modifier = Modifier
            .safeContentPadding()
            .fillMaxSize()
            .padding(16.dp)
            .widthIn(max = 480.dp),
    ) {
        TextButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("< Settings")
        }

        Spacer(modifier = Modifier.height(16.dp))

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
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(
                        onClick = { viewModel.setSearchQuery("") },
                    ) { Icon(Icons.Default.Close, contentDescription = "Clear") }
                }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        FilledTonalButton(
            onClick = {
                dialogState = AccountDialogState(null, "", Currency.CHF, "")
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("+ New Account")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "No accounts yet." else "No matching accounts.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(accounts, key = { it.id }) { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(role = Role.Button) {
                                dialogState = AccountDialogState(
                                    account = account,
                                    name = account.name,
                                    currency = account.currency,
                                    initialBalanceText = formatAmount(account.initialBalance),
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = account.name.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = account.currency.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${formatAmountCompact(account.initialBalance)} ${account.currency.symbol}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Initial",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    dialogState?.let { state ->
        val isEditing = state.account != null
        AccountDialog(
            name = state.name,
            currency = state.currency,
            initialBalanceText = state.initialBalanceText,
            isEditing = isEditing,
            canChangeCurrency = !isEditing || state.account.id !in accountsWithEntries,
            onNameChange = { dialogState = state.copy(name = it) },
            onCurrencyChange = { dialogState = state.copy(currency = it) },
            onInitialBalanceChange = { dialogState = state.copy(initialBalanceText = it) },
            onSave = {
                val name = state.name.trim()
                if (name.isNotEmpty()) {
                    val initialBalance = parseAmount(state.initialBalanceText) ?: 0
                    val existing = state.account
                    if (existing != null) {
                        viewModel.updateAccount(
                            existing.copy(
                                name = name,
                                currency = state.currency,
                                initialBalance = initialBalance,
                            ),
                        )
                    } else {
                        viewModel.createAccount(name, state.currency, initialBalance)
                    }
                    dialogState = null
                }
            },
            onDismiss = { dialogState = null },
        )
    }
}

@Composable
private fun AccountDialog(
    name: String,
    currency: Currency,
    initialBalanceText: String,
    isEditing: Boolean,
    canChangeCurrency: Boolean,
    onNameChange: (String) -> Unit,
    onCurrencyChange: (Currency) -> Unit,
    onInitialBalanceChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Edit Account" else "New Account")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    trailingIcon = if (name.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { onNameChange("") },
                            ) { Icon(Icons.Default.Close, contentDescription = "Clear") }
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = initialBalanceText,
                    onValueChange = onInitialBalanceChange,
                    label = { Text("Initial balance") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    trailingIcon = if (initialBalanceText.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { onInitialBalanceChange("") },
                            ) { Icon(Icons.Default.Close, contentDescription = "Clear") }
                        }
                    } else {
                        null
                    },
                    suffix = { Text(currency.symbol) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Currency",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (!canChangeCurrency) {
                    Text(
                        text = "Currency cannot be changed: this account has entries.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Currency.entries.forEach { entry ->
                        FilterChip(
                            selected = currency == entry,
                            enabled = canChangeCurrency,
                            onClick = { onCurrencyChange(entry) },
                            label = { Text(entry.name) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
