package org.sjbtimdan.linden.ui.entry

import org.sjbtimdan.linden.predictions.QuickEntry

/** A quick-entry option together with the compact label shown in its chip. */
internal data class QuickEntryChip(
    val entry: QuickEntry,
    val label: String,
)

/**
 * Builds chip labels from the description and amount. When two chips would
 * share the same label (same description and amount), the account — or the
 * transfer's destination — is appended so the variants stay distinguishable.
 * Amounts use the compact read-only formatter so the chips stay small.
 */
internal fun quickEntryChips(entries: List<QuickEntry>): List<QuickEntryChip> {
    val baseLabels = entries.map(::baseLabel)
    val counts = baseLabels.groupingBy { it }.eachCount()
    return entries.mapIndexed { index, entry ->
        val base = baseLabels[index]
        val label = if ((counts[base] ?: 0) > 1) "$base · ${entry.entry.subtitle()}" else base
        QuickEntryChip(entry, label)
    }
}

private fun baseLabel(entry: QuickEntry): String {
    val amount = formatAmountCompact(
        entry.entry.amount,
        entry.entry.account.currency.decimalDigits,
    )
    return "${entry.entry.description.orEmpty().trim()} · $amount"
}
