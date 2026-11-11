package org.sjbtimdan.linden.ui.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import org.sjbtimdan.linden.model.Account

/** The amount calculators, shown while their field is expanded. */
@Composable
internal fun EntryAmountCalculators(
    activeField: ActiveField?,
    state: EntryDraft,
    fromAccount: Account?,
    toAccount: Account?,
    amountLabel: String,
    receivedAmountLabel: String,
    amountPositiveWarning: String,
    onAmountChange: (String) -> Unit,
    onToAmountChange: (String) -> Unit,
    onAmountWarning: (String) -> Unit,
    onToAmountWarning: (String) -> Unit,
    onCancel: () -> Unit,
) {
    AnimatedVisibility(
        visible = activeField == ActiveField.Amount,
        enter = slideInVertically(tween(220)) { it / 4 } + fadeIn(tween(220)),
        exit = fadeOut(tween(120)),
    ) {
        EntryAmountCalculator(
            isToAmount = false,
            state = state,
            fromAccount = fromAccount,
            toAccount = toAccount,
            purpose = amountLabel,
            warning = amountPositiveWarning,
            onCommit = onAmountChange,
            onWarning = onAmountWarning,
            onCancel = onCancel,
        )
    }

    AnimatedVisibility(
        visible = activeField == ActiveField.ToAmount,
        enter = slideInVertically(tween(220)) { it / 4 } + fadeIn(tween(220)),
        exit = fadeOut(tween(120)),
    ) {
        EntryAmountCalculator(
            isToAmount = true,
            state = state,
            fromAccount = fromAccount,
            toAccount = toAccount,
            purpose = receivedAmountLabel,
            warning = amountPositiveWarning,
            onCommit = onToAmountChange,
            onWarning = onToAmountWarning,
            onCancel = onCancel,
        )
    }
}
