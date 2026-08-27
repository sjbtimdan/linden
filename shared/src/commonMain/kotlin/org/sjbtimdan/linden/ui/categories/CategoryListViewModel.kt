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
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType

class CategoryListViewModel(
    private val categoryDao: CategoryDao,
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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createCategory(name: String, type: CategoryType, icon: CategoryIcon? = null) {
        viewModelScope.launch {
            categoryDao.create(name, type, icon)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.update(category)
        }
    }
}
