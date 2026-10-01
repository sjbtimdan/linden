package org.sjbtimdan.linden.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class SettingsNavRowTest : StringSpec({
    "shows the label and exposes a click action" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    SettingsNavRow(
                        icon = Icons.Filled.Savings,
                        label = "Budgets",
                        onClick = {},
                    )
                }

                onNodeWithText("Budgets").assertIsDisplayed()
                onNodeWithText("Budgets").assertHasClickAction()
            }
        }
    }

    "reports every click" {
        onTestMain {
            runComposeUiTest {
                var clicks = 0
                setContent {
                    SettingsNavRow(
                        icon = Icons.Filled.Savings,
                        label = "Budgets",
                        onClick = { clicks++ },
                    )
                }

                onNodeWithText("Budgets").performClick()
                onNodeWithText("Budgets").performClick()

                clicks shouldBe 2
            }
        }
    }
})
