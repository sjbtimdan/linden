package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class EmptyStateTest : StringSpec({
    "shows the message" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    EmptyState(message = "No entries yet.")
                }

                onNodeWithText("No entries yet.").assertIsDisplayed()
            }
        }
    }

    "renders without an action button by default" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    EmptyState(message = "No accounts yet.")
                }

                onNodeWithText("No accounts yet.").assertIsDisplayed()
                onNodeWithText("Create an account").assertDoesNotExist()
            }
        }
    }

    "shows an action button that reports clicks" {
        onTestMain {
            runComposeUiTest {
                var clicks = 0
                setContent {
                    EmptyState(
                        message = "No categories yet.",
                        actionLabel = "Add categories",
                        onAction = { clicks++ },
                    )
                }

                onNodeWithText("Add categories").performClick()
                onNodeWithText("Add categories").performClick()

                clicks shouldBe 2
            }
        }
    }

    "hides the action when the label has no callback" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    EmptyState(
                        message = "No entries yet.",
                        actionLabel = "Add your first entry",
                        onAction = null,
                    )
                }

                onNodeWithText("No entries yet.").assertIsDisplayed()
                onNodeWithText("Add your first entry").assertDoesNotExist()
            }
        }
    }
})
