package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
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

    "typing in the search field filters the options" {
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
                onNodeWithText("Search").performTextInput("sav")

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
                onNodeWithText("Search").performTextInput("CREDIT")

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
                onNodeWithText("Search").performTextInput("zzz")

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
                onNodeWithText("Search").performTextInput("sav")
                onNodeWithText("Savings").performClick()

                selected shouldBe "Savings"
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
                onNodeWithText("Search").performTextInput("sav")
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
