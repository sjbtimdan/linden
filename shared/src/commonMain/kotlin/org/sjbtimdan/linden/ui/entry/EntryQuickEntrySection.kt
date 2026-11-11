package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.predictions.QuickEntry
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.entry_quick_entry
import org.sjbtimdan.linden.resources.entry_recurring

/** Keeps the quick-entry chips from sprawling when a description is long. */
private val QuickEntryChipMaxWidth = 220.dp

/**
 * Quick entries fill the form from a past entry but keep the current date and
 * time.
 */
@Composable
internal fun EntryQuickEntrySection(quickEntries: List<QuickEntry>, onSelect: (QuickEntry) -> Unit) {
    // Recurring quick-entry chips get a subtle tint and a screen-reader label:
    // the chip itself only shows the description and the repeat icon.
    val recurringChipColors = OptionChipColors(
        container = MaterialTheme.colorScheme.tertiaryContainer,
        content = MaterialTheme.colorScheme.onTertiaryContainer,
    )
    val recurringChipDescription = stringResource(Res.string.entry_recurring)

    Text(
        text = stringResource(Res.string.entry_quick_entry),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    val quickChips = remember(quickEntries) { quickEntryChips(quickEntries) }
    OptionChipRow(
        options = quickChips,
        optionLabel = { it.label },
        onSelect = { onSelect(it.entry) },
        dense = true,
        maxChipWidth = QuickEntryChipMaxWidth,
        optionIcon = { quickEntryIcon(it.entry) },
        optionColors = { if (it.entry.cadence != null) recurringChipColors else null },
        optionContentDescription = {
            if (it.entry.cadence != null) recurringChipDescription else null
        },
    )
}
