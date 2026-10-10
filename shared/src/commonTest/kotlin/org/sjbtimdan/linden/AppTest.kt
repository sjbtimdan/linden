package org.sjbtimdan.linden

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import org.sjbtimdan.linden.data.FakeFxRatesSource
import org.sjbtimdan.linden.ui.withApp

@OptIn(ExperimentalTestApi::class)
class AppTest : StringSpec({
    "starts on the entry screen" {
        withApp { dependencies ->
            setContent { App(dependencies) }
            onNodeWithText("Add").assertExists()
        }
    }

    "bottom navigation switches screens" {
        withApp { dependencies ->
            setContent { App(dependencies) }

            onNodeWithText("Ledger").performClick()
            // A brand-new dataset is guided to its first entry.
            onNodeWithText("No entries yet.").assertExists()
            onNodeWithText("Add your first entry").assertExists()

            onNodeWithText("Settings").performClick()
            onNodeWithText("Import from Ivy").assertExists()

            onNodeWithText("Entry").performClick()
            onNodeWithText("Add").assertExists()
        }
    }

    "settings navigates to categories and back" {
        withApp { dependencies ->
            setContent { App(dependencies) }

            onNodeWithText("Settings").performClick()
            onNodeWithText("Categories").performClick()
            onNodeWithText("No categories yet.").assertExists()

            onNodeWithContentDescription("Back").performClick()
            onNodeWithText("Import from Ivy").assertExists()
        }
    }

    "settings navigates to accounts and back" {
        withApp { dependencies ->
            setContent { App(dependencies) }

            onNodeWithText("Settings").performClick()
            onNodeWithText("Accounts").performClick()
            onNodeWithText("New Account").assertExists()

            onNodeWithContentDescription("Back").performClick()
            onNodeWithText("Import from Ivy").assertExists()
        }
    }

    "settings navigates to currency rates and back" {
        withApp { dependencies ->
            setContent { App(dependencies) }

            onNodeWithText("Settings").performClick()
            onNodeWithText("Currency rates").performClick()
            onNodeWithText("Refresh").assertExists()

            onNodeWithContentDescription("Back").performClick()
            onNodeWithText("Import from Ivy").assertExists()
        }
    }

    "startup shows a rates warning and navigates to rates when the fetch fails with no cached rates" {
        withApp(fxRatesSource = FakeFxRatesSource { error("network") }, ratesSeen = true) { dependencies ->
            setContent { App(dependencies) }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("No exchange rates available. You can set them manually.")
                    .fetchSemanticsNodes().isNotEmpty()
            }

            onNodeWithText("Set rates").performClick()
            onNodeWithText("Refresh").assertExists()
        }
    }

    "system back from a sub-screen returns to the screen it was opened from" {
        withApp(fxRatesSource = FakeFxRatesSource { error("network") }, ratesSeen = true) { dependencies ->
            var systemBack: (() -> Unit)? = null
            setContent {
                App(
                    dependencies,
                    systemBackHandler = { enabled, onBack -> if (enabled) systemBack = onBack },
                )
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("No exchange rates available. You can set them manually.")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Set rates").performClick()
            onNodeWithText("Refresh").assertExists()
            waitForIdle()

            systemBack shouldNotBe null
            systemBack!!.invoke()
            waitForIdle()

            // Rates were opened from Entry, so back returns there instead of
            // exiting the app or landing on Settings.
            onNodeWithText("Add").assertExists()
        }
    }

    "the back arrow from a sub-screen returns to the screen it was opened from" {
        withApp(fxRatesSource = FakeFxRatesSource { error("network") }, ratesSeen = true) { dependencies ->
            setContent { App(dependencies) }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("No exchange rates available. You can set them manually.")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Set rates").performClick()
            onNodeWithText("Refresh").assertExists()

            onNodeWithContentDescription("Back").performClick()

            onNodeWithText("Add").assertExists()
        }
    }

    "the active tab is marked selected in the bottom navigation" {
        withApp { dependencies ->
            setContent { App(dependencies) }

            onNodeWithText("Entry").assertIsSelected()
            onNodeWithText("Ledger").assertIsNotSelected()

            onNodeWithText("Ledger").performClick()
            onNodeWithText("Ledger").assertIsSelected()
            onNodeWithText("Entry").assertIsNotSelected()
        }
    }

    "swiping moves between the top-level screens and follows the bottom bar" {
        withApp { dependencies ->
            setContent { App(dependencies) }

            // Entry → Settings.
            swipeTopLevelPager(forward = true)
            waitForIdle()
            onNodeWithText("Import from Ivy").assertExists()
            onNode(hasText("Settings") and isSelectable()).assertIsSelected()

            // Settings → Entry.
            swipeTopLevelPager(forward = false)
            waitForIdle()
            onNodeWithText("Add").assertExists()
            onNode(hasText("Entry") and isSelectable()).assertIsSelected()

            // Entry → Ledger.
            swipeTopLevelPager(forward = false)
            waitForIdle()
            onNodeWithText("No entries yet.").assertExists()
            onNode(hasText("Ledger") and isSelectable()).assertIsSelected()
        }
    }
})

/**
 * Swipes the top-level pager one page forward (toward Settings) or back,
 * near its top edge so the gesture starts clear of form text fields.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.swipeTopLevelPager(forward: Boolean) {
    onNodeWithTag("topLevelPager").performTouchInput {
        val y = top + 40f
        val fromX = if (forward) right - 16f else left + 16f
        val toX = if (forward) left + 16f else right - 16f
        swipe(start = Offset(fromX, y), end = Offset(toX, y), durationMillis = 200)
    }
}
