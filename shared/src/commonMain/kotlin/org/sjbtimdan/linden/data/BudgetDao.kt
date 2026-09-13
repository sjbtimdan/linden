package org.sjbtimdan.linden.data

import kotlinx.coroutines.flow.Flow
import org.sjbtimdan.linden.BudgetEntity
import org.sjbtimdan.linden.BudgetQueries
import org.sjbtimdan.linden.model.Budget
import org.sjbtimdan.linden.util.asListFlow

/** Persists monthly category budgets in their own table, keyed by category name. */
class BudgetDao(private val queries: BudgetQueries) {
    fun budgetsFlow(): Flow<List<Budget>> = queries.selectAll().asListFlow { it.toBudget() }

    /** Inserts or replaces the budget for [categoryName]. */
    suspend fun upsert(categoryName: String, limitMinor: Long) {
        queries.insertOrReplace(categoryName, limitMinor)
    }

    /** Removes the budget for [categoryName] if one exists. */
    suspend fun delete(categoryName: String) {
        queries.deleteByCategoryName(categoryName)
    }

    private fun BudgetEntity.toBudget() = Budget(
        categoryName = category_name,
        limitMinor = limit_minor,
    )
}
