package org.sjbtimdan.linden

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
})
