package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class AmountFieldTest : StringSpec({
    "shows the label, value and currency suffix" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    AmountField(
                        value = "12.50",
                        label = "Amount",
                        suffix = "CHF",
                        warning = null,
                        onValueChange = {},
                        onFocus = {},
                    )
                }

                onNodeWithText("Amount").assertIsDisplayed()
                onNodeWithText("12.50").assertIsDisplayed()
                onNodeWithText("CHF").assertIsDisplayed()
            }
        }
    }

    "hides the suffix when none is given" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    AmountField(
                        value = "12.50",
                        label = "Amount",
                        suffix = null,
                        warning = null,
                        onValueChange = {},
                        onFocus = {},
                    )
                }

                onNodeWithText("CHF").assertDoesNotExist()
            }
        }
    }

    "typing invokes onValueChange with the new text" {
        onTestMain {
            runComposeUiTest {
                var changed: String? = null
                setContent {
                    AmountField(
                        value = "",
                        label = "Amount",
                        suffix = null,
                        warning = null,
                        onValueChange = { changed = it },
                        onFocus = {},
                    )
                }

                onNode(hasSetTextAction()).performClick()
                onNode(hasSetTextAction()).performTextInput("4.50")

                changed shouldBe "4.50"
            }
        }
    }

    "the clear button invokes onValueChange with an empty string" {
        onTestMain {
            runComposeUiTest {
                var changed: String? = null
                setContent {
                    AmountField(
                        value = "12.50",
                        label = "Amount",
                        suffix = null,
                        warning = null,
                        onValueChange = { changed = it },
                        onFocus = {},
                    )
                }

                onNodeWithContentDescription("Clear").performClick()

                changed shouldBe ""
            }
        }
    }

    "hides the clear button while the value is empty" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    AmountField(
                        value = "",
                        label = "Amount",
                        suffix = null,
                        warning = null,
                        onValueChange = {},
                        onFocus = {},
                    )
                }

                onNodeWithContentDescription("Clear").assertDoesNotExist()
            }
        }
    }

    "shows the warning as supporting text" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    AmountField(
                        value = "0.00",
                        label = "Amount",
                        suffix = null,
                        warning = "Amount must be greater than zero",
                        onValueChange = {},
                        onFocus = {},
                    )
                }

                onNodeWithText("Amount must be greater than zero").assertIsDisplayed()
            }
        }
    }

    "is not marked as an error without a warning" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    AmountField(
                        value = "12.50",
                        label = "Amount",
                        suffix = null,
                        warning = null,
                        onValueChange = {},
                        onFocus = {},
                    )
                }

                onNodeWithText("Amount must be greater than zero").assertDoesNotExist()
            }
        }
    }

    "invokes onFocus when the field gains focus" {
        onTestMain {
            runComposeUiTest {
                var focused = false
                setContent {
                    AmountField(
                        value = "",
                        label = "Amount",
                        suffix = null,
                        warning = null,
                        onValueChange = {},
                        onFocus = { focused = true },
                    )
                }

                onNode(hasSetTextAction()).performClick()

                focused shouldBe true
            }
        }
    }
})
