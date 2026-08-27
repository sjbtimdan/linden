package org.sjbtimdan.linden.ui.entry

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/** A labeled dropdown, or a [MissingFieldLink] when the field cannot be satisfied yet. */
@Composable
fun <T> FieldDropdown(
    label: String,
    selected: T?,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    missing: String? = null,
    onNavigateToSettings: () -> Unit = {},
    predicted: List<T> = emptyList(),
    optionIcon: ((T) -> ImageVector?)? = null,
) {
    if (missing != null) {
        MissingFieldLink(label = label, text = missing, onClick = onNavigateToSettings)
    } else {
        DropdownField(
            label = label,
            selected = selected,
            options = options,
            optionLabel = optionLabel,
            onSelect = onSelect,
            onFocusChange = onFocusChange,
            predictedOptions = predicted,
            optionIcon = optionIcon,
        )
    }
}
