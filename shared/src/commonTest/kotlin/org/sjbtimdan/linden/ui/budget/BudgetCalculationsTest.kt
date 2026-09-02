package org.sjbtimdan.linden.ui.budget

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.Budget
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.ui.ledger.CategoryWithTotal

class BudgetCalculationsTest : StringSpec({
    "returns empty when there are no budgets" {
        val totals = listOf(categoryTotal("Groceries", -80_000, 3))
        computeCategoryBudgets(totals, emptyList()) shouldBe emptyList()
    }

    "matches a category by name and reports spent as absolute value" {
        val totals = listOf(categoryTotal("Groceries", -52_000, 3))
        val budgets = listOf(Budget("Groceries", 80_000))
        val result = computeCategoryBudgets(totals, budgets)
        result shouldBe listOf(
            CategoryBudget(
                category = Category(1, "Groceries", CategoryType.Expense),
                spent = 52_000,
                limit = 80_000,
                count = 3,
            ),
        )
    }

    "matches category names case-insensitively" {
        val totals = listOf(categoryTotal("Groceries", -52_000, 3))
        val budgets = listOf(Budget("groceries", 80_000))
        val result = computeCategoryBudgets(totals, budgets)
        result.single().limit shouldBe 80_000
    }

    "omits categories without a budget" {
        val totals = listOf(
            categoryTotal("Groceries", -52_000, 3),
            categoryTotal("Travel", -200_000, 1),
        )
        val budgets = listOf(Budget("Groceries", 80_000))
        val result = computeCategoryBudgets(totals, budgets)
        result.map { it.category?.name } shouldBe listOf("Groceries")
    }

    "omits budgets whose category has no totals" {
        val totals = listOf(categoryTotal("Groceries", -52_000, 3))
        val budgets = listOf(Budget("Groceries", 80_000), Budget("Travel", 200_000))
        val result = computeCategoryBudgets(totals, budgets)
        result.map { it.category?.name } shouldBe listOf("Groceries")
    }

    "fraction is clamped to 1 when over budget" {
        val totals = listOf(categoryTotal("Groceries", -120_000, 3))
        val budgets = listOf(Budget("Groceries", 80_000))
        val result = computeCategoryBudgets(totals, budgets).single()
        result.fraction shouldBe 1f
        result.overBudget shouldBe true
    }

    "fraction is 0 for a non-positive limit" {
        val totals = listOf(categoryTotal("Groceries", -52_000, 3))
        val budgets = listOf(Budget("Groceries", 0))
        val result = computeCategoryBudgets(totals, budgets).single()
        result.fraction shouldBe 0f
        result.overBudget shouldBe false
    }

    "fraction reflects partial spending" {
        val totals = listOf(categoryTotal("Groceries", -40_000, 3))
        val budgets = listOf(Budget("Groceries", 80_000))
        val result = computeCategoryBudgets(totals, budgets).single()
        result.fraction shouldBe 0.5f
        result.overBudget shouldBe false
    }
})

private fun categoryTotal(name: String, total: Long, count: Int): CategoryWithTotal = CategoryWithTotal(
    category = Category(1, name, CategoryType.Expense),
    total = total,
    count = count,
)
