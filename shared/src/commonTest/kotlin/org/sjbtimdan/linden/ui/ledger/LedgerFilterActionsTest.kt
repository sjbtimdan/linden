package org.sjbtimdan.linden.ui.ledger

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
class LedgerFilterActionsTest : StringSpec({

    "search fires its callback and filter toggles" {
        onTestMain {
            runComposeUiTest {
                var searched = false
                var toggled = false
                setContent {
                    LedgerFilterActions(
                        activeFilterCount = 0,
                        onSearch = { searched = true },
                        onToggle = { toggled = true },
                    )
                }

                onNodeWithTag("searchAction").performClick()
                searched shouldBe true

                onNodeWithTag("filterAction").performClick()
                toggled shouldBe true
            }
        }
    }

    "the badge counts the active filters and disappears at zero" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    LedgerFilterActions(
                        activeFilterCount = 3,
                        onSearch = {},
                        onToggle = {},
                    )
                }

                onNodeWithText("3").assertIsDisplayed()
            }
        }
        onTestMain {
            runComposeUiTest {
                setContent {
                    LedgerFilterActions(
                        activeFilterCount = 0,
                        onSearch = {},
                        onToggle = {},
                    )
                }

                onNodeWithText("3").assertDoesNotExist()
            }
        }
    }
})
