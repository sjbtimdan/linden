package org.sjbtimdan.linden.ui.history

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.ui.entry.EntryEditorViewModel
import org.sjbtimdan.linden.ui.entry.SortOrder

class HistoryViewModel(
    entryDao: EntryDao,
    accountDao: AccountDao,
    categoryDao: CategoryDao,
) : EntryEditorViewModel(entryDao, accountDao, categoryDao) {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow<EntryType?>(null)
    val typeFilter: StateFlow<EntryType?> = _typeFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NewestFirst)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val entries: StateFlow<List<Entry>> = combine(
        allEntries,
        _searchQuery,
        _typeFilter,
        _sortOrder,
    ) { all, query, type, order ->
        all.asSequence()
            .filter { entry -> type == null || entry.type == type }
            .filter { entry -> query.isBlank() || entry.matches(query) }
            .sortedWith(order.comparator())
            .toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: EntryType?) {
        _typeFilter.value = type
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }
}

private fun Entry.matches(query: String): Boolean {
    val q = query.lowercase()
    return description?.lowercase()?.contains(q) == true ||
        category?.name?.lowercase()?.contains(q) == true ||
        account.name.lowercase().contains(q) ||
        (this as? TransferEntry)?.toAccount?.name?.lowercase()?.contains(q) == true
}

private fun SortOrder.comparator(): Comparator<Entry> = when (this) {
    SortOrder.NewestFirst -> compareByDescending<Entry> { it.createdAt }.thenByDescending { it.id }
    SortOrder.OldestFirst -> compareBy<Entry> { it.createdAt }.thenBy { it.id }
    SortOrder.AmountHighToLow -> compareByDescending<Entry> { it.amount }.thenByDescending { it.id }
    SortOrder.AmountLowToHigh -> compareBy<Entry> { it.amount }.thenByDescending { it.id }
}
