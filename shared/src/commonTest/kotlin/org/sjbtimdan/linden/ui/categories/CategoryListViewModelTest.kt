package org.sjbtimdan.linden.ui.categories

import androidx.compose.ui.test.ExperimentalTestApi
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.ui.withViewModel

@OptIn(ExperimentalTestApi::class)
class CategoryListViewModelTest : StringSpec({
    "creating a category adds it to the list" {
        withViewModel { viewModel ->
            viewModel.categories.value.shouldBeEmpty()

            viewModel.createCategory("Groceries", CategoryType.Expense)

            viewModel.categories.value.shouldHaveSize(1)
            viewModel.categories.value.first().category.name shouldBe "Groceries"
            viewModel.categories.value.first().category.type shouldBe CategoryType.Expense
        }
    }

    "updating a category reflects in the list" {
        withViewModel { viewModel ->
            viewModel.createCategory("Groceries", CategoryType.Expense)
            val created = viewModel.categories.value.first()

            viewModel.updateCategory(created.category.copy(name = "Food"))
            viewModel.categories.value.shouldHaveSize(1)
            viewModel.categories.value.first().category.name shouldBe "Food"
        }
    }

    "direct database writes reflect in the list" {
        withViewModel { dao, viewModel ->
            viewModel.categories.value.shouldBeEmpty()

            dao.create("Salary", CategoryType.Income)

            viewModel.categories.value.shouldHaveSize(1)
            viewModel.categories.value.first().category.name shouldBe "Salary"
        }
    }

    "balance nets the category's entries" {
        withViewModel { categoryDao, entryDao, accountDao, viewModel ->
            categoryDao.create("Groceries", CategoryType.Expense)
            categoryDao.create("Salary", CategoryType.Income)
            accountDao.create("Main", Currency.CHF)
            val groceries = categoryDao.getAll().first().first { it.name == "Groceries" }
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }
            val main = accountDao.getAll().first().first()

            entryDao.create(ExpenseEntry(id = 0, category = groceries, description = "Coffee", account = main, amount = 450))
            entryDao.create(IncomeEntry(id = 0, category = salary, description = "Pay", account = main, amount = 50_000))

            viewModel.categories.value.single { it.category.name == "Groceries" }.balance shouldBe -450
            viewModel.categories.value.single { it.category.name == "Salary" }.balance shouldBe 50_000
        }
    }

    "balance converts foreign entries to the default currency" {
        val rate = FxRate(baseCurrency = Currency.CHF, quoteCurrency = Currency.EUR, rate = 1.1, date = "2026-08-16")
        withViewModel(defaultCurrency = Currency.CHF, rates = listOf(rate)) { categoryDao, entryDao, accountDao, viewModel ->
            categoryDao.create("Salary", CategoryType.Income)
            accountDao.create("Euros", Currency.EUR)
            val salary = categoryDao.getAll().first().first()
            val euros = accountDao.getAll().first().first()

            entryDao.create(IncomeEntry(id = 0, category = salary, description = "Pay", account = euros, amount = 5_000))

            viewModel.categories.value.single().balance shouldBe (5_000.0 / 1.1).roundToLong()
        }
    }

    "balance is null when a foreign entry has no stored rate" {
        withViewModel { categoryDao, entryDao, accountDao, viewModel ->
            categoryDao.create("Salary", CategoryType.Income)
            accountDao.create("Euros", Currency.EUR)
            val salary = categoryDao.getAll().first().first()
            val euros = accountDao.getAll().first().first()

            entryDao.create(IncomeEntry(id = 0, category = salary, description = "Pay", account = euros, amount = 5_000))

            viewModel.categories.value.single().balance shouldBe null
        }
    }

    "total nets all entries in the default currency" {
        withViewModel { categoryDao, entryDao, accountDao, viewModel ->
            categoryDao.create("Groceries", CategoryType.Expense)
            categoryDao.create("Salary", CategoryType.Income)
            accountDao.create("Main", Currency.CHF)
            val groceries = categoryDao.getAll().first().first { it.name == "Groceries" }
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }
            val main = accountDao.getAll().first().first()

            entryDao.create(ExpenseEntry(id = 0, category = groceries, description = "Coffee", account = main, amount = 450))
            entryDao.create(IncomeEntry(id = 0, category = salary, description = "Pay", account = main, amount = 50_000))

            viewModel.totalMinor.value shouldBe 49_550
        }
    }

    "total is null when a rate is missing" {
        withViewModel { categoryDao, entryDao, accountDao, viewModel ->
            categoryDao.create("Salary", CategoryType.Income)
            accountDao.create("Euros", Currency.EUR)
            val salary = categoryDao.getAll().first().first()
            val euros = accountDao.getAll().first().first()

            entryDao.create(IncomeEntry(id = 0, category = salary, description = "Pay", account = euros, amount = 5_000))

            viewModel.totalMinor.value shouldBe null
        }
    }
})
