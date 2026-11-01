package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.FxRate

/** The entries view: the day-grouped list, or its guided/filtered empty state. */
@Composable
internal fun LedgerEntriesView(
    entries: List<Entry>,
    zone: TimeZone,
    defaultCurrency: Currency,
    rates: List<FxRate>,
    hideAmounts: Boolean,
    searchQuery: String,
    typeFilter: EntryType?,
    categoryFilter: Long?,
    accountFilter: Long?,
    amountFilter: AmountFilter?,
    periodIsAll: Boolean,
    showFuture: Boolean,
    upcomingCount: Int,
    hasAnyEntries: Boolean,
    paceHeader: (@Composable () -> Unit)?,
    onEntryClick: (Entry) -> Unit,
    onAddFirstEntry: () -> Unit,
    onRevealUpcoming: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        Column(modifier = modifier) {
            paceHeader?.invoke()
            LedgerEntriesEmpty(
                state = ledgerEntriesEmptyState(
                    searchQuery = searchQuery,
                    typeFilter = typeFilter,
                    categoryFilter = categoryFilter,
                    accountFilter = accountFilter,
                    amountFilter = amountFilter,
                    periodIsAll = periodIsAll,
                    showFuture = showFuture,
                    upcomingCount = upcomingCount,
                    hasAnyEntries = hasAnyEntries,
                ),
                onAddFirstEntry = onAddFirstEntry,
                onRevealUpcoming = onRevealUpcoming,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    } else {
        val listItems = remember(entries, defaultCurrency, rates) {
            ledgerListItems(
                entries = entries,
                zone = zone,
                defaultCurrency = defaultCurrency,
                rates = rates,
            )
        }
        LedgerEntriesList(
            items = listItems,
            hideAmounts = hideAmounts,
            zone = zone,
            defaultCurrency = defaultCurrency,
            onEntryClick = onEntryClick,
            header = paceHeader,
            modifier = modifier,
        )
    }
}
