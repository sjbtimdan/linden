package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class LedgerFilterControlsTest : StringSpec({
    "type dropdown lists every type and reports the selection" {
        onTestMain {
            runComposeUiTest {
                var selected: EntryType? = null
                setControlsContent(onTypeFilterChange = { selected = it })

                onNodeWithTag("typeFilterDropdown").performClick()

                onNodeWithText("Expense").assertIsDisplayed()
                onNodeWithText("Income").assertIsDisplayed()
                onNodeWithText("Transfer").assertIsDisplayed()

                onNodeWithText("Transfer").performClick()

                selected shouldBe EntryType.Transfer
            }
        }
    }

    "shows the active type on the chip" {
        onTestMain {
            runComposeUiTest {
                setControlsContent(typeFilter = EntryType.Income)

                onNodeWithText("Income").assertIsDisplayed()
            }
        }
    }

    "entries view offers the amount filter; other views hide it" {
        onTestMain {
            runComposeUiTest {
                setControlsContent(showAmountFilter = true)

                onNodeWithTag("amountFilterChip").assertIsDisplayed()
            }
        }
    }

    "categories view hides the amount filter" {
        onTestMain {
            runComposeUiTest {
                setControlsContent(showAmountFilter = false)

                onNodeWithTag("typeFilterDropdown").assertIsDisplayed()
                onNodeWithTag("amountFilterChip").assertDoesNotExist()
            }
        }
    }

    "picking the All option reports a cleared type filter" {
        onTestMain {
            runComposeUiTest {
                var selected: EntryType? = EntryType.Income
                setControlsContent(
                    typeFilter = EntryType.Income,
                    onTypeFilterChange = { selected = it },
                )

                onNodeWithTag("typeFilterDropdown").performClick()
                onNodeWithText("Types: All").performClick()

                selected shouldBe null
            }
        }
    }

    "applying an amount from the popover reports the filter" {
        onTestMain {
            runComposeUiTest {
                var applied: AmountFilter? = null
                setControlsContent(onAmountFilterChange = { applied = it })

                onNodeWithTag("amountFilterChip").performClick()
                onNodeWithText("Filter by amount").assertIsDisplayed()
                onNodeWithText("<").performClick()
                onNodeWithTag("amountFilterValue").performTextInput("100")
                onNodeWithText("Apply").performClick()

                applied shouldBe AmountFilter(AmountOperator.LessThan, 10_000)
            }
        }
    }

    "shows the active amount filter and reports Clear" {
        onTestMain {
            runComposeUiTest {
                var cleared = false
                setControlsContent(
                    amountFilter = AmountFilter(AmountOperator.GreaterThan, 500),
                    onClearAmountFilter = { cleared = true },
                )

                onNodeWithText("> 5").assertIsDisplayed()

                onNodeWithTag("amountFilterChip").performClick()
                onNodeWithTag("clearAmountFilterButton").performClick()

                cleared shouldBe true
            }
        }
    }
})

/** Renders the inline filter row with the shared fixture data; every callback defaults to a no-op. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.setControlsContent(
    showAmountFilter: Boolean = true,
    typeFilter: EntryType? = null,
    amountFilter: AmountFilter? = null,
    onTypeFilterChange: (EntryType?) -> Unit = {},
    onAmountFilterChange: (AmountFilter?) -> Unit = {},
    onClearAmountFilter: () -> Unit = {},
) {
    setContent {
        LedgerFilterControls(
            typeFilter = typeFilter,
            onTypeFilterChange = onTypeFilterChange,
            showAmountFilter = showAmountFilter,
            amountFilter = amountFilter,
            onAmountFilterChange = onAmountFilterChange,
            onClearAmountFilter = onClearAmountFilter,
        )
    }
}
