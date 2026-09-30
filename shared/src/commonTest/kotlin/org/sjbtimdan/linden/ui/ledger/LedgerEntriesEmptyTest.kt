package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class LedgerEntriesEmptyTest : StringSpec({
    "the guided state invites adding the first entry" {
        onTestMain {
            runComposeUiTest {
                var added = false
                setEmptyContent(LedgerEntriesEmptyState.Guided, onAddFirstEntry = { added = true })

                onNodeWithText("No entries yet.").assertIsDisplayed()
                onNodeWithText("Add your first entry").performClick()

                added shouldBe true
            }
        }
    }

    "the upcoming-hidden state offers revealing future entries" {
        onTestMain {
            runComposeUiTest {
                var revealed = false
                setEmptyContent(LedgerEntriesEmptyState.UpcomingHidden, onRevealUpcoming = { revealed = true })

                onNodeWithText("Entries after today are hidden.").assertIsDisplayed()
                onNodeWithText("Show entries after today").performClick()

                revealed shouldBe true
            }
        }
    }

    "the filtered state explains the filter miss without an action" {
        onTestMain {
            runComposeUiTest {
                setEmptyContent(LedgerEntriesEmptyState.Filtered)

                onNodeWithText("No entries match this filter.").assertIsDisplayed()
                onNodeWithText("Add your first entry").assertDoesNotExist()
                onNodeWithText("Show entries after today").assertDoesNotExist()
            }
        }
    }

    "the no-match state explains the miss without an action" {
        onTestMain {
            runComposeUiTest {
                setEmptyContent(LedgerEntriesEmptyState.NoMatch)

                onNodeWithText("No entries match.").assertIsDisplayed()
                onNodeWithText("Add your first entry").assertDoesNotExist()
            }
        }
    }

    "the no-entries state reads like an empty ledger without the guided action" {
        onTestMain {
            runComposeUiTest {
                setEmptyContent(LedgerEntriesEmptyState.NoEntries)

                onNodeWithText("No entries yet.").assertIsDisplayed()
                onNodeWithText("Add your first entry").assertDoesNotExist()
            }
        }
    }
})

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.setEmptyContent(
    state: LedgerEntriesEmptyState,
    onAddFirstEntry: () -> Unit = {},
    onRevealUpcoming: () -> Unit = {},
) {
    setContent {
        LedgerEntriesEmpty(
            state = state,
            onAddFirstEntry = onAddFirstEntry,
            onRevealUpcoming = onRevealUpcoming,
        )
    }
}
