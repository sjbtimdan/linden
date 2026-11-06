package org.sjbtimdan.linden.ui.budget

import androidx.compose.ui.test.ExperimentalTestApi
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.withBudgetViewModel

@OptIn(ExperimentalTestApi::class)
class BudgetViewModelTest : StringSpec({

    "saveBudget rejects a blank category name" {
        withBudgetViewModel { _, _, viewModel ->
            viewModel.saveBudget("  ", 80_000) shouldBe false
            viewModel.budgets.value.shouldBeEmpty()
        }
    }

    "saveBudget rejects a non-positive limit" {
        withBudgetViewModel { _, _, viewModel ->
            viewModel.saveBudget("Groceries", 0) shouldBe false
            viewModel.saveBudget("Groceries", -1) shouldBe false
            viewModel.budgets.value.shouldBeEmpty()
        }
    }

    "saveBudget stores a valid limit" {
        withBudgetViewModel { _, _, viewModel ->
            viewModel.saveBudget("Groceries", 80_000) shouldBe true

            viewModel.budgets.value.single().let { budget ->
                budget.categoryName shouldBe "Groceries"
                budget.limitMinor shouldBe 80_000L
            }
        }
    }

    "deleteBudget removes an existing budget" {
        withBudgetViewModel { _, _, viewModel ->
            viewModel.saveBudget("Groceries", 80_000)

            viewModel.deleteBudget("Groceries")

            viewModel.budgets.value.shouldBeEmpty()
        }
    }
})
