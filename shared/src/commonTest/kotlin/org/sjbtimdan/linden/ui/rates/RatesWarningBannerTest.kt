package org.sjbtimdan.linden.ui.rates

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class RatesWarningBannerTest : StringSpec({
    "missing rates warn that rates can be set manually" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    RatesWarningBanner(warning = RatesWarning.Missing, onSetRates = {})
                }

                onNodeWithTag("ratesWarningBanner").assertIsDisplayed()
                onNodeWithText("No exchange rates available. You can set them manually.").assertIsDisplayed()
            }
        }
    }

    "outdated rates warn that rates can be set manually" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    RatesWarningBanner(warning = RatesWarning.Outdated, onSetRates = {})
                }

                onNodeWithText("Exchange rates are over a week old. You can set them manually.").assertIsDisplayed()
            }
        }
    }

    "clicking Set rates invokes onSetRates" {
        onTestMain {
            runComposeUiTest {
                var clicked = false
                setContent {
                    RatesWarningBanner(warning = RatesWarning.Missing, onSetRates = { clicked = true })
                }

                onNodeWithText("Set rates").performClick()

                clicked shouldBe true
            }
        }
    }
})
