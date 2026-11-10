package org.sjbtimdan.linden.data

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.db.LindenDatabase
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency

@Suppress("KotlinNoActualForExpect")
expect fun createTestSqlDriver(): SqlDriver

suspend fun lindenDatabase(): LindenDatabase = createLindenDatabase(createTestSqlDriver())

/** The "Main" CHF account and "Groceries" expense category most screen tests start from. */
internal suspend fun seed(accountDao: AccountDao, categoryDao: CategoryDao): Pair<Account, Category> {
    accountDao.create("Main", Currency.CHF)
    categoryDao.create("Groceries", CategoryType.Expense)
    val main = accountDao.getAll().first().first()
    val groceries = categoryDao.getAll().first().first()
    return main to groceries
}
