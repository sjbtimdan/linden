package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.dayIn
import org.sjbtimdan.linden.ui.entry.EntryRow
import org.sjbtimdan.linden.ui.entry.formatDate

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
    var previousDay: LocalDate? = null
    entries.forEach { entry ->
        val day = entry.dayIn(zone)
        if (day != previousDay) {
            add(DayHeaderItem("day-$day", formatDate(entry.createdAt, zone)))
            previousDay = day
        }
        add(EntryListItem(entry))
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
) {
    LazyColumn(
        modifier = modifier.testTag("entryList"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { item ->
            when (item) {
                is DayHeaderItem -> stickyHeader(item.key) {
                    DayHeader(label = item.label)
                }

                is EntryListItem -> item(item.key) {
                    EntryRow(
                        entry = item.entry,
                        onClick = { onEntryClick(item.entry) },
                        hideAmounts = hideAmounts,
                        zone = zone,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}
