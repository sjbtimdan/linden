package org.sjbtimdan.linden.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType

class CategoryListViewModel(
    private val categoryDao: CategoryDao
) : ViewModel() {
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _categories.value = categoryDao.getAll()
        }
    }

    fun createCategory(name: String, type: CategoryType) {
        viewModelScope.launch {
            categoryDao.create(name, type)
            loadCategories()
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.update(category)
            loadCategories()
        }
    }
}
