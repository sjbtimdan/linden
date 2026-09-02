package org.sjbtimdan.linden.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType

class CategoryListViewModel(
    private val categoryDao: CategoryDao,
    entryDao: EntryDao,
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val categories: StateFlow<List<Category>> = combine(
        categoryDao.getAll(),
        _searchQuery,
    ) { categories, query ->
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) {
            categories
        } else {
            categories.filter { it.name.lowercase().contains(normalized) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    /** Categories referenced by at least one entry; they cannot be deleted. */
    val categoriesWithEntries: StateFlow<Set<Long>> = entryDao.categoriesWithEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptySet(),
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /** Creates a category; returns false when the name is empty or already taken (case-insensitive). */
    fun createCategory(name: String, type: CategoryType, icon: CategoryIcon? = null): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (categories.value.any { it.name.equals(trimmed, ignoreCase = true) }) return false
        viewModelScope.launch {
            categoryDao.create(trimmed, type, icon)
        }
        return true
    }

    /** Updates a category; returns false when the name is empty or taken by another category (case-insensitive). */
    fun updateCategory(category: Category): Boolean {
        val trimmed = category.name.trim()
        if (trimmed.isEmpty()) return false
        if (categories.value.any { it.id != category.id && it.name.equals(trimmed, ignoreCase = true) }) return false
        viewModelScope.launch {
            categoryDao.update(category.copy(name = trimmed))
        }
        return true
    }

    /** Deletes a category; ignored when the category still has entries on it. */
    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            if (id in categoriesWithEntries.value) return@launch
            categoryDao.delete(id)
        }
    }
}
