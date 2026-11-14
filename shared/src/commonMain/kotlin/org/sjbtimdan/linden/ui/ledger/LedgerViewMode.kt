package org.sjbtimdan.linden.ui.ledger

/** What the ledger screen shows: the entry list, period-end account balances, or category totals. */
enum class LedgerViewMode {
    Entries,
    Accounts,
    Categories,
}

/**
 * Slide direction of a view-mode change, in tab order: 1 when moving to a
 * later mode and -1 when moving back, so the incoming view enters from the
 * side the old one left. Switching to the same mode is 0.
 */
internal fun viewModeTravel(initial: LedgerViewMode, target: LedgerViewMode): Int = when {
    target.ordinal > initial.ordinal -> 1
    target.ordinal < initial.ordinal -> -1
    else -> 0
}
