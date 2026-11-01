package org.sjbtimdan.linden.ui.ledger

import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.TransferEntry

/** An entry with its searchable fields pre-lowercased, so filtering on a keystroke does not re-lowercase every field. */
internal class SearchableEntry(val entry: Entry) {
    private val description = entry.description?.lowercase()
    private val categoryName = entry.category?.name?.lowercase()
    private val accountName = entry.account.name.lowercase()
    private val toAccountName = (entry as? TransferEntry)?.toAccount?.name?.lowercase()

    fun matches(query: String): Boolean = description?.contains(query) == true ||
        categoryName?.contains(query) == true ||
        accountName.contains(query) ||
        toAccountName?.contains(query) == true

    /** Whether the entry's amount (or, for transfers, its to-amount) satisfies [filter]. */
    fun matchesAmount(filter: AmountFilter): Boolean =
        filter.matches(entry.amount) || (entry as? TransferEntry)?.toAmount?.let(filter::matches) == true
}
