package org.sjbtimdan.linden.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.accounts_currency
import org.sjbtimdan.linden.resources.accounts_currency_locked
import org.sjbtimdan.linden.resources.accounts_delete
import org.sjbtimdan.linden.resources.accounts_delete_locked
import org.sjbtimdan.linden.resources.accounts_edit
import org.sjbtimdan.linden.resources.accounts_hidden
import org.sjbtimdan.linden.resources.accounts_hidden_help
import org.sjbtimdan.linden.resources.accounts_initial_balance
import org.sjbtimdan.linden.resources.accounts_new
import org.sjbtimdan.linden.resources.common_cancel
import org.sjbtimdan.linden.resources.common_clear
import org.sjbtimdan.linden.resources.common_name
import org.sjbtimdan.linden.resources.common_save
import org.sjbtimdan.linden.ui.theme.DialogShape

data class AccountDialogState(
    val account: Account?,
    val name: String,
    val currency: Currency,
    val initialBalanceText: String,
    val nameError: String? = null,
    val initialBalanceError: String? = null,
    val hidden: Boolean = false,
    /** Create-only: select the new account as the transfer destination. */
    val selectAsTo: Boolean = false,
)

@Composable
fun AccountDialog(
    name: String,
    currency: Currency,
    initialBalanceText: String,
    nameError: String?,
    initialBalanceError: String?,
    isEditing: Boolean,
    canChangeCurrency: Boolean,
    canDelete: Boolean,
    hidden: Boolean,
    onNameChange: (String) -> Unit,
    onCurrencyChange: (Currency) -> Unit,
    onInitialBalanceChange: (String) -> Unit,
    onHiddenChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        title = {
            Text(
                if (isEditing) {
                    stringResource(Res.string.accounts_edit)
                } else {
                    stringResource(Res.string.accounts_new)
                },
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(Res.string.common_name)) },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { error -> { Text(error) } },
                    trailingIcon = if (name.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { onNameChange("") },
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(Res.string.common_clear),
                                )
                            }
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
                    label = { Text(stringResource(Res.string.accounts_initial_balance)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = initialBalanceError != null,
                    supportingText = initialBalanceError?.let { error -> { Text(error) } },
                    trailingIcon = if (initialBalanceText.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { onInitialBalanceChange("") },
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(Res.string.common_clear),
                                )
                            }
                        }
                    } else {
                        null
                    },
                    suffix = { Text(currency.symbol) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.accounts_currency),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (!canChangeCurrency) {
                    Text(
                        text = stringResource(Res.string.accounts_currency_locked),
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
                if (isEditing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.accounts_hidden),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = stringResource(Res.string.accounts_hidden_help),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = hidden,
                            onCheckedChange = onHiddenChange,
                            modifier = Modifier.testTag("hiddenSwitch"),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!canDelete) {
                        Text(
                            text = stringResource(Res.string.accounts_delete_locked),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        enabled = canDelete,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(Res.string.accounts_delete))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text(stringResource(Res.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        },
    )
}
