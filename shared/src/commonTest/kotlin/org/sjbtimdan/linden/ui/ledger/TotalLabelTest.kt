package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class TotalLabelTest : StringSpec({
    "renders positive, negative, missing and hidden totals" {
        onTestMain {
            runComposeUiTest {
                setContent { TotalLabel(total = 1_234, currency = Currency.CHF) }
                waitForIdle()
                onNodeWithText("+ 12.34 CHF").assertIsDisplayed()
            }
            runComposeUiTest {
                setContent { TotalLabel(total = -1_234, currency = Currency.CHF) }
                waitForIdle()
                onNodeWithText("− 12.34 CHF").assertIsDisplayed()
            }
            runComposeUiTest {
                setContent { TotalLabel(total = null, currency = Currency.CHF) }
                waitForIdle()
                onNodeWithText("–").assertIsDisplayed()
            }
            runComposeUiTest {
                setContent { TotalLabel(total = 1_234, currency = Currency.CHF, hidden = true) }
                waitForIdle()
                onNodeWithText("***").assertIsDisplayed()
            }
        }
    }
})
