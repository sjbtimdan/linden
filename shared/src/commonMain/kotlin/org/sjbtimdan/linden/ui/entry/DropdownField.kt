package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String,
    selected: T?,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val visibleOptions = query.trim().let { trimmed ->
        if (trimmed.isEmpty()) options
        else options.filter { optionLabel(it).contains(trimmed, ignoreCase = true) }
    }
    OutlinedTextField(
        // Derived, not synced: while unfocused the field always shows the current
        // selection (so external changes like a form reset can't leave stale text),
        // and focusing starts a fresh search.
        value = if (focused) query else selected?.let(optionLabel).orEmpty(),
        onValueChange = { query = it },
        label = { Text(label) },
        singleLine = true,
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = focused) },
        modifier = Modifier
            .onFocusChanged {
                focused = it.isFocused
                onFocusChange(it.isFocused)
                if (it.isFocused) query = ""
            }
            .fillMaxWidth(),
    )
    // Options render in-layout instead of a popup so the keyboard can never cover them.
    if (focused && visibleOptions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        DropdownOptionList(
            options = visibleOptions,
            optionLabel = optionLabel,
            isSelected = { it == selected },
            onSelect = { option ->
                onSelect(option)
                keyboardController?.hide()
                focusManager.clearFocus()
            },
        )
    }
}
