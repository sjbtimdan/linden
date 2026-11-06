package org.sjbtimdan.linden.ui.categories

import kotlinx.coroutines.flow.StateFlow
import org.sjbtimdan.linden.data.CategoryBudgetService
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.uniqueName
import org.sjbtimdan.linden.ui.SearchableListViewModel

class CategoryListViewModel(
    private val categoryDao: CategoryDao,
    private val categoryBudgetService: CategoryBudgetService,
    entryDao: EntryDao,
) : SearchableListViewModel() {
    private val searchableCategories = categoryDao.getAll().searchable()

    val categories: StateFlow<List<Category>> = searchableCategories.filtered

    /** Categories referenced by at least one entry; they cannot be deleted. */
    val categoriesWithEntries: StateFlow<Set<Long>> = entryDao.categoriesWithEntries().stateFlow(emptySet())

    /** Creates a category; returns false when the name is empty or already taken (case-insensitive). */
    fun createCategory(name: String, type: CategoryType, icon: CategoryIcon? = null): Boolean {
        val trimmed = uniqueName(searchableCategories.all.value, name) ?: return false
        launchWrite { categoryDao.create(trimmed, type, icon) }
        return true
    }

    /** Updates a category; returns false when the name is empty or taken by another category (case-insensitive). */
    fun updateCategory(category: Category): Boolean {
        val trimmed = uniqueName(
            searchableCategories.all.value,
            category.name,
            excludingId = category.id,
        ) ?: return false
        launchWrite {
            val previousName = searchableCategories.all.value.firstOrNull { it.id == category.id }?.name
            categoryBudgetService.updateCategory(category.copy(name = trimmed), previousName)
        }
        return true
    }

    /** Deletes a category and its budget; ignored when the category still has entries on it. */
    fun deleteCategory(id: Long) {
        launchWrite {
            if (id in categoriesWithEntries.value) return@launchWrite
            val name = searchableCategories.all.value.firstOrNull { it.id == id }?.name
            categoryBudgetService.deleteCategory(id, name)
        }
    }
}
