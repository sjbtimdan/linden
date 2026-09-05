package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.theme.DialogShape
import kotlin.time.Instant

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
    accountSuggestions: List<Long> = emptyList(),
    categorySuggestions: List<Long> = emptyList(),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        // Only Cancel or the system back closes the dialog; scrim taps must not
        // discard an in-progress entry.
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
            Column {
                // Explains why Save is disabled unless the form's own links
                // already point at the blocker (missing accounts or categories).
                missingRequirementHint(state, accounts, categories)?.let { hint ->
                    MissingRequirementHint(message = hint)
                    Spacer(modifier = Modifier.height(8.dp))
                }
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
                    accountSuggestions = accountSuggestions,
                    categorySuggestions = categorySuggestions,
                )
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
}
