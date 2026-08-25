package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

private val accountOptions = listOf("Checking", "Savings", "Credit Card")

@OptIn(ExperimentalTestApi::class)
class FieldDropdownTest : StringSpec({
    "shows the dropdown options when clicked" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    FieldDropdown(
                        label = "Account",
                        selected = null,
                        options = accountOptions,
                        optionLabel = { it },
                        onSelect = {},
                        onFocusChange = {},
                    )
                }

                onNodeWithText("Account").performClick()

                accountOptions.forEach { option ->
                    onNodeWithText(option).assertIsDisplayed()
                }
            }
        }
    }

    "selecting an option invokes onSelect with that option" {
        onTestMain {
            runComposeUiTest {
                var selected: String? = null
                setContent {
                    FieldDropdown(
                        label = "Account",
                        selected = null,
                        options = accountOptions,
                        optionLabel = { it },
                        onSelect = { selected = it },
                        onFocusChange = {},
                    )
                }

                onNodeWithText("Account").performClick()
                onNodeWithText("Savings").performClick()

                selected shouldBe "Savings"
            }
        }
    }

    "shows the predicted options while the field is focused" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    FieldDropdown(
                        label = "Account",
                        selected = null,
                        options = accountOptions,
                        optionLabel = { it },
                        onSelect = {},
                        onFocusChange = {},
                        predicted = listOf("Savings", "Checking"),
                    )
                }

                onNodeWithText("Account").performClick()

                onNodeWithText("Savings").assertIsDisplayed()
                onNodeWithText("Checking").assertIsDisplayed()
                onNodeWithText("Credit Card").assertDoesNotExist()
            }
        }
    }

    "reports focus changes through onFocusChange" {
        onTestMain {
            runComposeUiTest {
                var focused: Boolean? = null
                setContent {
                    FieldDropdown(
                        label = "Account",
                        selected = null,
                        options = accountOptions,
                        optionLabel = { it },
                        onSelect = {},
                        onFocusChange = { focused = it },
                    )
                }

                onNodeWithText("Account").performClick()

                focused shouldBe true
            }
        }
    }

    "shows the missing link instead of the dropdown when missing text is given" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    FieldDropdown(
                        label = "Account",
                        selected = null,
                        options = accountOptions,
                        optionLabel = { it },
                        onSelect = {},
                        onFocusChange = {},
                        missing = "Please enter account",
                    )
                }

                onNodeWithText("Account").assertIsDisplayed()
                onNodeWithText("Please enter account").assertIsDisplayed()
                onNode(hasSetTextAction()).assertDoesNotExist()
            }
        }
    }

    "clicking the missing link invokes onNavigateToSettings" {
        onTestMain {
            runComposeUiTest {
                var navigated = false
                setContent {
                    FieldDropdown(
                        label = "Account",
                        selected = null,
                        options = accountOptions,
                        optionLabel = { it },
                        onSelect = {},
                        onFocusChange = {},
                        missing = "Please enter account",
                        onNavigateToSettings = { navigated = true },
                    )
                }

                onNodeWithText("Please enter account").performClick()

                navigated shouldBe true
            }
        }
    }
})
