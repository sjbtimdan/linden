package org.sjbtimdan.linden.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.db.LindenDatabase
import org.sjbtimdan.linden.model.CategoryType

private class Fixture(database: LindenDatabase) {
    val categoryDao = CategoryDao(database.categoryQueries)
    val budgetDao = BudgetDao(database.budgetQueries)
    val service = CategoryBudgetService(database)
}

private suspend fun fixture(): Fixture = Fixture(lindenDatabase())

class CategoryBudgetServiceTest : StringSpec({

    "renaming a category moves its budget to the new name" {
        val fixture = fixture()
        fixture.categoryDao.create("Food", CategoryType.Expense)
        val food = fixture.categoryDao.getAll().first().single()
        fixture.budgetDao.upsert("Food", 80_000)

        fixture.service.updateCategory(food.copy(name = "Groceries"), previousName = "Food")

        fixture.categoryDao.getAll().first().single().name shouldBe "Groceries"
        fixture.budgetDao.budgetsFlow().first().map { it.categoryName to it.limitMinor } shouldBe
            listOf("Groceries" to 80_000L)
    }

    "renaming a category without a budget leaves the others untouched" {
        val fixture = fixture()
        fixture.categoryDao.create("Food", CategoryType.Expense)
        val food = fixture.categoryDao.getAll().first().single()
        fixture.budgetDao.upsert("Salary", 120_000)

        fixture.service.updateCategory(food.copy(name = "Groceries"), previousName = "Food")

        fixture.budgetDao.budgetsFlow().first().map { it.categoryName to it.limitMinor } shouldBe
            listOf("Salary" to 120_000L)
    }

    "renaming onto an orphan budget replaces it" {
        val fixture = fixture()
        fixture.categoryDao.create("Food", CategoryType.Expense)
        val food = fixture.categoryDao.getAll().first().single()
        fixture.budgetDao.upsert("Food", 80_000)
        fixture.budgetDao.upsert("Groceries", 50_000)

        fixture.service.updateCategory(food.copy(name = "Groceries"), previousName = "Food")

        fixture.budgetDao.budgetsFlow().first().map { it.categoryName to it.limitMinor } shouldBe
            listOf("Groceries" to 80_000L)
    }

    "deleting a category removes its budget" {
        val fixture = fixture()
        fixture.categoryDao.create("Food", CategoryType.Expense)
        val food = fixture.categoryDao.getAll().first().single()
        fixture.budgetDao.upsert("Food", 80_000)

        fixture.service.deleteCategory(food.id, "Food")

        fixture.categoryDao.getAll().first().shouldBeEmpty()
        fixture.budgetDao.budgetsFlow().first().shouldBeEmpty()
    }

    "deleting a category without a budget leaves the others untouched" {
        val fixture = fixture()
        fixture.categoryDao.create("Food", CategoryType.Expense)
        val food = fixture.categoryDao.getAll().first().single()
        fixture.budgetDao.upsert("Salary", 120_000)

        fixture.service.deleteCategory(food.id, "Food")

        fixture.categoryDao.getAll().first().shouldBeEmpty()
        fixture.budgetDao.budgetsFlow().first().map { it.categoryName to it.limitMinor } shouldBe
            listOf("Salary" to 120_000L)
    }

    "updating without a previous name leaves budgets untouched" {
        val fixture = fixture()
        fixture.categoryDao.create("Food", CategoryType.Expense)
        val food = fixture.categoryDao.getAll().first().single()
        fixture.budgetDao.upsert("Food", 80_000)

        fixture.service.updateCategory(food.copy(name = "Groceries"), previousName = null)

        fixture.categoryDao.getAll().first().single().name shouldBe "Groceries"
        fixture.budgetDao.budgetsFlow().first().map { it.categoryName to it.limitMinor } shouldBe
            listOf("Food" to 80_000L)
    }

    "renaming a category to its current name leaves the budget untouched" {
        val fixture = fixture()
        fixture.categoryDao.create("Food", CategoryType.Expense)
        val food = fixture.categoryDao.getAll().first().single()
        fixture.budgetDao.upsert("Food", 80_000)

        fixture.service.updateCategory(food, previousName = "Food")

        fixture.categoryDao.getAll().first().single().name shouldBe "Food"
        fixture.budgetDao.budgetsFlow().first().map { it.categoryName to it.limitMinor } shouldBe
            listOf("Food" to 80_000L)
    }

    "deleting without a name leaves other budgets untouched" {
        val fixture = fixture()
        fixture.categoryDao.create("Food", CategoryType.Expense)
        val food = fixture.categoryDao.getAll().first().single()
        fixture.budgetDao.upsert("Salary", 120_000)

        fixture.service.deleteCategory(food.id, name = null)

        fixture.categoryDao.getAll().first().shouldBeEmpty()
        fixture.budgetDao.budgetsFlow().first().map { it.categoryName to it.limitMinor } shouldBe
            listOf("Salary" to 120_000L)
    }
})
