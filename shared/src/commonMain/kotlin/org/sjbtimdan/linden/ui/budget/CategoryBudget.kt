package org.sjbtimdan.linden.ui.budget

import org.sjbtimdan.linden.model.Budget

/**
 * Budget limits keyed by lowercased category name; a later duplicate wins.
 * Matching is therefore case-insensitive, so a renamed category simply stops
 * matching its old budget.
 */
internal fun budgetsByCategoryName(budgets: List<Budget>): Map<String, Long> =
    budgets.associate { it.categoryName.lowercase() to it.limitMinor }
