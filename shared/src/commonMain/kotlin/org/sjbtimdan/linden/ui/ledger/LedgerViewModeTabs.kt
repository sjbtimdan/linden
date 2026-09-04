package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow

/**
 * Always-visible switch between the three ledger views (entries, period-end
 * account balances, category totals). Unlike the filters below it, the mode
 * tabs never collapse: they are navigation, not filtering.
 */
@Composable
fun LedgerViewModeTabs(viewMode: LedgerViewMode, onSelect: (LedgerViewMode) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("viewModeTabs"),
    ) {
        LedgerViewMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = viewMode == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = LedgerViewMode.entries.size),
                modifier = Modifier
                    .weight(1f)
                    .testTag("viewModeTab-${mode.name}"),
                label = {
                    Text(
                        text = mode.displayName(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

private fun LedgerViewMode.displayName(): String = when (this) {
    LedgerViewMode.Entries -> "Entries"
    LedgerViewMode.Accounts -> "Accounts"
    LedgerViewMode.Categories -> "Categories"
}
