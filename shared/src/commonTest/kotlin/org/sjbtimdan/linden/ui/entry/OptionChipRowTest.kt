package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
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
})
