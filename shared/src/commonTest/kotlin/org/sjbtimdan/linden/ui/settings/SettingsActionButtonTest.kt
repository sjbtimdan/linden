package org.sjbtimdan.linden.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class SettingsActionButtonTest : StringSpec({
    "shows the label and reports clicks" {
        onTestMain {
            runComposeUiTest {
                var clicks = 0
                setContent {
                    SettingsActionButton(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        label = "Back up database",
                        onClick = { clicks++ },
                    )
                }

                onNodeWithText("Back up database").assertIsDisplayed().assertHasClickAction()
                onNodeWithText("Back up database").performClick()

                clicks shouldBe 1
            }
        }
    }

    "a disabled button ignores clicks" {
        onTestMain {
            runComposeUiTest {
                var clicks = 0
                setContent {
                    SettingsActionButton(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        label = "Back up database",
                        onClick = { clicks++ },
                        enabled = false,
                    )
                }

                onNodeWithText("Back up database").assertIsNotEnabled().performClick()

                clicks shouldBe 0
            }
        }
    }

    "an outlined button still clicks" {
        onTestMain {
            runComposeUiTest {
                var clicks = 0
                setContent {
                    SettingsActionButton(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        label = "Restore from backup",
                        onClick = { clicks++ },
                        filled = false,
                    )
                }

                onNodeWithText("Restore from backup").performClick()

                clicks shouldBe 1
            }
        }
    }
})
