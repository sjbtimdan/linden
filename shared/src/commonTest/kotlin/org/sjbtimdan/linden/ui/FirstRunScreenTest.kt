package org.sjbtimdan.linden.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.Currency

@OptIn(ExperimentalTestApi::class)
class FirstRunScreenTest : StringSpec({
    "shows the welcome copy and all currencies with CHF pre-selected" {
        onTestMain {
            runComposeUiTest {
                var completed: Currency? = null
                setContent {
                    FirstRunScreen(onComplete = { completed = it })
                }

                onNodeWithText("Welcome to Linden").assertIsDisplayed()
                onNodeWithTag("firstRunCurrency-CHF").assertIsDisplayed()
                onNodeWithTag("firstRunCurrency-USD").assertIsDisplayed()
                onNodeWithTag("firstRunCurrency-EUR").assertIsDisplayed()
                onNodeWithTag("firstRunContinue").assertIsDisplayed()
            }
        }
    }

    "completes with the pre-selected currency without any interaction" {
        onTestMain {
            runComposeUiTest {
                var completed: Currency? = null
                setContent {
                    FirstRunScreen(onComplete = { completed = it })
                }

                onNodeWithTag("firstRunContinue").performClick()

                completed shouldBe Currency.CHF
            }
        }
    }

    "completes with the selected currency" {
        onTestMain {
            runComposeUiTest {
                var completed: Currency? = null
                setContent {
                    FirstRunScreen(onComplete = { completed = it })
                }

                onNodeWithTag("firstRunCurrency-USD").performClick()
                onNodeWithTag("firstRunContinue").performClick()

                completed shouldBe Currency.USD
            }
        }
    }
})
