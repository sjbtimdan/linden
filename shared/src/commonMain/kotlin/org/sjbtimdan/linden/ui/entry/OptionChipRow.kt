package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { option ->
            val isSelectedChip = isSelected(option)
            // A bare tap handler instead of a Material chip: focusable chip internals
            // steal focus from the text field on press, which unmounts this row
            // mid-gesture before the click ever completes.
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelectedChip) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSelectedChip) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .semantics(mergeDescendants = true) {
                        selected = isSelectedChip
                    }
                    .pointerInput(option) {
                        detectTapGestures { onSelect(option) }
                    },
            ) {
                Text(
                    text = optionLabel(option),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
