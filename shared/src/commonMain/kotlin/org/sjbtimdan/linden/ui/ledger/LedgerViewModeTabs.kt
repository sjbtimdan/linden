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
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.ledger_mode_accounts
import org.sjbtimdan.linden.resources.ledger_mode_categories
import org.sjbtimdan.linden.resources.ledger_mode_entries

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

@Composable
private fun LedgerViewMode.displayName(): String = stringResource(
    when (this) {
        LedgerViewMode.Entries -> Res.string.ledger_mode_entries
        LedgerViewMode.Accounts -> Res.string.ledger_mode_accounts
        LedgerViewMode.Categories -> Res.string.ledger_mode_categories
    },
)
