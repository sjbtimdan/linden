package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class LedgerFilterActionsTest : StringSpec({

    "fires its toggle callback" {
        onTestMain {
            runComposeUiTest {
                var toggled = false
                setContent {
                    LedgerFilterActions(
                        hasActiveFilters = false,
                        onToggle = { toggled = true },
                    )
                }

                onNodeWithTag("filterAction").performClick()
                toggled shouldBe true
            }
        }
    }

    "lights up while filters are active and off otherwise" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    LedgerFilterActions(
                        hasActiveFilters = true,
                        onToggle = {},
                    )
                }

                onNodeWithTag("filterAction").assertIsOn()
            }
        }
        onTestMain {
            runComposeUiTest {
                setContent {
                    LedgerFilterActions(
                        hasActiveFilters = false,
                        onToggle = {},
                    )
                }

                onNodeWithTag("filterAction").assertIsOff()
            }
        }
    }
})
