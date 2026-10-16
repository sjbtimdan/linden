package org.sjbtimdan.linden.ui.capture

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

private val apps = listOf(
    CapturableApp("com.example.bank", "Example Bank"),
    CapturableApp("com.example.wallet", "Wallet"),
)

@OptIn(ExperimentalTestApi::class)
class CaptureAppPickerDialogTest : StringSpec({

    "lists the apps and reports the picked one" {
        onTestMain {
            runComposeUiTest {
                var picked: CapturableApp? = null
                setContent {
                    CaptureAppPickerDialog(apps = apps, onPick = { picked = it }, onDismiss = {})
                }

                onNodeWithText("Choose an app").assertExists()
                onNodeWithText("Example Bank").assertExists()
                onNodeWithText("Wallet").assertExists()

                onNodeWithText("Wallet").performClick()

                picked?.packageName shouldBe "com.example.wallet"
            }
        }
    }

    "search narrows the list to matching apps" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    CaptureAppPickerDialog(apps = apps, onPick = {}, onDismiss = {})
                }

                onNode(hasSetTextAction()).performTextInput("wallet")

                onNodeWithText("Wallet").assertExists()
                onNodeWithText("Example Bank").assertDoesNotExist()
            }
        }
    }

    "an empty search result explains there is no match" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    CaptureAppPickerDialog(apps = apps, onPick = {}, onDismiss = {})
                }

                onNode(hasSetTextAction()).performTextInput("zzz")

                onNodeWithText("No apps match.").assertExists()
            }
        }
    }

    "dismiss reports the dismissal" {
        onTestMain {
            runComposeUiTest {
                var dismissed = false
                setContent {
                    CaptureAppPickerDialog(apps = emptyList(), onPick = {}, onDismiss = { dismissed = true })
                }

                onNodeWithText("Dismiss").performClick()

                dismissed shouldBe true
            }
        }
    }
})
