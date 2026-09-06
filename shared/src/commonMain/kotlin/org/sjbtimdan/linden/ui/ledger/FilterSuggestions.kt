package org.sjbtimdan.linden.ui.ledger

import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category

/** Which list a suggestion would narrow the entries view to. */
internal enum class FilterSuggestionKind {
    Account,
    Category,
}

/** A category or account the search field offers as a structural filter chip. */
internal data class FilterSuggestion(
    val kind: FilterSuggestionKind,
    val id: Long,
    val name: String,
)

/**
 * Ranks the categories and accounts whose names match [query] as filter
 * suggestions. Name matches are case-insensitive substrings; prefix matches
 * rank first, then alphabetical order (ties broken by kind, then id). The
 * already-active filter of the same kind is never suggested again, and the
 * result is capped at [limit] chips so the row stays compact.
 */
internal fun rankFilterSuggestions(
    query: String,
    categories: List<Category>,
    accounts: List<Account>,
    activeCategoryId: Long? = null,
    activeAccountId: Long? = null,
    limit: Int = 6,
): List<FilterSuggestion> {
    val normalized = query.trim().lowercase()
    if (normalized.isEmpty()) return emptyList()
    val suggestions = buildList {
        categories
            .asSequence()
            .filterNot { it.id == activeCategoryId }
            .mapTo(this) { FilterSuggestion(FilterSuggestionKind.Category, it.id, it.name) }
        accounts
            .asSequence()
            .filterNot { it.id == activeAccountId }
            .mapTo(this) { FilterSuggestion(FilterSuggestionKind.Account, it.id, it.name) }
    }
    return suggestions
        .filter { it.name.contains(normalized, ignoreCase = true) }
        .sortedWith(
            compareBy<FilterSuggestion> { !it.name.startsWith(normalized, ignoreCase = true) }
                .thenBy { it.name.lowercase() }
                .thenBy { it.kind }
                .thenBy { it.id },
        )
        .take(limit)
        .toList()
}
