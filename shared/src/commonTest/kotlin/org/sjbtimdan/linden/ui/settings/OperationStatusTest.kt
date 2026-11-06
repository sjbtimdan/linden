package org.sjbtimdan.linden.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class OperationStatusTest : StringSpec({
    "idle renders nothing" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    OperationStatus<Unit>(
                        state = BackupState.Idle,
                        workingText = "Working…",
                        successText = { "Done" },
                        failureText = { "Failed: $it" },
                        onDismiss = {},
                    )
                }

                onNodeWithText("Working…").assertDoesNotExist()
                onNodeWithText("Done").assertDoesNotExist()
            }
        }
    }

    "working shows the progress text" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    OperationStatus<Unit>(
                        state = BackupState.Working,
                        workingText = "Backing up…",
                        successText = { "Done" },
                        failureText = { "Failed: $it" },
                        onDismiss = {},
                    )
                }

                onNodeWithText("Backing up…").assertExists()
            }
        }
    }

    "success shows the message and dismisses on click" {
        onTestMain {
            runComposeUiTest {
                var dismissals = 0
                setContent {
                    OperationStatus(
                        state = BackupState.Success(Unit),
                        workingText = "Working…",
                        successText = { "Backup saved." },
                        failureText = { "Failed: $it" },
                        onDismiss = { dismissals++ },
                    )
                }

                onNodeWithText("Backup saved.").performClick()

                dismissals shouldBe 1
            }
        }
    }

    "error interpolates the failure message" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    OperationStatus<Unit>(
                        state = BackupState.Error("disk full"),
                        workingText = "Working…",
                        successText = { "Done" },
                        failureText = { "Backup failed: $it" },
                        onDismiss = {},
                    )
                }

                onNodeWithText("Backup failed: disk full").assertExists()
            }
        }
    }

    "error without a message falls back to the localized unknown error" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    OperationStatus<Unit>(
                        state = BackupState.Error(null),
                        workingText = "Working…",
                        successText = { "Done" },
                        failureText = { "Backup failed: $it" },
                        onDismiss = {},
                    )
                }

                onNodeWithText("Backup failed: unknown error").assertExists()
            }
        }
    }
})
