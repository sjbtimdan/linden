package org.sjbtimdan.linden.ui.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.onTestMain
import kotlin.time.Instant

private val main = Account(1, "Main", Currency.CHF)
private val groceries = Category(1, "Groceries", CategoryType.Expense)

private fun draft(amountText: String): EntryDraft = EntryDraft(
    editing = null,
    type = EntryType.Expense,
    amountText = amountText,
    categoryId = groceries.id,
    accountId = main.id,
    toAccountId = null,
    toAmountText = "",
    description = "",
    createdAt = Instant.parse("2026-08-10T14:30:00Z"),
    createdZone = TimeZone.UTC,
)

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.showForm(initial: EntryDraft, onAmountChange: (String) -> Unit = {}) {
    setContent {
        var state by remember { mutableStateOf(initial) }
        EntryForm(
            state = state,
            accounts = listOf(main),
            categories = listOf(groceries),
            onAmountChange = {
                onAmountChange(it)
                state = state.copy(amountText = it)
            },
            onCategoryChange = {},
            onAccountChange = {},
            onToAccountChange = {},
            onToAmountChange = {},
            onDescriptionChange = {},
            onCreatedAtChange = {},
            onNavigateToSettings = {},
        )
    }
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.openCalculator() {
    onNodeWithTag("amountField").performClick()
    waitForIdle()
    onNodeWithText("Enter").assertIsDisplayed()
}

@OptIn(ExperimentalTestApi::class)
class AmountCalculatorTest : StringSpec({

    "entering a calculation commits the result" {
        onTestMain {
            runComposeUiTest {
                var committed: String? = null
                showForm(draft(""), onAmountChange = { committed = it })
                openCalculator()

                onNodeWithText("1").performClick()
                onNodeWithText("0").performClick()
                onNodeWithText("0").performClick()
                onNodeWithText("÷").performClick()
                onNodeWithText("3").performClick()
                onNodeWithText("=").performClick()
                onNodeWithText("33.33").assertIsDisplayed()

                onNodeWithText("Enter").performClick()
                waitForIdle()

                committed shouldBe "33.33"
                onNodeWithText("Enter").assertDoesNotExist()
            }
        }
    }

    "Enter evaluates a pending expression before committing" {
        onTestMain {
            runComposeUiTest {
                var committed: String? = null
                showForm(draft(""), onAmountChange = { committed = it })
                openCalculator()

                onNodeWithText("1").performClick()
                onNodeWithText("0").performClick()
                onNodeWithText("0").performClick()
                onNodeWithText("+").performClick()
                onNodeWithText("3").performClick()
                onNodeWithText("Enter").performClick()
                waitForIdle()

                committed shouldBe "103.00"
            }
        }
    }

    "Enter with zero closes the calculator and warns" {
        onTestMain {
            runComposeUiTest {
                var committed: String? = null
                showForm(draft(""), onAmountChange = { committed = it })
                openCalculator()

                onNodeWithText("Enter").performClick()
                waitForIdle()

                committed.shouldBeNull()
                onNodeWithText("Amount must be greater than zero").assertIsDisplayed()
                onNodeWithText("Enter").assertDoesNotExist()
            }
        }
    }

    "Escape closes the calculator without committing" {
        onTestMain {
            runComposeUiTest {
                var committed: String? = null
                showForm(draft("10.00"), onAmountChange = { committed = it })
                openCalculator()

                onNodeWithText("1").performClick()
                onNodeWithText("2").performClick()
                onRoot().performKeyInput { pressKey(Key.Escape) }
                waitForIdle()

                committed.shouldBeNull()
                onNodeWithText("10.00").assertIsDisplayed()
                onNodeWithText("Enter").assertDoesNotExist()
            }
        }
    }

    "keypad hugs the bottom of the available space without stretching the keys" {
        onTestMain {
            runComposeUiTest {
                showForm(draft(""))
                openCalculator()

                val rootBounds = onRoot().getBoundsInRoot()
                val enterBounds = onNodeWithText("Enter").getBoundsInRoot()
                val keyBounds = onNodeWithText("7").getBoundsInRoot()

                // The Enter button sits at the bottom of the available space,
                // with only the keypad's 8.dp outer padding below it.
                (rootBounds.bottom - enterBounds.bottom) shouldBeLessThan 24.dp
                // Keys cap out at 56.dp instead of stretching to fill the space.
                keyBounds.height shouldBeLessThanOrEqualTo 57.dp
            }
        }
    }
})
