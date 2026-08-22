package org.sjbtimdan.linden

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.CompletableDeferred
import org.sjbtimdan.linden.data.FakeFxRatesSource
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class AppRootTest : StringSpec({
    "shows a loading indicator until dependencies are ready" {
        onTestMain {
            runComposeUiTest {
                val gate = CompletableDeferred<AppDependencies>()
                val dependencies = AppDependencies(
                    database = lindenDatabase(),
                    initialTheme = ThemeMode.SYSTEM,
                    initialCurrency = Currency.CHF,
                    fxRatesSource = FakeFxRatesSource(),
                )

                setContent {
                    AppRoot { gate.await() }
                }

                onNodeWithTag("loading").assertIsDisplayed()

                gate.complete(dependencies)
                waitForIdle()

                onNodeWithText("Add").assertIsDisplayed()
            }
        }
    }

    "shows an error screen when dependency creation fails" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    AppRoot { throw IllegalStateException("boom") }
                }

                waitForIdle()

                onNodeWithTag("loading").assertDoesNotExist()
                onNodeWithTag("startupError").assertIsDisplayed()
                onNodeWithText("Retry").assertIsDisplayed()
            }
        }
    }

    "retries dependency creation from the error screen" {
        onTestMain {
            runComposeUiTest {
                var attempts = 0
                val dependencies = AppDependencies(
                    database = lindenDatabase(),
                    initialTheme = ThemeMode.SYSTEM,
                    initialCurrency = Currency.CHF,
                    fxRatesSource = FakeFxRatesSource(),
                )

                setContent {
                    AppRoot {
                        attempts++
                        if (attempts == 1) throw IllegalStateException("boom")
                        dependencies
                    }
                }

                waitForIdle()
                onNodeWithTag("startupError").assertIsDisplayed()

                onNodeWithText("Retry").performClick()
                waitForIdle()

                onNodeWithTag("startupError").assertDoesNotExist()
                onNodeWithText("Add").assertIsDisplayed()
            }
        }
    }
})
