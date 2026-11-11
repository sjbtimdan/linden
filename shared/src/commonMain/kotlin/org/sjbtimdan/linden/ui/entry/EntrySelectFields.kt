package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_category
import org.sjbtimdan.linden.resources.entry_account
import org.sjbtimdan.linden.resources.entry_amount_received
import org.sjbtimdan.linden.resources.entry_from_account
import org.sjbtimdan.linden.resources.entry_to_account

/**
 * The account and category pickers: source and destination for a transfer,
 * category and account for everything else. While a field is expanded the rest
 * collapse so it and its options get the whole area above the keyboard.
 */
@Composable
internal fun EntrySelectFields(
    ui: EntryFormUi,
    state: EntryDraft,
    activeField: ActiveField?,
    toAmountWarning: String?,
    accountCreateLabel: ((String) -> String)?,
    categoryCreateLabel: ((String) -> String)?,
    onAccountChange: (Long?) -> Unit,
    onToAccountChange: (Long?) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onToAmountChange: (String) -> Unit,
    onActiveFieldChange: (ActiveField?) -> Unit,
    onOpenCreateAccount: (String, Boolean) -> Unit,
    onOpenCreateCategory: (String) -> Unit,
    onToAmountWarningClear: () -> Unit,
) {
    val editing = activeField != null

    if (state.type == EntryType.Transfer) {
        if (!editing || activeField == ActiveField.From) {
            Spacer(modifier = Modifier.height(16.dp))
            DropdownField(
                label = stringResource(Res.string.entry_from_account),
                selected = ui.fromAccount,
                options = ui.accounts,
                optionLabel = { it.name },
                onSelect = { onAccountChange(it.id) },
                onFocusChange = { onActiveFieldChange(if (it) ActiveField.From else null) },
                createLabel = accountCreateLabel,
                onCreate = if (accountCreateLabel != null) {
                    { query -> onOpenCreateAccount(query, false) }
                } else {
                    null
                },
            )
        }
        if (!editing || activeField == ActiveField.To) {
            Spacer(modifier = Modifier.height(16.dp))
            DropdownField(
                label = stringResource(Res.string.entry_to_account),
                selected = ui.toAccount,
                options = ui.toAccountOptions,
                optionLabel = { it.name },
                onSelect = { onToAccountChange(it.id) },
                onFocusChange = { onActiveFieldChange(if (it) ActiveField.To else null) },
                createLabel = accountCreateLabel,
                onCreate = if (accountCreateLabel != null) {
                    { query -> onOpenCreateAccount(query, true) }
                } else {
                    null
                },
            )
        }
        if (!editing && ui.showReceivedAmount) {
            Spacer(modifier = Modifier.height(16.dp))
            AmountField(
                value = state.toAmountText,
                label = stringResource(Res.string.entry_amount_received),
                suffix = ui.toAccount?.currency?.symbol,
                warning = toAmountWarning,
                onValueChange = {
                    onToAmountChange(it)
                    onToAmountWarningClear()
                },
                onFocus = { onActiveFieldChange(ActiveField.ToAmount) },
            )
        }
    } else {
        if (!editing || activeField == ActiveField.Category) {
            Spacer(modifier = Modifier.height(16.dp))
            DropdownField(
                label = stringResource(Res.string.common_category),
                selected = ui.selectedCategory,
                options = ui.visibleCategories,
                optionLabel = { it.name },
                onSelect = { onCategoryChange(it.id) },
                onFocusChange = { onActiveFieldChange(if (it) ActiveField.Category else null) },
                predictedOptions = ui.predictedCategoryOptions,
                optionIcon = { it.icon?.imageVector() },
                createLabel = categoryCreateLabel,
                onCreate = if (categoryCreateLabel != null) {
                    { query -> onOpenCreateCategory(query) }
                } else {
                    null
                },
            )
        }
        if (!editing || activeField == ActiveField.Account) {
            Spacer(modifier = Modifier.height(16.dp))
            DropdownField(
                label = stringResource(Res.string.entry_account),
                selected = ui.fromAccount,
                options = ui.accounts,
                optionLabel = { it.name },
                onSelect = { onAccountChange(it.id) },
                onFocusChange = { onActiveFieldChange(if (it) ActiveField.Account else null) },
                predictedOptions = ui.predictedAccountOptions,
                createLabel = accountCreateLabel,
                onCreate = if (accountCreateLabel != null) {
                    { query -> onOpenCreateAccount(query, false) }
                } else {
                    null
                },
            )
        }
    }
}
