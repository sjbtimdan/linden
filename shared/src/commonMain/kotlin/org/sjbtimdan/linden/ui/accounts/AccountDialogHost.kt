package org.sjbtimdan.linden.ui.accounts

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.accounts_duplicate_name
import org.sjbtimdan.linden.resources.common_invalid_amount
import org.sjbtimdan.linden.resources.common_name_required
import org.sjbtimdan.linden.ui.entry.parseAmount

/**
 * Hosts [AccountDialog] for [state]: validates the name and initial balance,
 * reports a duplicate through the state's name error, and closes the dialog on
 * a successful save. The hide toggle is delegated so a host can confirm a
 * non-zero account leaving the totals first.
 */
@Composable
fun AccountDialogHost(
    state: AccountDialogState?,
    isEditing: Boolean,
    canChangeCurrency: Boolean,
    canDelete: Boolean,
    onStateChange: (AccountDialogState?) -> Unit,
    onSave: (name: String, currency: Currency, initialBalance: Long, selectAsTo: Boolean) -> Boolean,
    onHiddenChange: ((AccountDialogState, Boolean) -> Unit)? = null,
    onDelete: (Account) -> Unit = {},
) {
    val dialogState = state ?: return
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val nameRequiredError = stringResource(Res.string.common_name_required)
    val duplicateNameError = stringResource(Res.string.accounts_duplicate_name)
    val invalidAmountError = stringResource(Res.string.common_invalid_amount)

    AccountDialog(
        name = dialogState.name,
        currency = dialogState.currency,
        initialBalanceText = dialogState.initialBalanceText,
        nameError = dialogState.nameError,
        initialBalanceError = dialogState.initialBalanceError,
        isEditing = isEditing,
        canChangeCurrency = canChangeCurrency,
        canDelete = canDelete,
        hidden = dialogState.hidden,
        onNameChange = { onStateChange(dialogState.copy(name = it, nameError = null)) },
        onCurrencyChange = { onStateChange(dialogState.copy(currency = it)) },
        onInitialBalanceChange = {
            onStateChange(dialogState.copy(initialBalanceText = it, initialBalanceError = null))
        },
        onHiddenChange = { hidden ->
            if (onHiddenChange != null) {
                onHiddenChange(dialogState, hidden)
            } else {
                onStateChange(dialogState.copy(hidden = hidden))
            }
        },
        onDelete = {
            dialogState.account?.let { existing ->
                onStateChange(null)
                onDelete(existing)
            }
        },
        onSave = {
            val name = dialogState.name.trim()
            if (name.isEmpty()) {
                onStateChange(dialogState.copy(nameError = nameRequiredError))
            } else {
                // Blank means zero; any other unparseable value is an error, not zero.
                val initialBalance = if (dialogState.initialBalanceText.isBlank()) {
                    0L
                } else {
                    parseAmount(dialogState.initialBalanceText, dialogState.currency.decimalDigits)
                }
                if (initialBalance == null) {
                    onStateChange(dialogState.copy(initialBalanceError = invalidAmountError))
                } else {
                    val saved = onSave(name, dialogState.currency, initialBalance, dialogState.selectAsTo)
                    if (saved) {
                        onStateChange(null)
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    } else {
                        onStateChange(dialogState.copy(nameError = duplicateNameError))
                    }
                }
            }
        },
        onDismiss = { onStateChange(null) },
    )
}
