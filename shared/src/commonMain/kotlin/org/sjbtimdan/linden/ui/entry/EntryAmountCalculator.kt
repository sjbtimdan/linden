package org.sjbtimdan.linden.ui.entry

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.sjbtimdan.linden.model.Account

/** One-line context for the keypad takeover: the field's purpose plus the accounts involved. */
private fun calculatorContextLabel(purpose: String, from: Account?, to: Account?): String = when {
    from != null && to != null ->
        "$purpose · ${from.name} · ${from.currency.symbol} → ${to.name} · ${to.currency.symbol}"

    from != null -> "$purpose · ${from.name} · ${from.currency.symbol}"

    to != null -> "$purpose · ${to.name} · ${to.currency.symbol}"

    else -> purpose
}

/**
 * The calculator for one amount field of the entry form: the sent amount or
 * the received amount of a cross-currency transfer. The host owns which field
 * is open and turns a commit or invalid result back into form state.
 */
@Composable
internal fun EntryAmountCalculator(
    isToAmount: Boolean,
    state: EntryDraft,
    accounts: List<Account>,
    fromAccount: Account?,
    toAccount: Account?,
    purpose: String,
    warning: String,
    onCommit: (String) -> Unit,
    onWarning: (String) -> Unit,
    onCancel: () -> Unit,
    onAdd: (() -> Unit)?,
    addLabel: StringResource,
) {
    AmountCalculator(
        initialMinor = if (isToAmount) state.toAmount else state.amount,
        currencySymbol = if (isToAmount) toAccount?.currency?.symbol else fromAccount?.currency?.symbol,
        decimalDigits = if (isToAmount) {
            toAccount?.currency?.decimalDigits ?: 2
        } else {
            fromAccount?.currency?.decimalDigits ?: 2
        },
        contextLabel = calculatorContextLabel(purpose, fromAccount, toAccount),
        onEnter = { value ->
            onCommit(value)
            onCancel()
        },
        onInvalid = {
            onWarning(warning)
            onCancel()
        },
        onCancel = onCancel,
        onAdd = onAdd,
        addEnabled = { value ->
            val updated = if (isToAmount) state.copy(toAmountText = value) else state.copy(amountText = value)
            updated.isValid(accounts)
        },
        addLabel = addLabel,
    )
}
