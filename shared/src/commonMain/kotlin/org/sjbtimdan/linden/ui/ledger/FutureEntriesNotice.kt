package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Explains what the show-future toggle did, shown under the period bar while
 * future entries are included. Tapping the chip hides future entries again.
 */
@Composable
fun FutureEntriesNotice(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    InputChip(
        selected = false,
        onClick = onClick,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        },
        modifier = modifier.testTag("showFutureNotice"),
    )
}

/**
 * Text of the notice while future entries are shown. [upcoming] is the number
 * of entries dated after today in the current window; [bounded] is false for
 * the [LedgerPeriod.All] period, which has no window to name.
 */
internal fun futureEntriesNoticeLabel(viewMode: LedgerViewMode, upcoming: Int, bounded: Boolean): String = when {
    upcoming == 0 && bounded -> "No entries after today in this period"

    upcoming == 0 -> "No entries after today"

    viewMode == LedgerViewMode.Entries ->
        "Showing $upcoming ${if (upcoming == 1) "entry" else "entries"} after today"

    viewMode == LedgerViewMode.Accounts -> "Balances include entries after today"

    else -> "Totals include entries after today"
}
