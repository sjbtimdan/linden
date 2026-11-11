package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
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
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.ui.onTestMain
import kotlin.time.Instant

private val now = Instant.parse("2026-08-10T14:30:00Z")
private val main = Account(1, "Main", Currency.CHF)
private val groceries = Category(1, "Groceries", CategoryType.Expense)

private fun draft(editing: Boolean = false): EntryDraft = EntryDraft(
    editing = if (editing) {
        ExpenseEntry(1, groceries, "Coffee", main, 450, createdAt = now, createdZone = TimeZone.UTC)
    } else {
        null
    },
    type = EntryType.Expense,
    amountText = "4.50",
    categoryId = groceries.id,
    accountId = main.id,
    toAccountId = null,
    toAmountText = "",
    description = "",
    createdAt = now,
    createdZone = TimeZone.UTC,
)

@OptIn(ExperimentalTestApi::class)
class EntryActionsRowTest : StringSpec({

    "add is disabled while the form is invalid" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    EntryActionsRow(draft = null, addEnabled = false, onAdd = {}, onClear = {})
                }

                onNodeWithTag("saveEntry").assertIsNotEnabled()
            }
        }
    }

    "editing an entry labels add as save" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    EntryActionsRow(draft = draft(editing = true), addEnabled = true, onAdd = {}, onClear = {})
                }

                onNodeWithText("Save").assertIsEnabled()
            }
        }
    }

    "add and clear run their callbacks" {
        onTestMain {
            runComposeUiTest {
                var added = 0
                var cleared = 0
                setContent {
                    EntryActionsRow(
                        draft = draft(),
                        addEnabled = true,
                        onAdd = { added++ },
                        onClear = { cleared++ },
                    )
                }

                onNodeWithText("Add").performClick()
                onNodeWithText("Clear").performClick()

                added shouldBe 1
                cleared shouldBe 1
            }
        }
    }
})
