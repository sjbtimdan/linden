package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.waitForText(text: String) {
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
}

/**
 * Taps dead space to drop focus, as a user would to close the keyboard and
 * bring the full form back.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.tapOutside() {
    onRoot().performTouchInput {
        click(Offset(centerX * 0.2f, bottom - 40f))
    }
}
