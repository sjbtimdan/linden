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

    /** [source] narrowed by the current search query on the entity name. */
    protected fun <T : NamedEntity> Flow<List<T>>.filteredBySearch(): StateFlow<List<T>> = combine(
        this,
        _searchQuery,
    ) { items, query ->
        items.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }.stateFlow(emptyList())
}
