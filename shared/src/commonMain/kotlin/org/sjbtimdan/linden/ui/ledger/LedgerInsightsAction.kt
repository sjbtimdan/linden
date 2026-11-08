package org.sjbtimdan.linden.ui.ledger

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.settings_nav_insights

/**
 * Shortcut from the ledger's period toolbar to the Insights sub-screen, so the
 * month-trend view is reachable without going through Settings.
 */
@Composable
fun LedgerInsightsAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier.testTag("insightsAction"),
    ) {
        Icon(
            imageVector = Icons.Filled.BarChart,
            contentDescription = stringResource(Res.string.settings_nav_insights),
        )
    }
}
