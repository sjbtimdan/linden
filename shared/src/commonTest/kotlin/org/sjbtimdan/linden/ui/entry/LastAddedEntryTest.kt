package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.ui.onTestMain

private val main = Account(1, "Main", Currency.CHF)
private val savings = Account(2, "Savings", Currency.EUR)
private val groceries = Category(1, "Groceries", CategoryType.Expense)

@OptIn(ExperimentalTestApi::class)
class LastAddedEntryTest : StringSpec({

    "shows the added expense with its confirmation label" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    LastAddedEntry(entry = ExpenseEntry(1, groceries, "Coffee", main, 450), onClick = {})
                }

                onNodeWithTag("lastAddedEntry").assertIsDisplayed()
                onNodeWithText("Added").assertIsDisplayed()
                onNodeWithText("Coffee · Main").assertIsDisplayed()
                onNodeWithText("− 4.50 CHF").assertIsDisplayed()
                onNodeWithText("Undo").assertIsDisplayed()
            }
        }
    }

    "falls back to the category name when the expense has no description" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    LastAddedEntry(entry = ExpenseEntry(1, groceries, null, main, 450), onClick = {})
                }

                onNodeWithText("Groceries · Main").assertIsDisplayed()
            }
        }
    }

    "shows both accounts for an added transfer" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    LastAddedEntry(
                        entry = TransferEntry(1, null, "Move", main, 10_000, toAccount = savings, toAmount = 9_500),
                        onClick = {},
                    )
                }

                onNodeWithText("Move · Main → Savings").assertIsDisplayed()
                onNodeWithText("100.00 CHF").assertIsDisplayed()
            }
        }
    }

    "masks the receipt amount while keeping the title when amounts are hidden" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    LastAddedEntry(
                        entry = ExpenseEntry(1, groceries, "Coffee", main, 450),
                        onClick = {},
                        hideAmounts = true,
                    )
                }

                onNodeWithText("Coffee · Main").assertIsDisplayed()
                onNodeWithText("− 4.50 CHF").assertDoesNotExist()
                onNodeWithText("••••••").assertIsDisplayed()
            }
        }
    }

    "tapping the receipt undoes the add" {
        onTestMain {
            runComposeUiTest {
                var undone = false
                setContent {
                    LastAddedEntry(entry = ExpenseEntry(1, groceries, "Coffee", main, 450), onClick = { undone = true })
                }

                onNodeWithTag("lastAddedEntry").performClick()

                undone shouldBe true
            }
        }
    }

    "tapping the undo label undoes the add" {
        onTestMain {
            runComposeUiTest {
                var undone = false
                setContent {
                    LastAddedEntry(entry = ExpenseEntry(1, groceries, "Coffee", main, 450), onClick = { undone = true })
                }

                onNodeWithText("Undo").performClick()

                undone shouldBe true
            }
        }
    }
})
