package org.sjbtimdan.linden.ui.ledger

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.ledger_action_add_first_entry
import org.sjbtimdan.linden.resources.ledger_action_show_future_entries
import org.sjbtimdan.linden.resources.ledger_empty_future_hidden
import org.sjbtimdan.linden.resources.ledger_empty_no_entries
import org.sjbtimdan.linden.resources.ledger_empty_no_match
import org.sjbtimdan.linden.resources.ledger_empty_no_match_filter

/** Which empty state the entries view shows for its empty list. */
internal enum class LedgerEntriesEmptyState {
    /** No filters and no entries at all: the guided "add your first entry" state. */
    Guided,

    /** No filters, but entries exist after today and are hidden by the show-future rule. */
    UpcomingHidden,

    /** Only the filter chips narrow the list; nothing matches them. */
    Filtered,

    /** No filters; the shown period window holds no entries. */
    NoEntries,

    /** Search, type or period narrows the list; nothing matches. */
    NoMatch,
}

/**
 * Resolves the entries view's empty state from the active filters and data. The
 * single place deciding which state applies, so a new filter cannot fall
 * through a copy of the predicate (the amount filter once did, reporting
 * "No entries yet." for a filter miss).
 */
internal fun ledgerEntriesEmptyState(
    searchQuery: String,
    typeFilter: EntryType?,
    categoryFilter: Long?,
    accountFilter: Long?,
    amountFilter: AmountFilter?,
    periodIsAll: Boolean,
    showFuture: Boolean,
    upcomingCount: Int,
    hasAnyEntries: Boolean,
): LedgerEntriesEmptyState {
    val nothingFiltered = searchQuery.isBlank() &&
        typeFilter == null &&
        categoryFilter == null &&
        accountFilter == null &&
        amountFilter == null
    val filterChipsOnly = (categoryFilter != null || accountFilter != null || amountFilter != null) &&
        searchQuery.isBlank() &&
        typeFilter == null &&
        periodIsAll
    return when {
        !showFuture && upcomingCount > 0 && nothingFiltered -> LedgerEntriesEmptyState.UpcomingHidden
        nothingFiltered && !hasAnyEntries -> LedgerEntriesEmptyState.Guided
        filterChipsOnly -> LedgerEntriesEmptyState.Filtered
        nothingFiltered && periodIsAll -> LedgerEntriesEmptyState.NoEntries
        else -> LedgerEntriesEmptyState.NoMatch
    }
}

/** Empty state of the entries view, explaining the empty list and offering the fitting action. */
@Composable
internal fun LedgerEntriesEmpty(
    state: LedgerEntriesEmptyState,
    onAddFirstEntry: () -> Unit,
    onRevealUpcoming: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        message = when (state) {
            LedgerEntriesEmptyState.UpcomingHidden -> stringResource(Res.string.ledger_empty_future_hidden)

            LedgerEntriesEmptyState.Guided, LedgerEntriesEmptyState.NoEntries ->
                stringResource(Res.string.ledger_empty_no_entries)

            LedgerEntriesEmptyState.Filtered -> stringResource(Res.string.ledger_empty_no_match_filter)

            LedgerEntriesEmptyState.NoMatch -> stringResource(Res.string.ledger_empty_no_match)
        },
        actionLabel = when (state) {
            LedgerEntriesEmptyState.Guided -> stringResource(Res.string.ledger_action_add_first_entry)
            LedgerEntriesEmptyState.UpcomingHidden -> stringResource(Res.string.ledger_action_show_future_entries)
            else -> null
        },
        onAction = when (state) {
            LedgerEntriesEmptyState.Guided -> onAddFirstEntry
            LedgerEntriesEmptyState.UpcomingHidden -> onRevealUpcoming
            else -> null
        },
        modifier = modifier,
    )
}
