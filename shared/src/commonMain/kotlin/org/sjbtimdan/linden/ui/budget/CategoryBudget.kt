package org.sjbtimdan.linden.ui.budget

import org.sjbtimdan.linden.model.Budget
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.ui.ledger.CategoryWithTotal
import kotlin.math.abs

/**
 * A category paired with its budget progress: how much was spent (absolute net
 * value in the default currency's minor units) against the monthly limit, plus
 * the entry count. Only categories that have a budget set are represented.
 */
data class CategoryBudget(
    val category: Category?,
    val spent: Long,
    val limit: Long,
    val count: Int,
) {
    /** Fraction of the budget spent, clamped to [0, 1]; 0 when the limit is not positive. */
    val fraction: Float
        get() = if (limit <= 0) 0f else (spent.toFloat() / limit.toFloat()).coerceIn(0f, 1f)

    /** Whether spending has exceeded the limit. */
    val overBudget: Boolean get() = limit > 0 && spent > limit
}

/**
 * Joins the per-category totals with the configured budgets, producing one
 * [CategoryBudget] per category that has a budget. Categories without a budget
 * are omitted. Matching is by category name (case-insensitive), so a renamed
 * category simply stops matching its old budget.
 */
fun computeCategoryBudgets(categoryTotals: List<CategoryWithTotal>, budgets: List<Budget>): List<CategoryBudget> {
    if (budgets.isEmpty()) return emptyList()
    val byName = budgets.associateBy { it.categoryName.lowercase() }
    return categoryTotals.mapNotNull { total ->
        val categoryName = total.category?.name ?: return@mapNotNull null
        val budget = byName[categoryName.lowercase()] ?: return@mapNotNull null
        CategoryBudget(
            category = total.category,
            spent = abs(total.total),
            limit = budget.limitMinor,
            count = total.count,
        )
    }
}
