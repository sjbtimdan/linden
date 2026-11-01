package org.sjbtimdan.linden.ui.ledger

import androidx.compose.runtime.Composable
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.ui.entry.EntryDialog
import org.sjbtimdan.linden.ui.entry.EntryDraft

/** Hosts the ledger's entry editor and Adjust Balance dialogs. */
@Composable
internal fun LedgerDialogHost(
    viewModel: LedgerViewModel,
    accounts: List<Account>,
    categories: List<Category>,
    dialogState: EntryDraft?,
    saving: Boolean,
    adjustState: AdjustBalanceDialogState?,
    hideAmounts: Boolean,
    onAdjustStateChange: (AdjustBalanceDialogState?) -> Unit,
) {
    dialogState?.let { state ->
        // Editing may open an entry that lives on a hidden account (its history is
        // kept), so the account pickers also offer exactly the hidden accounts the
        // draft references — switching onto any other hidden account stays impossible.
        val dialogAccounts = accounts.filter { account ->
            !account.hidden || account.id == state.accountId || account.id == state.toAccountId
        }
        EntryDialog(
            state = state,
            accounts = dialogAccounts,
            categories = categories,
            saving = saving,
            onAmountChange = viewModel::onAmountChange,
            onCategoryChange = viewModel::onCategoryChange,
            onAccountChange = viewModel::onAccountChange,
            onToAccountChange = viewModel::onToAccountChange,
            onToAmountChange = viewModel::onToAmountChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onCreatedAtChange = viewModel::onCreatedAtChange,
            onTypeChange = viewModel::changeDialogType,
            onSave = { viewModel.saveDialog() },
            onDelete = if (state.editing != null) viewModel::deleteDialogEntry else null,
            onDuplicate = viewModel::duplicateDialogEntry,
            onDismiss = viewModel::dismissDialog,
        )
    }

    adjustState?.let { state ->
        AdjustBalanceHost(
            viewModel = viewModel,
            state = state,
            hideAmounts = hideAmounts,
            onStateChange = onAdjustStateChange,
            onDismiss = { onAdjustStateChange(null) },
        )
    }
}
