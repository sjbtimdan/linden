package org.sjbtimdan.linden.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency

class DefaultDataSeederTest : StringSpec({
    "seeds the default categories and accounts into an empty database" {
        val database = lindenDatabase()
        val categoryDao = CategoryDao(database.categoryQueries)
        val accountDao = AccountDao(database.accountQueries)

        DefaultDataSeeder(database).seedIfEmpty(Currency.CHF)

        val categories = categoryDao.getAll().first()
        categories.map { it.name } shouldContainExactly listOf(
            "Bills",
            "Car",
            "Education",
            "Gifts",
            "Groceries",
            "Holidays",
            "House",
            "Interest",
            "Pets",
            "Public Transport",
            "Refunds",
            "Restaurants",
            "Salary",
        )
        categories.filter { it.type == CategoryType.Expense }.map { it.name } shouldContainExactly listOf(
            "Bills",
            "Car",
            "Education",
            "Groceries",
            "Holidays",
            "House",
            "Pets",
            "Public Transport",
            "Restaurants",
        )
        categories.filter { it.type == CategoryType.Income }.map { it.name } shouldContainExactly listOf(
            "Gifts",
            "Interest",
            "Refunds",
            "Salary",
        )
        categories.first { it.name == "Groceries" }.icon shouldBe CategoryIcon.ShoppingCart
        categories.first { it.name == "Public Transport" }.icon shouldBe CategoryIcon.DirectionsBus
        categories.first { it.name == "Salary" }.icon shouldBe CategoryIcon.Savings
        categories.first { it.name == "Gifts" }.icon shouldBe CategoryIcon.FavoriteBorder

        accountDao.getAll().first().map { it.name } shouldContainExactly
            listOf("Current Account", "Savings Account")
    }

    "creates the seeded accounts in the default currency with a zero balance" {
        val database = lindenDatabase()
        val accountDao = AccountDao(database.accountQueries)

        DefaultDataSeeder(database).seedIfEmpty(Currency.EUR)

        val accounts = accountDao.getAll().first()
        accounts.map { it.currency } shouldBe listOf(Currency.EUR, Currency.EUR)
        accounts.all { it.initialBalance == 0L } shouldBe true
    }

    "does not seed when the database already has an account" {
        val database = lindenDatabase()
        val categoryDao = CategoryDao(database.categoryQueries)
        AccountDao(database.accountQueries).create("Cash", Currency.CHF)

        DefaultDataSeeder(database).seedIfEmpty(Currency.CHF)

        categoryDao.getAll().first() shouldBe emptyList()
    }

    "does not seed when the database already has a category" {
        val database = lindenDatabase()
        val accountDao = AccountDao(database.accountQueries)
        CategoryDao(database.categoryQueries).create("Custom", CategoryType.Expense)

        DefaultDataSeeder(database).seedIfEmpty(Currency.CHF)

        accountDao.getAll().first() shouldBe emptyList()
    }

    "seeding twice does not duplicate the defaults" {
        val database = lindenDatabase()
        val categoryDao = CategoryDao(database.categoryQueries)
        val accountDao = AccountDao(database.accountQueries)
        val seeder = DefaultDataSeeder(database)

        seeder.seedIfEmpty(Currency.CHF)
        seeder.seedIfEmpty(Currency.CHF)

        categoryDao.getAll().first().size shouldBe 13
        accountDao.getAll().first().size shouldBe 2
    }
})
