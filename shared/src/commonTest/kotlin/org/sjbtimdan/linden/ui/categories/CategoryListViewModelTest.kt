package org.sjbtimdan.linden.ui.categories

import androidx.compose.ui.test.ExperimentalTestApi
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.ui.withViewModel

@OptIn(ExperimentalTestApi::class)
class CategoryListViewModelTest : StringSpec({
    "creating a category adds it to the list" {
        withViewModel { viewModel ->
            viewModel.categories.value.shouldBeEmpty()

            viewModel.createCategory("Groceries", CategoryType.Expense)

            viewModel.categories.value.shouldHaveSize(1)
            viewModel.categories.value.first().name shouldBe "Groceries"
            viewModel.categories.value.first().type shouldBe CategoryType.Expense
        }
    }

    "updating a category reflects in the list" {
        withViewModel { viewModel ->
            viewModel.createCategory("Groceries", CategoryType.Expense)
            val created = viewModel.categories.value.first()

            viewModel.updateCategory(created.copy(name = "Food"))
            viewModel.categories.value.shouldHaveSize(1)
            viewModel.categories.value.first().name shouldBe "Food"
        }
    }

    "direct database writes reflect in the list" {
        withViewModel { dao, viewModel ->
            viewModel.categories.value.shouldBeEmpty()

            dao.create("Salary", CategoryType.Income)

            viewModel.categories.value.shouldHaveSize(1)
            viewModel.categories.value.first().name shouldBe "Salary"
        }
    }

    "search filters categories by name, case-insensitively" {
        withViewModel { viewModel ->
            viewModel.createCategory("Groceries", CategoryType.Expense)
            viewModel.createCategory("Salary", CategoryType.Income)

            viewModel.setSearchQuery("GRO")

            viewModel.categories.value.map { it.name } shouldBe listOf("Groceries")
        }
    }

    "search matches substrings and clearing it restores all categories" {
        withViewModel { viewModel ->
            viewModel.createCategory("Groceries", CategoryType.Expense)
            viewModel.createCategory("Salary", CategoryType.Income)

            viewModel.setSearchQuery("ary")
            viewModel.categories.value.map { it.name } shouldBe listOf("Salary")

            viewModel.setSearchQuery("")
            viewModel.categories.value.map { it.name } shouldBe listOf("Groceries", "Salary")
        }
    }

    "search with no matches yields an empty list" {
        withViewModel { viewModel ->
            viewModel.createCategory("Groceries", CategoryType.Expense)

            viewModel.setSearchQuery("nonexistent")

            viewModel.categories.value.shouldBeEmpty()
        }
    }
})
