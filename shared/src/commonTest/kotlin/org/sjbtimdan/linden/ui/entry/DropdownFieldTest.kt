package org.sjbtimdan.linden.ui.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

private val accountOptions = listOf("Checking", "Savings", "Credit Card")

@OptIn(ExperimentalTestApi::class)
class DropdownFieldTest : StringSpec({
    "clicking the field opens the menu with all options" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    DropdownField(
                        label = "Account",
                        selected = null,
                        options = accountOptions,
                        optionLabel = { it },
                        onSelect = {},
                    )
                }

                onNodeWithText("Account").performClick()

                accountOptions.forEach { option ->
                    onNodeWithText(option).assertIsDisplayed()
                }
            }
        }
    }

    "typing in the field filters the options" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    DropdownField(
                        label = "Account",
                        selected = null,
                        options = accountOptions,
                        optionLabel = { it },
                        onSelect = {},
                    )
                }

                onNodeWithText("Account").performClick()
                onNode(hasSetTextAction()).performTextInput("sav")

                onNodeWithText("Savings").assertIsDisplayed()
                onNodeWithText("Checking").assertDoesNotExist()
                onNodeWithText("Credit Card").assertDoesNotExist()
            }
        }
    }

    "search filtering is case-insensitive" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    DropdownField(
                        label = "Account",
                        selected = null,
                        options = accountOptions,
                        optionLabel = { it },
                        onSelect = {},
                    )
                }

                onNodeWithText("Account").performClick()
                onNode(hasSetTextAction()).performTextInput("CREDIT")

                onNodeWithText("Credit Card").assertIsDisplayed()
                onNodeWithText("Checking").assertDoesNotExist()
            }
        }
    }

    "a search with no matches hides all options" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    DropdownField(
                        label = "Account",
                        selected = null,
                        options = accountOptions,
                        optionLabel = { it },
                        onSelect = {},
                    )
                }

                onNodeWithText("Account").performClick()
                onNode(hasSetTextAction()).performTextInput("zzz")

                accountOptions.forEach { option ->
                    onNodeWithText(option).assertDoesNotExist()
                }
            }
        }
    }

    "selecting a filtered option invokes onSelect with that option" {
        onTestMain {
            runComposeUiTest {
                var selected: String? = null
                setContent {
                    DropdownField(
                        label = "Account",
                        selected = null,
                        options = accountOptions,
                        optionLabel = { it },
                        onSelect = { selected = it },
                    )
                }

                onNodeWithText("Account").performClick()
                onNode(hasSetTextAction()).performTextInput("sav")
                onNodeWithText("Savings").performClick()

                selected shouldBe "Savings"
            }
        }
    }

    "the field shows the selected option after the menu closes" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    var selected by remember { mutableStateOf<String?>(null) }
                    DropdownField(
                        label = "Account",
                        selected = selected,
                        options = accountOptions,
                        optionLabel = { it },
                        onSelect = { selected = it },
                    )
                }

                onNodeWithText("Account").performClick()
                onNode(hasSetTextAction()).performTextInput("sav")
                onNodeWithText("Savings").performClick()

                // Menu closed: only the anchor field shows the selection.
                onNodeWithText("Savings").assertIsDisplayed()
                onNodeWithText("Checking").assertDoesNotExist()
            }
        }
    }

    "opening the menu focuses the field for typing" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    DropdownField(
                        label = "Account",
                        selected = null,
                        options = accountOptions,
                        optionLabel = { it },
                        onSelect = {},
                    )
                }

                onNodeWithText("Account").performClick()

                waitUntil(timeoutMillis = 5_000) {
                    onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
                        .any {
                            it.config.contains(SemanticsProperties.Focused) &&
                                it.config[SemanticsProperties.Focused] == true
                        }
                }
            }
        }
    }

    "search query resets when the menu is reopened" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    var selected by remember { mutableStateOf<String?>(null) }
                    DropdownField(
                        label = "Account",
                        selected = selected,
                        options = accountOptions,
                        optionLabel = { it },
                        onSelect = { selected = it },
                    )
                }

                onNodeWithText("Account").performClick()
                onNode(hasSetTextAction()).performTextInput("sav")
                onNodeWithText("Savings").performClick()

                // With text present the label floats; click the field itself to reopen.
                onNode(hasSetTextAction() and hasText("Savings")).performClick()

                accountOptions.forEach { option ->
                    onNodeWithText(option).assertIsDisplayed()
                }
            }
        }
    }

    "shows all options with the predicted ones first and highlighted" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    DropdownField(
                        label = "Account",
                        selected = null,
                        options = accountOptions,
                        optionLabel = { it },
                        onSelect = {},
                        predictedOptions = listOf("Savings", "Checking"),
                    )
                }

                onNodeWithText("Account").performClick()

                accountOptions.forEach { option ->
                    onNodeWithText(option).assertIsDisplayed()
                }
                // Predicted options lead the list, highlighted.
                onNode(hasText("Savings") and hasContentDescription("Recommended")).assertIsDisplayed()
                onNode(hasText("Checking") and hasContentDescription("Recommended")).assertIsDisplayed()
                onNode(hasText("Credit Card") and hasContentDescription("Recommended")).assertDoesNotExist()
                val savingsLeft = onNodeWithText("Savings").getUnclippedBoundsInRoot().left
                val checkingLeft = onNodeWithText("Checking").getUnclippedBoundsInRoot().left
                val creditCardLeft = onNodeWithText("Credit Card").getUnclippedBoundsInRoot().left
                (savingsLeft < checkingLeft) shouldBe true
                (checkingLeft < creditCardLeft) shouldBe true
            }
        }
    }

    "typing filters the full option list, not just the predicted ones" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    DropdownField(
                        label = "Account",
                        selected = null,
                        options = accountOptions,
                        optionLabel = { it },
                        onSelect = {},
                        predictedOptions = listOf("Savings"),
                    )
                }

                onNodeWithText("Account").performClick()
                onNode(hasSetTextAction()).performTextInput("cred")

                onNodeWithText("Credit Card").assertIsDisplayed()
                onNodeWithText("Savings").assertDoesNotExist()
            }
        }
    }

    "typing keeps predicted matches first and highlighted" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    DropdownField(
                        label = "Account",
                        selected = null,
                        options = listOf("Checking", "Savings", "Savings Account"),
                        optionLabel = { it },
                        onSelect = {},
                        predictedOptions = listOf("Savings"),
                    )
                }

                onNodeWithText("Account").performClick()
                onNode(hasSetTextAction()).performTextInput("sav")

                onNode(hasText("Savings") and hasContentDescription("Recommended")).assertIsDisplayed()
                onNode(hasText("Savings Account") and hasContentDescription("Recommended")).assertDoesNotExist()
                val savingsLeft = onNodeWithText("Savings").getUnclippedBoundsInRoot().left
                val savingsAccountLeft = onNodeWithText("Savings Account").getUnclippedBoundsInRoot().left
                (savingsLeft < savingsAccountLeft) shouldBe true
            }
        }
    }

    "falls back to options when predicted options are empty" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    DropdownField(
                        label = "Account",
                        selected = null,
                        options = accountOptions,
                        optionLabel = { it },
                        onSelect = {},
                        predictedOptions = emptyList(),
                    )
                }

                onNodeWithText("Account").performClick()

                accountOptions.forEach { option ->
                    onNodeWithText(option).assertIsDisplayed()
                }
            }
        }
    }

    "shows every option regardless of list size" {
        onTestMain {
            runComposeUiTest {
                val manyOptions = (1L..15L).map { "Option $it" }
                setContent {
                    DropdownField(
                        label = "Account",
                        selected = null,
                        options = manyOptions,
                        optionLabel = { it },
                        onSelect = {},
                        predictedOptions = emptyList(),
                    )
                }

                onNodeWithText("Account").performClick()

                manyOptions.forEach { option ->
                    onNodeWithText(option).assertIsDisplayed()
                }
            }
        }
    }

    "ignores predicted options that are not in the list" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    DropdownField(
                        label = "Account",
                        selected = null,
                        options = accountOptions,
                        optionLabel = { it },
                        onSelect = {},
                        predictedOptions = listOf("Ghost"),
                    )
                }

                onNodeWithText("Account").performClick()

                onNodeWithText("Ghost").assertDoesNotExist()
                accountOptions.forEach { option ->
                    onNodeWithText(option).assertIsDisplayed()
                }
            }
        }
    }
})
