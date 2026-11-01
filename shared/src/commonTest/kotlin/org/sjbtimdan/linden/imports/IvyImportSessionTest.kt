package org.sjbtimdan.linden.imports

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.db.LindenDatabase
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType

class IvyImportSessionTest : StringSpec({
    "resolves a same-currency transfer without a received amount" {
        withSession {
            val resolved = session.resolveTransaction(
                IvyTransaction(
                    type = "TRANSFER",
                    amount = 1_000,
                    accountId = "a1",
                    categoryId = "c1",
                    title = "Move",
                    toAccountId = "a3",
                ),
                createdAt = 123L,
            )

            resolved.shouldNotBeNull()
            resolved.type shouldBe EntryType.Transfer
            resolved.account shouldBe main
            resolved.toAccount shouldBe cash
            resolved.toAmount shouldBe null
            resolved.categoryId.shouldNotBeNull()
            session.splitTransactions shouldBe 0
        }
    }

    "resolves a cross-currency transfer with the received amount" {
        withSession {
            val resolved = session.resolveTransaction(
                IvyTransaction(
                    type = "TRANSFER",
                    amount = 1_000,
                    accountId = "a1",
                    title = "Move",
                    toAccountId = "a2",
                    toAmount = 900,
                ),
                createdAt = 0L,
            )

            resolved.shouldNotBeNull()
            resolved.toAccount shouldBe savings
            resolved.toAmount shouldBe 900
        }
    }

    "throws when a cross-currency transfer has no received amount" {
        withSession {
            shouldThrow<IvyImportException> {
                session.resolveTransaction(
                    IvyTransaction(
                        type = "TRANSFER",
                        amount = 1_000,
                        accountId = "a1",
                        title = "Move",
                        toAccountId = "a2",
                    ),
                    createdAt = 0L,
                )
            }
        }
    }

    "throws for an unknown transaction type" {
        withSession {
            shouldThrow<IvyImportException> {
                session.resolveTransaction(
                    IvyTransaction(type = "REFUND", amount = 100, accountId = "a1"),
                    createdAt = 0L,
                )
            }
        }
    }

    "falls back to a per-currency account and reuses it" {
        withSession {
            val transaction = IvyTransaction(
                type = "EXPENSE",
                amount = 100,
                accountId = "missing",
                categoryId = "c1",
                currency = "EUR",
            )

            val first = session.resolveTransaction(transaction, createdAt = 0L)
            first.shouldNotBeNull()
            first.account.name shouldBe "Imported Account (EUR)"
            session.fallbackAccountsCreated shouldBe 1

            val second = session.resolveTransaction(transaction, createdAt = 0L)
            second.shouldNotBeNull()
            second.account shouldBe first.account
            session.fallbackAccountsCreated shouldBe 1
        }
    }

    "splits an account whose transaction currency conflicts and reuses the split" {
        withSession {
            val transaction = IvyTransaction(
                type = "EXPENSE",
                amount = 100,
                accountId = "a1",
                categoryId = "c1",
                currency = "USD",
            )

            val first = session.resolveTransaction(transaction, createdAt = 0L)
            first.shouldNotBeNull()
            first.account.name shouldBe "IVY: Main (USD)"
            first.account.currency shouldBe Currency.USD
            session.splitAccountsCreated shouldBe 1

            val second = session.resolveTransaction(transaction, createdAt = 0L)
            second.shouldNotBeNull()
            second.account shouldBe first.account
            session.splitAccountsCreated shouldBe 1
        }
    }

    "creates one fallback category for uncategorised entries" {
        withSession {
            val transaction = IvyTransaction(
                type = "EXPENSE",
                amount = 100,
                accountId = "a1",
                title = "Unknown merchant",
            )

            val first = session.resolveTransaction(transaction, createdAt = 0L)
            first.shouldNotBeNull()
            first.categoryId.shouldNotBeNull()
            session.fallbackCategoryCreated shouldBe true

            val second = session.resolveTransaction(transaction, createdAt = 0L)
            second.shouldNotBeNull()
            second.categoryId shouldBe first.categoryId
        }
    }

    "applies an initial balance once per account" {
        withSession {
            val balance = IvyTransaction(
                type = "INCOME",
                amount = 5_000,
                accountId = "a1",
                title = "initial balance",
            )

            session.resolveTransaction(balance, createdAt = 0L).shouldBeNull()

            database.accountQueries.selectByName("Main").awaitAsOneOrNull()?.initialBalance shouldBe 5_000

            val again = session.resolveTransaction(balance, createdAt = 0L)
            again.shouldNotBeNull()
            again.type shouldBe EntryType.Income
            database.accountQueries.selectByName("Main").awaitAsOneOrNull()?.initialBalance shouldBe 5_000
        }
    }

    "applies an expense initial balance as a negative amount" {
        withSession {
            val resolved = session.resolveTransaction(
                IvyTransaction(
                    type = "EXPENSE",
                    amount = 2_500,
                    accountId = "a1",
                    title = "adjust balance",
                ),
                createdAt = 0L,
            )

            resolved.shouldBeNull()
            database.accountQueries.selectByName("Main").awaitAsOneOrNull()?.initialBalance shouldBe -2_500
        }
    }

    "uses the source currency for a transfer target with no explicit currency" {
        withSession {
            val resolved = session.resolveTransaction(
                IvyTransaction(
                    type = "TRANSFER",
                    amount = 100,
                    accountId = "a2",
                    title = "Move",
                    toAccountId = "missing",
                ),
                createdAt = 0L,
            )

            resolved.shouldNotBeNull()
            resolved.toAccount?.name shouldBe "Imported Account (USD)"
            resolved.toAmount shouldBe null
        }
    }

    "counts a split balance entry once" {
        withSession {
            val resolved = session.resolveTransaction(
                IvyTransaction(
                    type = "INCOME",
                    amount = 1_000,
                    accountId = "a1",
                    title = "initial balance",
                    currency = "USD",
                ),
                createdAt = 0L,
            )

            resolved.shouldBeNull()
            session.splitAccountsCreated shouldBe 1
            session.splitTransactions shouldBe 1
        }
    }
})

private class SessionFixture(
    val database: LindenDatabase,
    val session: IvyImportSession,
    val main: ResolvedAccount,
    val savings: ResolvedAccount,
    val cash: ResolvedAccount,
)

private fun withSession(block: suspend SessionFixture.() -> Unit) {
    runBlocking {
        val database = lindenDatabase()
        val main = insertResolvedAccount(database, "Main", Currency.CHF)
        val savings = insertResolvedAccount(database, "Savings", Currency.USD)
        val cash = insertResolvedAccount(database, "Cash", Currency.CHF)
        val groceries = insertIvyCategory(database, "Groceries", CategoryType.Expense, icon = null)
        val session = IvyImportSession(
            database = database,
            accounts = mapOf("a1" to main, "a2" to savings, "a3" to cash),
            categories = mapOf("c1" to groceries),
            backupCategories = listOf(IvyCategory("c1", "Groceries")),
            defaultCurrency = Currency.CHF,
        )
        SessionFixture(database, session, main, savings, cash).block()
    }
}
