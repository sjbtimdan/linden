package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.dayIn
import org.sjbtimdan.linden.ui.entry.EntryRow
import org.sjbtimdan.linden.ui.entry.formatDate

internal sealed interface LedgerListItem {
    val key: Any
}

internal data class DayHeaderItem(
    override val key: Any,
    val label: String,
    /** Net income minus expenses for the day in the default currency; null when a rate is missing. */
    val totalMinor: Long?,
) : LedgerListItem

internal data class EntryListItem(val entry: Entry) : LedgerListItem {
    override val key: Any get() = entry.id
}

/** Builds the flat list of headers and entries shown by the ledger list. */
internal fun ledgerListItems(
    entries: List<Entry>,
    zone: TimeZone,
    defaultCurrency: Currency,
    rates: List<FxRate>,
): List<LedgerListItem> = buildList {
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
                totalMinor = periodTotalMinor(dayEntries, defaultCurrency, rates),
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
    defaultCurrency: Currency,
    onEntryClick: (Entry) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    LazyColumn(
        modifier = modifier.testTag("entryList"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        header?.let { content ->
            item(key = "entryListHeader") { content() }
        }
        items.forEach { item ->
            when (item) {
                is DayHeaderItem -> stickyHeader(item.key) {
                    DayHeader(
                        label = item.label,
                        total = item.totalMinor,
                        currency = defaultCurrency,
                        hidden = hideAmounts,
                    )
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
