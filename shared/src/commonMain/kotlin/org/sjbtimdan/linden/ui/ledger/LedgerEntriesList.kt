package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.dayIn
import org.sjbtimdan.linden.ui.LocalBottomBarContentPadding
import org.sjbtimdan.linden.ui.entry.EntryRow
import org.sjbtimdan.linden.ui.entry.formatDate
import org.sjbtimdan.linden.ui.rememberEntranceComplete
import org.sjbtimdan.linden.ui.staggeredEntrance

internal sealed interface LedgerListItem {
    val key: Any
}

internal data class DayHeaderItem(
    override val key: Any,
    val label: String,
) : LedgerListItem

internal data class EntryListItem(val entry: Entry) : LedgerListItem {
    override val key: Any get() = entry.id
}

/** Builds the flat list of headers and entries shown by the ledger list. */
internal fun ledgerListItems(entries: List<Entry>, zone: TimeZone): List<LedgerListItem> = buildList {
    var index = 0
    while (index < entries.size) {
        val day = entries[index].dayIn(zone)
        val start = index
        while (index < entries.size && entries[index].dayIn(zone) == day) {
            index++
        }
        val dayEntries = entries.subList(start, index)
        add(
            DayHeaderItem(
                key = "day-$day",
                label = formatDate(entries[start].createdAt, zone),
            ),
        )
        dayEntries.forEach { entry -> add(EntryListItem(entry)) }
    }
}

/** The entries view's list: sticky day headers with their rows. */
@Composable
internal fun LedgerEntriesList(
    items: List<LedgerListItem>,
    hideAmounts: Boolean,
    zone: TimeZone,
    onEntryClick: (Entry) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    val entranceComplete = rememberEntranceComplete(items.count { it is EntryListItem })
    LazyColumn(
        modifier = modifier.testTag("entryList"),
        contentPadding = PaddingValues(bottom = LocalBottomBarContentPadding.current),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        header?.let { content ->
            item(key = "entryListHeader") { content() }
        }
        var entryIndex = 0
        items.forEach { item ->
            when (item) {
                is DayHeaderItem -> stickyHeader(item.key) {
                    DayHeader(label = item.label)
                }

                is EntryListItem -> {
                    val index = entryIndex++
                    item(item.key) {
                        EntryRow(
                            entry = item.entry,
                            onClick = { onEntryClick(item.entry) },
                            hideAmounts = hideAmounts,
                            zone = zone,
                            modifier = Modifier
                                .animateItem()
                                .staggeredEntrance(index, !entranceComplete),
                        )
                    }
                }
            }
        }
    }
}
