package org.sjbtimdan.linden.ui.entry

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/** A labeled dropdown with an optional inline "+ New" create chip. */
@Composable
fun <T> FieldDropdown(
    label: String,
    selected: T?,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    predicted: List<T> = emptyList(),
    optionIcon: ((T) -> ImageVector?)? = null,
    createLabel: ((String) -> String)? = null,
    onCreate: ((String) -> Unit)? = null,
) {
    DropdownField(
        label = label,
        selected = selected,
        options = options,
        optionLabel = optionLabel,
        onSelect = onSelect,
        onFocusChange = onFocusChange,
        predictedOptions = predicted,
        optionIcon = optionIcon,
        createLabel = createLabel,
        onCreate = onCreate,
    )
}