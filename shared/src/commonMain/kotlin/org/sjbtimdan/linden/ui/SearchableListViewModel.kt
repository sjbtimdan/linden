package org.sjbtimdan.linden.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import org.sjbtimdan.linden.model.NamedEntity
import org.sjbtimdan.linden.util.stateFlow

/**
 * Base for the list screens that offer a name search: owns the query state and
 * narrows a source flow of named entities by it, case-insensitively.
 */
abstract class SearchableListViewModel : AppViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * A shared source together with its search-narrowed view. Name validation
     * must use [all], because a duplicate filtered out by the current search
     * would otherwise be invisible to it and reach the database.
     */
    protected class SearchableList<T : NamedEntity>(
        val all: StateFlow<List<T>>,
        val filtered: StateFlow<List<T>>,
    )

    /** Shares [this] once and derives both the complete and the filtered list from it. */
    protected fun <T : NamedEntity> Flow<List<T>>.searchable(): SearchableList<T> {
        val all = stateFlow(emptyList())
        return SearchableList(all, all.filteredBySearch())
    }

    /** [source] narrowed by the current search query on the entity name. */
    protected fun <T : NamedEntity> Flow<List<T>>.filteredBySearch(): StateFlow<List<T>> = combine(
        this,
        _searchQuery,
    ) { items, query ->
        items.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }.stateFlow(emptyList())
}
