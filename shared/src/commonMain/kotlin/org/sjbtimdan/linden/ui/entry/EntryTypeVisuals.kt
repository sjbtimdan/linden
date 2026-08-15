package org.sjbtimdan.linden.ui.entry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import org.sjbtimdan.linden.model.EntryType

/** Icon used for an entry type across list avatars, selectors and buttons. */
fun EntryType.icon(): ImageVector = when (this) {
    EntryType.Expense -> Icons.Filled.ShoppingCart
    EntryType.Income -> Icons.Filled.AddCircle
    EntryType.Transfer -> Icons.AutoMirrored.Filled.ArrowForward
}
