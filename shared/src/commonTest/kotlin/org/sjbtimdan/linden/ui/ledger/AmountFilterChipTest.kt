package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

private val chipTag = "amountFilterChip"
private val valueTag = "amountFilterValue"

@OptIn(ExperimentalTestApi::class)
class AmountFilterChipTest : StringSpec({
    "shows Amount: All when no filter is set" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    AmountFilterChip(
                        filter = null,
                        onApply = {},
                        onClear = {},
                        modifier = Modifier.testTag(chipTag),
                    )
                }

                onNodeWithText("Amount: All").assertIsDisplayed()
                onNodeWithTag(chipTag).assertIsNotSelected()
            }
        }
    }

    "shows the active filter on the chip" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    AmountFilterChip(
                        filter = AmountFilter(AmountOperator.GreaterThan, 500),
                        onApply = {},
                        onClear = {},
                        modifier = Modifier.testTag(chipTag),
                    )
                }

                onNodeWithText("> 5").assertIsDisplayed()
                onNodeWithTag(chipTag).assertIsSelected()
            }
        }
    }

    "clicking the chip opens the popover with operators and an amount field" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    AmountFilterChip(
                        filter = null,
                        onApply = {},
                        onClear = {},
                        modifier = Modifier.testTag(chipTag),
                    )
                }

                onNodeWithTag(chipTag).performClick()

                onNodeWithText("Filter by amount").assertIsDisplayed()
                onNodeWithText(">").assertIsDisplayed()
                onNodeWithText("<").assertIsDisplayed()
                onNodeWithText("~").assertIsDisplayed()
                onNodeWithTag(valueTag).assertIsDisplayed()
            }
        }
    }

    "applying an operator and amount invokes onApply" {
        onTestMain {
            runComposeUiTest {
                var applied: AmountFilter? = null
                setContent {
                    AmountFilterChip(
                        filter = null,
                        onApply = { applied = it },
                        onClear = {},
                        modifier = Modifier.testTag(chipTag),
                    )
                }

                onNodeWithTag(chipTag).performClick()
                onNodeWithText("<").performClick()
                onNodeWithTag(valueTag).performTextInput("100")
                onNodeWithText("Apply").performClick()

                applied shouldBe AmountFilter(AmountOperator.LessThan, 10_000)
            }
        }
    }

    "clear invokes onClear when a filter is set" {
        onTestMain {
            runComposeUiTest {
                var cleared = false
                setContent {
                    AmountFilterChip(
                        filter = AmountFilter(AmountOperator.GreaterThan, 500),
                        onApply = {},
                        onClear = { cleared = true },
                        modifier = Modifier.testTag(chipTag),
                    )
                }

                onNodeWithTag(chipTag).performClick()
                onNodeWithText("Clear").performClick()

                cleared shouldBe true
            }
        }
    }
})
