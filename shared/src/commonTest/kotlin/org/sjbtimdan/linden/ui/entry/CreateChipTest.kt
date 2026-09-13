package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class CreateChipTest : StringSpec({
    "shows the label" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    CreateChip(label = "New category", onClick = {})
                }

                onNodeWithText("New category").assertIsDisplayed()
            }
        }
    }

    "invokes onClick when tapped" {
        onTestMain {
            runComposeUiTest {
                var tapped = false
                setContent {
                    CreateChip(label = "New account", onClick = { tapped = true })
                }

                onNodeWithText("New account").performClick()

                tapped shouldBe true
            }
        }
    }
})
