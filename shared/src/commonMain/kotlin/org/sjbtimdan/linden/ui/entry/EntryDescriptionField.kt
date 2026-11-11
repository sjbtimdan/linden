package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.entry_description_optional
import org.sjbtimdan.linden.resources.entry_ok
import org.sjbtimdan.linden.ui.ClearIconButton

/**
 * The optional description field. While it is expanded it shows suggestions
 * from past entries and an Ok button that closes the field.
 */
@Composable
internal fun EntryDescriptionField(
    value: String,
    suggestions: List<String>,
    expanded: Boolean,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val visibleSuggestions = remember(value, suggestions) {
        value.trim().let { query ->
            val matches = if (query.isEmpty()) {
                suggestions
            } else {
                suggestions.filter { it.contains(query, ignoreCase = true) }
            }
            matches.filterNot { it.equals(query, ignoreCase = true) }
        }
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(Res.string.entry_description_optional)) },
        singleLine = true,
        trailingIcon = if (value.isNotEmpty()) {
            { ClearIconButton(onClear = { onValueChange("") }) }
        } else {
            null
        },
        modifier = Modifier
            .onFocusChanged { onFocusChange(it.isFocused) }
            .fillMaxWidth(),
    )
    if (expanded && visibleSuggestions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        OptionChipRow(
            options = visibleSuggestions,
            optionLabel = { it },
            onSelect = { suggestion ->
                onValueChange(suggestion)
                keyboardController?.hide()
                focusManager.clearFocus()
            },
        )
    }
    // Ok button to confirm the description without using the back arrow
    if (expanded) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
            ) {
                Text(stringResource(Res.string.entry_ok))
            }
        }
    }
}
