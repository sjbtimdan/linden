package org.sjbtimdan.linden.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import org.sjbtimdan.linden.db.LindenDatabase
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency

/**
 * Populates a brand-new database with a starter set of categories and accounts so
 * the first launch is usable without manual setup. Seeding is skipped as soon as
 * the database holds any account, category or entry, so a user who deletes the
 * defaults does not get them back on the next launch.
 */
class DefaultDataSeeder(private val database: LindenDatabase) {

    suspend fun seedIfEmpty(defaultCurrency: Currency) {
        if (!isEmpty()) return
        database.transaction {
            DEFAULT_EXPENSE_CATEGORIES.forEach { (name, icon) ->
                database.categoryQueries.insert(name, CategoryType.Expense.name, icon.name)
            }
            DEFAULT_INCOME_CATEGORIES.forEach { (name, icon) ->
                database.categoryQueries.insert(name, CategoryType.Income.name, icon.name)
            }
            DEFAULT_ACCOUNTS.forEach { name ->
                database.accountQueries.insert(name, defaultCurrency.name, 0)
            }
        }
    }

    private suspend fun isEmpty(): Boolean = database.accountQueries.selectAll().awaitAsList().isEmpty() &&
        database.categoryQueries.selectAll().awaitAsList().isEmpty() &&
        database.entryQueries.selectAllRows().awaitAsList().isEmpty()
}

/** Starter expense categories, in seeding order. Also the cold-start suggestion fallback. */
val DEFAULT_EXPENSE_CATEGORIES = listOf(
    "Groceries" to CategoryIcon.ShoppingCart,
    "House" to CategoryIcon.Home,
    "Car" to CategoryIcon.DirectionsCar,
    "Restaurants" to CategoryIcon.Restaurant,
    "Bills" to CategoryIcon.AccountBalance,
    "Pets" to CategoryIcon.Pets,
    "Education" to CategoryIcon.School,
    "Holidays" to CategoryIcon.Flight,
    "Public Transport" to CategoryIcon.DirectionsBus,
)

/** Starter income categories, in seeding order. Also the cold-start suggestion fallback. */
val DEFAULT_INCOME_CATEGORIES = listOf(
    "Salary" to CategoryIcon.Savings,
    "Gifts" to CategoryIcon.FavoriteBorder,
    "Interest" to CategoryIcon.AccountBalance,
    "Refunds" to CategoryIcon.ShoppingBag,
)

/** Starter accounts, in seeding order. Also the cold-start suggestion fallback. */
val DEFAULT_ACCOUNTS = listOf("Savings Account", "Current Account")
