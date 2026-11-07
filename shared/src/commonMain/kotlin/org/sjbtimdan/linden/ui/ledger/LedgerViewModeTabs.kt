package org.sjbtimdan.linden.ui.ledger

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_accounts
import org.sjbtimdan.linden.resources.common_categories
import org.sjbtimdan.linden.resources.ledger_mode_entries
import org.sjbtimdan.linden.ui.SlidingPillTab
import org.sjbtimdan.linden.ui.SlidingPillTabs

/**
 * Always-visible switch between the three ledger views (entries, period-end
 * account balances, category totals). Unlike the filters below it, the mode
 * tabs never collapse: they are navigation, not filtering. The selected pill
 * slides between fixed thirds, which leaves more room for the labels than a
 * segmented button row on narrow phones.
 */
@Composable
fun LedgerViewModeTabs(viewMode: LedgerViewMode, onSelect: (LedgerViewMode) -> Unit, modifier: Modifier = Modifier) {
    val modes = LedgerViewMode.entries
    SlidingPillTabs(
        tabs = modes.map { mode ->
            SlidingPillTab(
                label = mode.displayName(),
                tag = "viewModeTab-${mode.name}",
            )
        },
        selectedIndex = modes.indexOf(viewMode).coerceAtLeast(0),
        onSelect = { index -> onSelect(modes[index]) },
        modifier = modifier.testTag("viewModeTabs"),
    )
}

@Composable
private fun LedgerViewMode.displayName(): String = stringResource(
    when (this) {
        LedgerViewMode.Entries -> Res.string.ledger_mode_entries
        LedgerViewMode.Accounts -> Res.string.common_accounts
        LedgerViewMode.Categories -> Res.string.common_categories
    },
)
