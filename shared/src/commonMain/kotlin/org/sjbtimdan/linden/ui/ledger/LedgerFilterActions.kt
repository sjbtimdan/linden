package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_search
import org.sjbtimdan.linden.resources.ledger_filters_header

/**
 * The filter entry points merged into the period toolbar, replacing the
 * standalone "Filters & search" header row: a search icon that expands the
 * panel and focuses its field in one tap, and a filter icon whose badge
 * counts the active filters so a narrowed view reads at a glance.
 */
@Composable
fun LedgerFilterActions(
    activeFilterCount: Int,
    onSearch: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        IconButton(
            onClick = onSearch,
            modifier = Modifier.testTag("searchAction"),
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(Res.string.common_search),
            )
        }
        IconButton(
            onClick = onToggle,
            modifier = Modifier.testTag("filterAction"),
        ) {
            BadgedBox(
                badge = {
                    if (activeFilterCount > 0) {
                        Badge {
                            Text(activeFilterCount.toString())
                        }
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = stringResource(Res.string.ledger_filters_header),
                )
            }
        }
    }
}
