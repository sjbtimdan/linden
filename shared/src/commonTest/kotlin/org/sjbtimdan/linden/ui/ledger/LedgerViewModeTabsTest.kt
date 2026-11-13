package org.sjbtimdan.linden.ui.ledger

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class LedgerViewModeTabsTest : StringSpec({

    "tapping a tab reports and marks the selected mode" {
        onTestMain {
            runComposeUiTest {
                var selected by mutableStateOf(LedgerViewMode.Entries)
                setContent {
                    LedgerViewModeTabs(viewMode = selected, onSelect = { selected = it })
                }

                onNodeWithTag("viewModeTab-Accounts").performClick()
                waitForIdle()

                selected shouldBe LedgerViewMode.Accounts
                onNodeWithTag("viewModeTab-Accounts").assertIsSelected()
            }
        }
    }

    "each tab keeps a 48dp touch target" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    LedgerViewModeTabs(viewMode = LedgerViewMode.Entries, onSelect = {})
                }

                LedgerViewMode.entries.forEach { mode ->
                    onNodeWithTag("viewModeTab-${mode.name}").assertHeightIsAtLeast(48.dp)
                }
            }
        }
    }
})
