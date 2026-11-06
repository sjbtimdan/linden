package org.sjbtimdan.linden.data

import org.sjbtimdan.linden.db.LindenDatabase
import org.sjbtimdan.linden.model.Category

/**
 * Category writes that must also maintain the name-keyed budget table. Both
 * operations run in one transaction so a renamed or deleted category can never
 * leave an orphaned budget behind.
 */
class CategoryBudgetService(private val database: LindenDatabase) {
    /** Updates [updated], moving a budget from [previousName] to the new name. */
    suspend fun updateCategory(updated: Category, previousName: String?) {
        database.transaction {
            database.categoryQueries.update(updated.name, updated.type.name, updated.icon?.name, updated.id)
            if (previousName != null && previousName != updated.name) {
                database.budgetQueries.renameByCategoryName(newName = updated.name, oldName = previousName)
            }
        }
    }

    /** Deletes the category and the budget that shared its [name]. */
    suspend fun deleteCategory(id: Long, name: String?) {
        database.transaction {
            database.categoryQueries.deleteById(id)
            name?.let { database.budgetQueries.deleteByCategoryName(it) }
        }
    }
}
