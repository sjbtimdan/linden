package org.sjbtimdan.linden.ui.ledger

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class LedgerFilterControlsTest : StringSpec({
    "type chips offer All and every type and report the selection" {
        onTestMain {
            runComposeUiTest {
                var selected: EntryType? = EntryType.Expense
                setControlsContent(onTypeFilterChange = { selected = it })

                onNodeWithTag("typeFilter-All").assertIsDisplayed()
                onNodeWithTag("typeFilter-Expense").assertIsDisplayed()
                onNodeWithTag("typeFilter-Income").assertIsDisplayed()
                onNodeWithTag("typeFilter-Transfer").assertIsDisplayed()

                onNodeWithTag("typeFilter-Transfer").performClick()

                selected shouldBe EntryType.Transfer
            }
        }
    }

    "All is the default type selection" {
        onTestMain {
            runComposeUiTest {
                setControlsContent()

                onNodeWithTag("typeFilter-All").assertIsSelected()
                onNodeWithTag("typeFilter-Expense").assertIsNotSelected()
            }
        }
    }

    "type chips run All, Expense, Income, Transfer left to right" {
        onTestMain {
            runComposeUiTest {
                setControlsContent(showAmountFilter = false)

                val all = onNodeWithTag("typeFilter-All").getUnclippedBoundsInRoot()
                val expense = onNodeWithTag("typeFilter-Expense").getUnclippedBoundsInRoot()
                val income = onNodeWithTag("typeFilter-Income").getUnclippedBoundsInRoot()
                val transfer = onNodeWithTag("typeFilter-Transfer").getUnclippedBoundsInRoot()

                // The chips share one row, ordered like the entry screen's type selector.
                expense.top shouldBe all.top
                income.top shouldBe all.top
                transfer.top shouldBe all.top
                expense.left shouldBeGreaterThan all.left
                income.left shouldBeGreaterThan expense.left
                transfer.left shouldBeGreaterThan income.left
            }
        }
    }

    "without the transfer chip a Transfer filter reads back as All selected" {
        onTestMain {
            runComposeUiTest {
                setControlsContent(
                    typeOptions = listOf(EntryType.Expense, EntryType.Income),
                    typeFilter = EntryType.Transfer,
                )

                onNodeWithTag("typeFilter-All").assertIsSelected()
                onNodeWithTag("typeFilter-Expense").assertIsNotSelected()
                onNodeWithTag("typeFilter-Income").assertIsNotSelected()
                onNodeWithTag("typeFilter-Transfer").assertDoesNotExist()
            }
        }
    }

    "without the transfer chip the offered types still report their selection" {
        onTestMain {
            runComposeUiTest {
                var selected: EntryType? = null
                setControlsContent(
                    typeOptions = listOf(EntryType.Expense, EntryType.Income),
                    onTypeFilterChange = { selected = it },
                )

                onNodeWithTag("typeFilter-Income").performClick()

                selected shouldBe EntryType.Income
            }
        }
    }

    "the active type reads back as the selected chip" {
        onTestMain {
            runComposeUiTest {
                setControlsContent(typeFilter = EntryType.Income)

                onNodeWithTag("typeFilter-Income").assertIsSelected()
                onNodeWithTag("typeFilter-All").assertIsNotSelected()
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

                onNodeWithTag("typeFilter-All").assertIsDisplayed()
                onNodeWithTag("amountFilterChip").assertDoesNotExist()
            }
        }
    }

    "picking All reports a cleared type filter" {
        onTestMain {
            runComposeUiTest {
                var selected: EntryType? = EntryType.Income
                setControlsContent(
                    typeFilter = EntryType.Income,
                    onTypeFilterChange = { selected = it },
                )

                onNodeWithTag("typeFilter-All").performClick()

                selected shouldBe null
                onNodeWithTag("typeFilter-All").assertIsSelected()
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
    typeOptions: List<EntryType> = typeOrder,
    amountFilter: AmountFilter? = null,
    onTypeFilterChange: (EntryType?) -> Unit = {},
    onAmountFilterChange: (AmountFilter?) -> Unit = {},
    onClearAmountFilter: () -> Unit = {},
) {
    setContent {
        // The controls are stateless; the fixture hoists the type like the
        // screen's ViewModel does, so chips reflect a change immediately.
        var type by remember { mutableStateOf(typeFilter) }
        LedgerFilterControls(
            typeFilter = type,
            onTypeFilterChange = {
                type = it
                onTypeFilterChange(it)
            },
            typeOptions = typeOptions,
            showAmountFilter = showAmountFilter,
            amountFilter = amountFilter,
            onAmountFilterChange = onAmountFilterChange,
            onClearAmountFilter = onClearAmountFilter,
        )
    }
}
