package org.sjbtimdan.linden.ui.history

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

private const val CHIP_TAG = "filterChip"

@OptIn(ExperimentalTestApi::class)
class EntryFilterChipTest : StringSpec({
    "shows the label and the remove icon" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    EntryFilterChip(
                        name = "Groceries",
                        onClick = {},
                        modifier = Modifier.testTag(CHIP_TAG),
                    )
                }

                onNodeWithTag(CHIP_TAG).assertIsDisplayed()
                onNodeWithText("Groceries").assertIsDisplayed()
                onNodeWithContentDescription("Remove filter").assertIsDisplayed()
            }
        }
    }

    "clicking the chip invokes onClick" {
        onTestMain {
            runComposeUiTest {
                var removed = false
                setContent {
                    EntryFilterChip(
                        name = "Groceries",
                        onClick = { removed = true },
                        modifier = Modifier.testTag(CHIP_TAG),
                    )
                }

                onNodeWithTag(CHIP_TAG).performClick()

                removed shouldBe true
            }
        }
    }

    "clicking the remove icon invokes onClick" {
        onTestMain {
            runComposeUiTest {
                var removed = false
                setContent {
                    EntryFilterChip(
                        name = "Groceries",
                        onClick = { removed = true },
                        modifier = Modifier.testTag(CHIP_TAG),
                    )
                }

                onNodeWithContentDescription("Remove filter").performClick()

                removed shouldBe true
            }
        }
    }

    "shows the color dot when a leading color is provided" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    EntryFilterChip(
                        name = "Groceries",
                        onClick = {},
                        leadingColor = Color.Red,
                        modifier = Modifier.testTag(CHIP_TAG),
                    )
                }

                // The dot sits inside the chip's clickable, which merges child
                // semantics, so it is only visible in the unmerged tree.
                onNodeWithTag("filterChipDot", useUnmergedTree = true).assertIsDisplayed()
            }
        }
    }

    "renders without a color dot when no leading color is provided" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    EntryFilterChip(
                        name = "Main",
                        onClick = {},
                        modifier = Modifier.testTag(CHIP_TAG),
                    )
                }

                onNodeWithTag("filterChipDot", useUnmergedTree = true).assertDoesNotExist()
            }
        }
    }
})
