package org.sjbtimdan.linden.ui.rates

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_save
import org.sjbtimdan.linden.resources.rates_edit_rate
import org.sjbtimdan.linden.resources.rates_positive_number
import org.sjbtimdan.linden.resources.rates_rate_label
import org.sjbtimdan.linden.ui.DialogCancelButton
import org.sjbtimdan.linden.ui.entry.AmountField
import org.sjbtimdan.linden.ui.theme.DialogShape

@Composable
fun RateEditorDialog(quoteCurrency: Currency, currentRate: Double?, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by remember(currentRate) { mutableStateOf(currentRate?.let(::formatRate) ?: "") }
    val parsed = parseRate(text)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        title = { Text(stringResource(Res.string.rates_edit_rate, quoteCurrency.name)) },
        text = {
            val warning = if (text.isNotEmpty() && parsed == null) {
                stringResource(Res.string.rates_positive_number)
            } else {
                null
            }
            AmountField(
                value = text,
                label = stringResource(Res.string.rates_rate_label),
                suffix = quoteCurrency.symbol,
                warning = warning,
                onValueChange = { text = it },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onSave) },
                enabled = parsed != null,
            ) {
                Text(stringResource(Res.string.common_save))
            }
        },
        dismissButton = { DialogCancelButton(onDismiss) },
    )
}
