package org.sjbtimdan.linden.ui.categories

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.uniqueName
import org.sjbtimdan.linden.ui.SearchableListViewModel

class CategoryListViewModel(
    private val categoryDao: CategoryDao,
    entryDao: EntryDao,
) : SearchableListViewModel() {
    val categories: StateFlow<List<Category>> = categoryDao.getAll().filteredBySearch()

    /** Categories referenced by at least one entry; they cannot be deleted. */
    val categoriesWithEntries: StateFlow<Set<Long>> = entryDao.categoriesWithEntries().stateFlow(emptySet())

    /** Creates a category; returns false when the name is empty or already taken (case-insensitive). */
    fun createCategory(name: String, type: CategoryType, icon: CategoryIcon? = null): Boolean {
        val trimmed = uniqueName(categories.value, name) ?: return false
        viewModelScope.launch {
            try {
                categoryDao.create(trimmed, type, icon)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reportError(e.message)
            }
        }
        return true
    }

    /** Updates a category; returns false when the name is empty or taken by another category (case-insensitive). */
    fun updateCategory(category: Category): Boolean {
        val trimmed = uniqueName(categories.value, category.name, excludingId = category.id) ?: return false
        viewModelScope.launch {
            try {
                categoryDao.update(category.copy(name = trimmed))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reportError(e.message)
            }
        }
        return true
    }

    /** Deletes a category; ignored when the category still has entries on it. */
    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            try {
                if (id in categoriesWithEntries.value) return@launch
                categoryDao.delete(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reportError(e.message)
            }
        }
    }
}
