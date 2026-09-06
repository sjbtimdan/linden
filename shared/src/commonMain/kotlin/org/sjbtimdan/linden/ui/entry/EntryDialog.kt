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
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_cancel
import org.sjbtimdan.linden.resources.common_delete
import org.sjbtimdan.linden.resources.common_save
import org.sjbtimdan.linden.resources.entry_title_edit
import org.sjbtimdan.linden.resources.entry_title_new
import org.sjbtimdan.linden.resources.entry_type_expense
import org.sjbtimdan.linden.resources.entry_type_income
import org.sjbtimdan.linden.resources.entry_type_transfer
import org.sjbtimdan.linden.ui.theme.DialogShape
import kotlin.time.Instant

/** Localized display name of an entry type, e.g. for segmented buttons and dialog titles. */
@Composable
fun EntryType.displayName(): String = stringResource(
    when (this) {
        EntryType.Expense -> Res.string.entry_type_expense
        EntryType.Income -> Res.string.entry_type_income
        EntryType.Transfer -> Res.string.entry_type_transfer
    },
)

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
            val typeName = state.type.displayName()
            Text(
                if (state.editing != null) {
                    stringResource(Res.string.entry_title_edit, typeName)
                } else {
                    stringResource(Res.string.entry_title_new, typeName)
                },
            )
        },
        text = {
            Column {
                // Explains why Save is disabled unless the form's own links
                // already point at the blocker (missing accounts or categories).
                missingRequirement(state, accounts, categories)?.let { requirement ->
                    MissingRequirementHint(message = requirement.text())
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
                Text(stringResource(Res.string.common_save))
            }
        },
        dismissButton = {
            Row {
                onDelete?.let { delete ->
                    TextButton(onClick = delete) {
                        Text(stringResource(Res.string.common_delete))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        },
    )
}
