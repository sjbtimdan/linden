package org.sjbtimdan.linden.ui.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.ui.onTestMain
import kotlin.time.Instant

private val main = Account(1, "Main", Currency.CHF)
private val groceries = Category(1, "Groceries", CategoryType.Expense)
private val accounts = listOf(main)
private val categories = listOf(groceries)

private fun draft(
    type: EntryType = EntryType.Expense,
    editing: Entry? = null,
    amountText: String = "10.00",
    createdAt: Instant = Instant.parse("2026-08-10T14:30:00Z"),
    createdZone: TimeZone = TimeZone.UTC,
): EntryDraft = EntryDraft(
    editing = editing,
    type = type,
    amountText = amountText,
    categoryId = if (type == EntryType.Transfer) null else groceries.id,
    accountId = main.id,
    toAccountId = null,
    toAmountText = "",
    description = "",
    createdAt = createdAt,
    createdZone = createdZone,
)

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.showDialog(state: EntryDraft, onSave: () -> Unit = {}, onDelete: (() -> Unit)? = null) {
    setContent {
        EntryDialog(
            state = state,
            accounts = accounts,
            categories = categories,
            onAmountChange = {},
            onCategoryChange = {},
            onAccountChange = {},
            onToAccountChange = {},
            onToAmountChange = {},
            onDescriptionChange = {},
            onCreatedAtChange = {},
            onSave = onSave,
            onDelete = onDelete,
            onNavigateToSettings = {},
            onDismiss = {},
        )
    }
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.showForm(initial: EntryDraft, onCreatedAtChange: (Instant) -> Unit = {}) {
    setContent {
        var state by remember { mutableStateOf(initial) }
        EntryForm(
            state = state,
            accounts = accounts,
            categories = categories,
            onAmountChange = { state = state.copy(amountText = it) },
            onCategoryChange = { state = state.copy(categoryId = it) },
            onAccountChange = { state = state.copy(accountId = it) },
            onToAccountChange = { state = state.copy(toAccountId = it) },
            onToAmountChange = { state = state.copy(toAmountText = it) },
            onDescriptionChange = { state = state.copy(description = it) },
            onCreatedAtChange = {
                state = state.copy(createdAt = it)
                onCreatedAtChange(it)
            },
            onNavigateToSettings = {},
        )
    }
}

@OptIn(ExperimentalTestApi::class)
class EntryDialogTest : StringSpec({

    "shows New and Edit titles based on the draft type" {
        onTestMain {
            runComposeUiTest {
                showDialog(draft())
                onNodeWithText("New Expense").assertIsDisplayed()
            }
            runComposeUiTest {
                showDialog(draft(type = EntryType.Income))
                onNodeWithText("New Income").assertIsDisplayed()
            }
            runComposeUiTest {
                showDialog(draft(type = EntryType.Transfer))
                onNodeWithText("New Transfer").assertIsDisplayed()
            }
            runComposeUiTest {
                showDialog(draft(editing = ExpenseEntry(7, groceries, "Coffee", main, 450)))
                onNodeWithText("Edit Expense").assertIsDisplayed()
            }
        }
    }

    "enables Save only for a valid draft" {
        onTestMain {
            runComposeUiTest {
                showDialog(draft(amountText = ""))
                onNodeWithText("Save").assertIsNotEnabled()
            }
            runComposeUiTest {
                showDialog(draft())
                onNodeWithText("Save").assertIsEnabled()
            }
        }
    }

    "shows Delete only when editing an entry" {
        onTestMain {
            runComposeUiTest {
                showDialog(draft())
                onNodeWithText("Delete").assertDoesNotExist()
            }
            runComposeUiTest {
                showDialog(draft(editing = ExpenseEntry(7, groceries, "Coffee", main, 450)), onDelete = {})
                onNodeWithText("Delete").assertIsDisplayed()
            }
        }
    }

    "changing the date in the date picker keeps the time of day" {
        onTestMain {
            runComposeUiTest {
                var changedTo: Instant? = null
                showForm(draft(), onCreatedAtChange = { changedTo = it })

                onNodeWithText("10 Aug 2026").performClick()
                waitForIdle()
                onNodeWithText("Saturday, August 15, 2026").performClick()
                onNodeWithText("OK").performClick()
                waitForIdle()

                changedTo shouldBe Instant.parse("2026-08-15T14:30:00Z")
                onNodeWithText("15 Aug 2026").assertIsDisplayed()
                onNodeWithText("14:30").assertIsDisplayed()
            }
        }
    }

    "interprets the picked date in the draft's zone" {
        onTestMain {
            runComposeUiTest {
                var changedTo: Instant? = null
                showForm(
                    draft(createdZone = TimeZone.of("Europe/Zurich")),
                    onCreatedAtChange = { changedTo = it },
                )

                onNodeWithText("10 Aug 2026").performClick()
                waitForIdle()
                onNodeWithText("Saturday, August 15, 2026").performClick()
                onNodeWithText("OK").performClick()
                waitForIdle()

                // The 16:30 local time (CEST, UTC+2) survives the date change.
                changedTo shouldBe Instant.parse("2026-08-15T14:30:00Z")
                onNodeWithText("16:30").assertIsDisplayed()
            }
        }
    }

    "cancelling the date picker leaves the draft unchanged" {
        onTestMain {
            runComposeUiTest {
                var changedTo: Instant? = null
                showForm(draft(), onCreatedAtChange = { changedTo = it })

                onNodeWithText("10 Aug 2026").performClick()
                waitForIdle()
                onNodeWithText("Cancel").performClick()
                waitForIdle()

                changedTo.shouldBeNull()
                onNodeWithText("10 Aug 2026").assertIsDisplayed()
                onNodeWithText("14:30").assertIsDisplayed()
            }
        }
    }

    "changing the time in the time picker keeps the date" {
        onTestMain {
            runComposeUiTest {
                var changedTo: Instant? = null
                showForm(draft(), onCreatedAtChange = { changedTo = it })

                onNodeWithText("14:30").performClick()
                waitForIdle()
                onNodeWithContentDescription("15 hours").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithContentDescription("30 minutes").fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithText("OK").performClick()
                waitForIdle()

                changedTo shouldBe Instant.parse("2026-08-10T15:30:00Z")
                onNodeWithText("15:30").assertIsDisplayed()
                onNodeWithText("10 Aug 2026").assertIsDisplayed()
            }
        }
    }

    "interprets the picked time in the draft's zone" {
        onTestMain {
            runComposeUiTest {
                var changedTo: Instant? = null
                showForm(
                    draft(createdZone = TimeZone.of("Europe/Zurich")),
                    onCreatedAtChange = { changedTo = it },
                )

                onNodeWithText("16:30").performClick()
                waitForIdle()
                onNodeWithContentDescription("17 hours").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithContentDescription("30 minutes").fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithText("OK").performClick()
                waitForIdle()

                // 17:30 local time (CEST, UTC+2) is 15:30 UTC.
                changedTo shouldBe Instant.parse("2026-08-10T15:30:00Z")
                onNodeWithText("17:30").assertIsDisplayed()
            }
        }
    }

    "cancelling the time picker leaves the draft unchanged" {
        onTestMain {
            runComposeUiTest {
                var changedTo: Instant? = null
                showForm(draft(), onCreatedAtChange = { changedTo = it })

                onNodeWithText("14:30").performClick()
                waitForIdle()
                onNodeWithText("Cancel").performClick()
                waitForIdle()

                changedTo.shouldBeNull()
                onNodeWithText("14:30").assertIsDisplayed()
                onNodeWithText("10 Aug 2026").assertIsDisplayed()
            }
        }
    }
})
