package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.ui.onTestMain
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class EntryRowTest : StringSpec({
    val main = Account(1, "Main", Currency.CHF)
    val wallet = Account(2, "Wallet", Currency.EUR)
    val savings = Account(3, "Savings", Currency.USD)
    val groceries = Category(1, "Groceries", CategoryType.Expense)
    val salary = Category(2, "Salary", CategoryType.Income)
    val atHalfPastTwo = Instant.fromEpochMilliseconds(14 * 3_600_000 + 30 * 60_000)

    "expense row shows description, account, date and a minus-signed amount" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    EntryRow(
                        entry = ExpenseEntry(
                            id = 1,
                            category = groceries,
                            description = "Coffee",
                            account = main,
                            amount = 450,
                            createdAt = atHalfPastTwo,
                            createdZone = TimeZone.UTC,
                        ),
                        onClick = {},
                    )
                }

                onNodeWithText("Coffee").assertIsDisplayed()
                onNodeWithText("Main").assertIsDisplayed()
                onNodeWithText("1 Jan 1970, 14:30").assertIsDisplayed()
                onNodeWithText("− 4.50 CHF").assertIsDisplayed()
            }
        }
    }

    "expense with a blank description falls back to the category name" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    EntryRow(
                        entry = ExpenseEntry(
                            id = 1,
                            category = groceries,
                            description = "   ",
                            account = main,
                            amount = 450,
                        ),
                        onClick = {},
                    )
                }

                onNodeWithText("Groceries").assertIsDisplayed()
            }
        }
    }

    "hides the date line when showTimestamp is false" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    EntryRow(
                        entry = ExpenseEntry(
                            id = 1,
                            category = groceries,
                            description = "Coffee",
                            account = main,
                            amount = 450,
                            createdAt = atHalfPastTwo,
                            createdZone = TimeZone.UTC,
                        ),
                        onClick = {},
                        showTimestamp = false,
                    )
                }

                onNodeWithText("Coffee").assertIsDisplayed()
                onNodeWithText("Main").assertIsDisplayed()
                onNodeWithText("1 Jan 1970, 14:30").assertDoesNotExist()
                onNodeWithText("− 4.50 CHF").assertIsDisplayed()
            }
        }
    }

    "income row shows a plus-signed amount with the account currency symbol" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    EntryRow(
                        entry = IncomeEntry(
                            id = 2,
                            category = salary,
                            description = "Salary",
                            account = wallet,
                            amount = 450,
                        ),
                        onClick = {},
                    )
                }

                onNodeWithText("+ 4.50 €").assertIsDisplayed()
            }
        }
    }

    "transfer row shows both accounts and an unsigned amount" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    EntryRow(
                        entry = TransferEntry(
                            id = 3,
                            category = null,
                            description = null,
                            account = main,
                            amount = 10_000,
                            toAccount = savings,
                            toAmount = 7_500,
                        ),
                        onClick = {},
                    )
                }

                onNodeWithText("Transfer").assertIsDisplayed()
                onNodeWithText("Main → Savings").assertIsDisplayed()
                onNodeWithText("100.00 CHF").assertIsDisplayed()
            }
        }
    }

    "transfer with a description uses it as the title" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    EntryRow(
                        entry = TransferEntry(
                            id = 3,
                            category = null,
                            description = "Rent split",
                            account = main,
                            amount = 10_000,
                            toAccount = savings,
                            toAmount = 7_500,
                        ),
                        onClick = {},
                    )
                }

                onNodeWithText("Rent split").assertIsDisplayed()
                onNodeWithText("Transfer").assertDoesNotExist()
            }
        }
    }

    "clicking the row invokes onClick" {
        onTestMain {
            runComposeUiTest {
                var clicks = 0
                setContent {
                    EntryRow(
                        entry = ExpenseEntry(
                            id = 1,
                            category = groceries,
                            description = "Coffee",
                            account = main,
                            amount = 450,
                        ),
                        onClick = { clicks++ },
                    )
                }

                onNodeWithText("Coffee").performClick()

                clicks shouldBe 1
            }
        }
    }
})
