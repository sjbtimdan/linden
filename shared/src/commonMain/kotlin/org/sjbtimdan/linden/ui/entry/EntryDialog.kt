package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import kotlin.time.Instant
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.EntryType

fun EntryType.displayName(): String = when (this) {
    EntryType.Expense -> "Expense"
    EntryType.Income -> "Income"
    EntryType.Transfer -> "Transfer"
}

@Composable
fun EntryDialog(
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
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    onNavigateToSettings: () -> Unit,
    onDismiss: () -> Unit,
    descriptionSuggestions: List<String> = emptyList(),
) {
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
            EntryForm(
                state = state,
                accounts = accounts,
                categories = categories,
                onAmountChange = onAmountChange,
                onCategoryChange = onCategoryChange,
                onAccountChange = onAccountChange,
                onToAccountChange = onToAccountChange,
                onToAmountChange = onToAmountChange,
                onDescriptionChange = onDescriptionChange,
                onCreatedAtChange = onCreatedAtChange,
                onNavigateToSettings = onNavigateToSettings,
                descriptionSuggestions = descriptionSuggestions,
            )
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
}
