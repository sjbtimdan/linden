package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

private data class ChipAccount(val id: Long, val name: String)

@OptIn(ExperimentalTestApi::class)
class OptionChipRowTest : StringSpec({
    "renders every option as a chip" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    OptionChipRow(
                        options = listOf("Checking", "Savings", "Credit Card"),
                        optionLabel = { it },
                        onSelect = {},
                    )
                }

                listOf("Checking", "Savings", "Credit Card").forEach { option ->
                    onNodeWithText(option).assertIsDisplayed()
                }
            }
        }
    }

    "wraps overflowing options onto additional lines instead of hiding them" {
        onTestMain {
            runComposeUiTest {
                val options = (1L..12L).map { "Option number $it with a longer label" }
                setContent {
                    OptionChipRow(
                        options = options,
                        optionLabel = { it },
                        onSelect = {},
                    )
                }

                // Chips wrap onto more lines, so every option stays composed and visible.
                options.forEach { option ->
                    onNodeWithText(option).assertIsDisplayed()
                }
            }
        }
    }

    "clicking a chip invokes onSelect with that option" {
        onTestMain {
            runComposeUiTest {
                var picked: String? = null
                setContent {
                    OptionChipRow(
                        options = listOf("Checking", "Savings"),
                        optionLabel = { it },
                        onSelect = { picked = it },
                    )
                }

                onNodeWithText("Savings").performClick()

                picked shouldBe "Savings"
            }
        }
    }

    "chips are labelled through optionLabel and select the underlying item" {
        onTestMain {
            runComposeUiTest {
                var picked: ChipAccount? = null
                val accounts = listOf(ChipAccount(1, "Main"), ChipAccount(2, "Savings"))
                setContent {
                    OptionChipRow(
                        options = accounts,
                        optionLabel = { it.name },
                        onSelect = { picked = it },
                    )
                }

                onNodeWithText("Main").performClick()

                picked shouldBe ChipAccount(1, "Main")
            }
        }
    }

    "the selected option is marked as selected" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    OptionChipRow(
                        options = listOf("Checking", "Savings"),
                        optionLabel = { it },
                        onSelect = {},
                        isSelected = { it == "Savings" },
                    )
                }

                onNodeWithText("Savings").assertIsSelected()
                onNodeWithText("Checking").assertIsNotSelected()
            }
        }
    }

    "predicted options are marked as recommended and still selectable" {
        onTestMain {
            runComposeUiTest {
                var picked: String? = null
                setContent {
                    OptionChipRow(
                        options = listOf("Checking", "Savings"),
                        optionLabel = { it },
                        onSelect = { picked = it },
                        isPredicted = { it == "Savings" },
                    )
                }

                onNode(hasText("Savings") and hasContentDescription("Recommended")).assertIsDisplayed()
                onNode(hasText("Checking") and hasContentDescription("Recommended")).assertDoesNotExist()

                onNodeWithText("Savings").performClick()

                picked shouldBe "Savings"
            }
        }
    }

    "the selected option stays marked selected even when predicted" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    OptionChipRow(
                        options = listOf("Checking", "Savings"),
                        optionLabel = { it },
                        onSelect = {},
                        isSelected = { it == "Savings" },
                        isPredicted = { it == "Savings" },
                    )
                }

                onNodeWithText("Savings").assertIsSelected()
                onNode(hasText("Savings") and hasContentDescription("Recommended")).assertDoesNotExist()
            }
        }
    }
})
