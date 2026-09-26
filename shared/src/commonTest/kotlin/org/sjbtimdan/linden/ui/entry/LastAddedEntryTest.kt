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
                    LastAddedEntry(entry = ExpenseEntry(1, groceries, "Coffee", main, 450), onUndo = {}, onDismiss = {})
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
                    LastAddedEntry(entry = ExpenseEntry(1, groceries, null, main, 450), onUndo = {}, onDismiss = {})
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
                        onUndo = {},
                        onDismiss = {},
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
                        onUndo = {},
                        onDismiss = {},
                        hideAmounts = true,
                    )
                }

                onNodeWithText("Coffee · Main").assertIsDisplayed()
                onNodeWithText("− 4.50 CHF").assertDoesNotExist()
                onNodeWithText("••••••").assertIsDisplayed()
            }
        }
    }

    "tapping the receipt body does not undo the add" {
        onTestMain {
            runComposeUiTest {
                var undone = false
                setContent {
                    LastAddedEntry(
                        entry = ExpenseEntry(1, groceries, "Coffee", main, 450),
                        onUndo = { undone = true },
                        onDismiss = {},
                    )
                }

                onNodeWithTag("lastAddedEntry").performClick()

                undone shouldBe false
            }
        }
    }

    "tapping the confirmation icon dismisses without undoing" {
        onTestMain {
            runComposeUiTest {
                var undone = false
                var dismissed = false
                setContent {
                    LastAddedEntry(
                        entry = ExpenseEntry(1, groceries, "Coffee", main, 450),
                        onUndo = { undone = true },
                        onDismiss = { dismissed = true },
                    )
                }

                onNodeWithTag("dismissAddedEntry").performClick()

                dismissed shouldBe true
                undone shouldBe false
            }
        }
    }

    "tapping the undo action undoes the add" {
        onTestMain {
            runComposeUiTest {
                var undone = false
                setContent {
                    LastAddedEntry(
                        entry = ExpenseEntry(1, groceries, "Coffee", main, 450),
                        onUndo = { undone = true },
                        onDismiss = {},
                    )
                }

                onNodeWithTag("undoAddedEntry").performClick()

                undone shouldBe true
            }
        }
    }
})
