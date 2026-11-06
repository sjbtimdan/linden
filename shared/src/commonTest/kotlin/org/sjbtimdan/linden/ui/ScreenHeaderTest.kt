package org.sjbtimdan.linden.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@OptIn(ExperimentalTestApi::class)
class ScreenHeaderTest : StringSpec({

    "the header shows the title and back fires exactly once per tap" {
        onTestMain {
            runComposeUiTest {
                var backs = 0
                setContent {
                    ScreenHeader(title = "Accounts", onBack = { backs++ })
                }

                onNodeWithTag("screenTitle").assertTextEquals("Accounts").assertIsDisplayed()
                onNodeWithContentDescription("Back").performClick()
                backs shouldBe 1
            }
        }
    }

    "header actions are placed in the actions slot" {
        onTestMain {
            runComposeUiTest {
                var taps = 0
                setContent {
                    ScreenHeader(
                        title = "Budgets",
                        onBack = {},
                        actions = {
                            IconButton(
                                onClick = { taps++ },
                                modifier = Modifier.testTag("headerAction"),
                            ) {
                                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                            }
                        },
                    )
                }

                onNodeWithTag("screenTitle").assertTextEquals("Budgets")
                onNodeWithTag("headerAction").performClick()
                taps shouldBe 1
            }
        }
    }
})
