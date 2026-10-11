package org.sjbtimdan.linden.ui

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.CoroutineScope

private class FailingWriteViewModel : AppViewModel() {
    fun write(block: suspend CoroutineScope.() -> Unit) = launchWrite(block)
}

@OptIn(ExperimentalTestApi::class)
class ErrorSnackbarTest : StringSpec({
    "a failed write shows a friendly message instead of the exception text" {
        onTestMain {
            runComposeUiTest {
                val viewModel = FailingWriteViewModel()
                val hostState = SnackbarHostState()
                setContent {
                    SnackbarHost(hostState = hostState)
                    viewModel.ErrorSnackbar(hostState)
                }

                viewModel.write { throw IllegalStateException("write failed") }
                waitForIdle()

                onNodeWithText("Your change couldn't be saved. Please try again.").assertIsDisplayed()
            }
        }
    }
})
