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
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.theme.DialogShape

@Composable
fun RateEditorDialog(quoteCurrency: Currency, currentRate: Double?, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by remember(currentRate) { mutableStateOf(currentRate?.let(::formatRate) ?: "") }
    val parsed = parseRate(text)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = DialogShape,
        title = { Text("Edit ${quoteCurrency.name} rate") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Rate") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = text.isNotEmpty() && parsed == null,
                supportingText = if (text.isNotEmpty() && parsed == null) {
                    { Text("Enter a positive number.") }
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
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
