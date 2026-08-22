package org.sjbtimdan.linden.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@OptIn(ExperimentalTestApi::class)
class StartupErrorTest : StringSpec({
    "shows an error message and a retry button" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    StartupError(onRetry = {})
                }

                onNodeWithTag("startupError").assertIsDisplayed()
                onNodeWithText("Linden failed to start").assertIsDisplayed()
                onNodeWithText("Retry").assertIsDisplayed()
            }
        }
    }

    "invokes onRetry when the button is clicked" {
        onTestMain {
            runComposeUiTest {
                var retried = false
                setContent {
                    StartupError(onRetry = { retried = true })
                }

                onNodeWithText("Retry").performClick()

                retried shouldBe true
            }
        }
    }
})
