package org.sjbtimdan.linden.ui.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

private val options = listOf("All", "Income", "Expense")
private val chipTag = "chipDropdown"

private data class ChipOption(val id: Long, val name: String)

@OptIn(ExperimentalTestApi::class)
class ChipDropdownTest : StringSpec({
    "shows the current selection on the chip" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    ChipDropdown(
                        selected = "Income",
                        options = options,
                        optionLabel = { it },
                        onSelect = {},
                        modifier = Modifier.testTag(chipTag),
                    )
                }

                onNodeWithText("Income").assertIsDisplayed()
                onNodeWithTag(chipTag).assertIsSelected()
            }
        }
    }

    "clicking the chip opens the menu listing every option" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    ChipDropdown(
                        selected = "All",
                        options = options,
                        optionLabel = { it },
                        onSelect = {},
                        modifier = Modifier.testTag(chipTag),
                    )
                }

                onNodeWithTag(chipTag).performClick()

                options.forEach { option ->
                    // The selected option reads the same on the chip and in the menu;
                    // the menu item is the one carrying the "Selected" check.
                    if (option == "All") {
                        onNode(hasText("All") and hasContentDescription("Selected")).assertIsDisplayed()
                    } else {
                        onNodeWithText(option).assertIsDisplayed()
                    }
                }
            }
        }
    }

    "selecting an option invokes onSelect with the underlying item" {
        onTestMain {
            runComposeUiTest {
                var picked: ChipOption? = null
                val chipOptions = listOf(ChipOption(1, "Main"), ChipOption(2, "Savings"))
                setContent {
                    ChipDropdown(
                        selected = chipOptions.first(),
                        options = chipOptions,
                        optionLabel = { it.name },
                        onSelect = { picked = it },
                        modifier = Modifier.testTag(chipTag),
                    )
                }

                onNodeWithTag(chipTag).performClick()
                onNodeWithText("Savings").performClick()

                picked shouldBe ChipOption(2, "Savings")
            }
        }
    }

    "the chip shows the new selection and the menu closes after picking" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    var selected by remember { mutableStateOf("All") }
                    ChipDropdown(
                        selected = selected,
                        options = options,
                        optionLabel = { it },
                        onSelect = { selected = it },
                        modifier = Modifier.testTag(chipTag),
                    )
                }

                onNodeWithTag(chipTag).performClick()
                onNodeWithText("Expense").performClick()

                // Chip label updated; the menu is gone (no other option visible).
                onNodeWithText("Expense").assertIsDisplayed()
                onNodeWithText("Income").assertDoesNotExist()
                onNodeWithText("All").assertDoesNotExist()
            }
        }
    }

    "the selected option is the only one marked with a check" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    var selected by remember { mutableStateOf("All") }
                    ChipDropdown(
                        selected = selected,
                        options = options,
                        optionLabel = { it },
                        onSelect = { selected = it },
                        modifier = Modifier.testTag(chipTag),
                    )
                }

                onNodeWithTag(chipTag).performClick()
                onNodeWithText("Expense").performClick()

                // Reopen: the check has moved to the new selection, exactly one item is marked.
                onNodeWithTag(chipTag).performClick()
                onNodeWithContentDescription("Selected").assertIsDisplayed()
                onNode(hasText("Expense") and hasContentDescription("Selected")).assertIsDisplayed()
                onNode(hasText("Income") and hasContentDescription("Selected")).assertDoesNotExist()
                onNode(hasText("All") and hasContentDescription("Selected")).assertDoesNotExist()
            }
        }
    }

    "a disabled chip stays closed and shows the current selection" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    ChipDropdown(
                        selected = "Income",
                        options = options,
                        optionLabel = { it },
                        onSelect = {},
                        modifier = Modifier.testTag(chipTag),
                        enabled = false,
                    )
                }

                onNodeWithTag(chipTag).assertIsNotEnabled()
                onNodeWithText("Income").assertIsDisplayed()
                // Raw touch input: a click lands on the chip but cannot open the menu.
                onNodeWithTag(chipTag).performTouchInput { click() }

                // "All" and "Expense" only exist inside the menu: it stayed closed.
                onNodeWithText("All").assertDoesNotExist()
                onNodeWithText("Expense").assertDoesNotExist()
            }
        }
    }

    "picking the already selected option still closes the menu" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    ChipDropdown(
                        selected = "Income",
                        options = options,
                        optionLabel = { it },
                        onSelect = {},
                        modifier = Modifier.testTag(chipTag),
                    )
                }

                onNodeWithTag(chipTag).performClick()
                // The chip and the menu item both read "Income"; the check marks the menu item.
                onNode(hasText("Income") and hasContentDescription("Selected")).performClick()

                onNodeWithText("Expense").assertDoesNotExist()
                onNodeWithText("All").assertDoesNotExist()
            }
        }
    }
})
