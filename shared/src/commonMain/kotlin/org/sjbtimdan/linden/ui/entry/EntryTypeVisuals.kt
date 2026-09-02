package org.sjbtimdan.linden.ui.entry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.predictions.QuickEntry
import org.sjbtimdan.linden.predictions.RecurrenceCadence

/** Icon used for an entry type across list avatars, selectors and buttons. */
fun EntryType.icon(): ImageVector = when (this) {
    EntryType.Expense -> Icons.Filled.ShoppingCart
    EntryType.Income -> Icons.Filled.AddCircle
    EntryType.Transfer -> Icons.AutoMirrored.Filled.ArrowForward
}

/** Label of a quick-entry chip: the description, suffixed with a detected cadence. */
fun quickEntryLabel(quickEntry: QuickEntry): String {
    val description = quickEntry.entry.description.orEmpty()
    return when (quickEntry.cadence) {
        RecurrenceCadence.Weekly -> "$description · Weekly"
        RecurrenceCadence.Monthly -> "$description · Monthly"
        null -> description
    }
}
