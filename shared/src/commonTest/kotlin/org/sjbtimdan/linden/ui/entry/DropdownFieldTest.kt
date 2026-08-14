package org.sjbtimdan.linden.ui.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
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
                onNodeWithText("Account").performClick()

                accountOptions.forEach { option ->
                    onNodeWithText(option).assertIsDisplayed()
                }
            }
        }
    }
})
