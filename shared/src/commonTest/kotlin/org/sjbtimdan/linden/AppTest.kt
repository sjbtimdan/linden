package org.sjbtimdan.linden

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.core.spec.style.StringSpec
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
            onNodeWithText("No entries match.").assertExists()

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
        withApp(fxRatesSource = FakeFxRatesSource { error("network") }) { dependencies ->
            setContent { App(dependencies) }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("No exchange rates available. You can set them manually.")
                    .fetchSemanticsNodes().isNotEmpty()
            }

            onNodeWithText("Set rates").performClick()
            onNodeWithText("Refresh").assertExists()
        }
    }
})
