package org.sjbtimdan.linden.ui.rates

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_cancel
import org.sjbtimdan.linden.resources.common_save
import org.sjbtimdan.linden.resources.rates_edit_rate
import org.sjbtimdan.linden.resources.rates_positive_number
import org.sjbtimdan.linden.resources.rates_rate_label
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
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(Res.string.rates_rate_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = text.isNotEmpty() && parsed == null,
                supportingText = if (text.isNotEmpty() && parsed == null) {
                    { Text(stringResource(Res.string.rates_positive_number)) }
                } else {
                    null
                },
                suffix = { Text(quoteCurrency.symbol) },
                modifier = Modifier.fillMaxWidth(),
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
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.common_cancel))
            }
        },
    )
}
