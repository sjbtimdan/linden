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
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.ledger_future_balances
import org.sjbtimdan.linden.resources.ledger_future_entries
import org.sjbtimdan.linden.resources.ledger_future_none
import org.sjbtimdan.linden.resources.ledger_future_none_bounded
import org.sjbtimdan.linden.resources.ledger_future_totals

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
@Composable
internal fun futureEntriesNoticeLabel(viewMode: LedgerViewMode, upcoming: Int, bounded: Boolean): String = when {
    upcoming == 0 && bounded -> stringResource(Res.string.ledger_future_none_bounded)

    upcoming == 0 -> stringResource(Res.string.ledger_future_none)

    viewMode == LedgerViewMode.Entries ->
        // The quantity selects the plural category; the same value must be
        // passed again as the %1$d format argument.
        pluralStringResource(Res.plurals.ledger_future_entries, upcoming, upcoming)

    viewMode == LedgerViewMode.Accounts -> stringResource(Res.string.ledger_future_balances)

    else -> stringResource(Res.string.ledger_future_totals)
}
