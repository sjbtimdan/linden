package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType

/** Amount input that opens the calculator on focus. */
@Composable
fun AmountField(
    value: String,
    label: String,
    suffix: String?,
    warning: String?,
    onValueChange: (String) -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = warning != null,
        supportingText = warning?.let { warning -> { Text(warning) } },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(
                    onClick = { onValueChange("") },
                ) { Icon(Icons.Default.Close, contentDescription = "Clear") }
            }
        } else {
            null
        },
        suffix = suffix?.let { symbol -> { Text(symbol) } },
        modifier = modifier
            .onFocusChanged { if (it.isFocused) onFocus() }
            .fillMaxWidth(),
    )
}
