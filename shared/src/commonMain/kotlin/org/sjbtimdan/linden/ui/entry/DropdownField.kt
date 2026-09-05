package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp

/** How many predicted options are offered first (and highlighted) ahead of the full list. */
private const val PREDICTED_OPTION_LIMIT = 5

@Composable
fun <T> DropdownField(
    label: String,
    selected: T?,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
    predictedOptions: List<T>? = null,
    optionIcon: ((T) -> ImageVector?)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    // Predicted options lead, highlighted; the rest follow alphabetically so nothing is hidden.
    val predicted = predictedOptions.orEmpty()
        .distinct()
        .filter { it in options }
        .take(PREDICTED_OPTION_LIMIT)
    val orderedOptions = predicted + options
        .filterNot { it in predicted }
        .sortedBy { optionLabel(it).lowercase() }
    val visibleOptions = query.trim().let { trimmed ->
        if (trimmed.isEmpty()) {
            orderedOptions
        } else {
            orderedOptions.filter { optionLabel(it).contains(trimmed, ignoreCase = true) }
        }
    }
    OutlinedTextField(
        // Derived, not synced: while unfocused the field shows the current selection
        // (a form reset can't leave stale text); focusing starts a fresh search.
        value = if (focused) query else selected?.let(optionLabel).orEmpty(),
        onValueChange = { query = it },
        label = { Text(label) },
        singleLine = true,
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
        OptionChipRow(
            options = visibleOptions,
            optionLabel = optionLabel,
            isSelected = { it == selected },
            isPredicted = { it in predicted },
            optionIcon = optionIcon,
            onSelect = { option ->
                onSelect(option)
                keyboardController?.hide()
                focusManager.clearFocus()
            },
        )
    }
}
