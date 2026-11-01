package org.sjbtimdan.linden.ui.entry

import org.sjbtimdan.linden.predictions.QuickEntry

/** Suggestion lists shown by the entry form; empty when there are none. */
data class EntryFormSuggestions(
    val descriptions: List<String> = emptyList(),
    val accountIds: List<Long> = emptyList(),
    val categoryIds: List<Long> = emptyList(),
    val quickEntries: List<QuickEntry> = emptyList(),
)

/** Shared empty value, so the default parameter does not allocate on every recomposition. */
val EmptyEntryFormSuggestions = EntryFormSuggestions()
