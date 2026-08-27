package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun <T> OptionChipRow(
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: (T) -> Boolean = { false },
    isPredicted: (T) -> Boolean = { false },
    optionIcon: ((T) -> ImageVector?)? = null,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelectedChip = isSelected(option)
            val isPredictedChip = !isSelectedChip && isPredicted(option)
            // A bare tap handler instead of a Material chip: focusable chip internals
            // steal focus from the text field on press, which unmounts this row
            // mid-gesture before the click ever completes.
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    isSelectedChip -> MaterialTheme.colorScheme.secondaryContainer
                    isPredictedChip -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = when {
                    isSelectedChip -> MaterialTheme.colorScheme.onSecondaryContainer
                    isPredictedChip -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .semantics(mergeDescendants = true) {
                        selected = isSelectedChip
                        if (isPredictedChip) {
                            contentDescription = "Recommended"
                        }
                    }
                    .pointerInput(option) {
                        detectTapGestures { onSelect(option) }
                    },
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    val icon = optionIcon?.invoke(option)
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(16.dp),
                        )
                    }
                    Text(
                        text = optionLabel(option),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(
                            start = if (icon != null) 4.dp else 12.dp,
                            end = 12.dp,
                            top = 6.dp,
                            bottom = 6.dp,
                        ),
                    )
                }
            }
        }
    }
}
