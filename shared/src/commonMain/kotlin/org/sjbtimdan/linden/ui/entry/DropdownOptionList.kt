package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun <T> DropdownOptionList(
    options: List<T>,
    optionLabel: (T) -> String,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        options.forEach { option ->
            val selected = isSelected(option)
            // Bare tap handlers instead of a clickable Material component: focusable
            // internals steal focus from the filter field on press, which unmounts
            // this list mid-gesture before the click ever completes.
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {}
                    .pointerInput(option) {
                        detectTapGestures { onSelect(option) }
                    },
            ) {
                Text(
                    text = optionLabel(option),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}
