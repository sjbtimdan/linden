package org.sjbtimdan.linden.ui.entry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.predictions.QuickEntry

/** Icon used for an entry type across list avatars, selectors and buttons. */
fun EntryType.icon(): ImageVector = when (this) {
    EntryType.Expense -> Icons.Filled.ShoppingCart
    EntryType.Income -> Icons.Filled.AddCircle
    EntryType.Transfer -> Icons.AutoMirrored.Filled.ArrowForward
}

/** Icon of a quick-entry chip: a repeat mark when the entry recurs, none otherwise. */
fun quickEntryIcon(quickEntry: QuickEntry): ImageVector? = if (quickEntry.cadence != null) Icons.Filled.Repeat else null
