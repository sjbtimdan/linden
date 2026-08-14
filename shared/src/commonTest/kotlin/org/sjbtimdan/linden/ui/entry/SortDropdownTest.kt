package org.sjbtimdan.linden.ui.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class SortDropdownTest : StringSpec({
    "displays the current sort order label" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    SortDropdown(current = SortOrder.NewestFirst, onChange = {})
                }

                onNodeWithText("Sort: Newest first").assertIsDisplayed()
            }
        }
    }

    "clicking the button opens the menu with all sort options" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    SortDropdown(current = SortOrder.NewestFirst, onChange = {})
                }

                onNodeWithText("Sort: Newest first").performClick()

                onNodeWithText("Newest first").assertIsDisplayed()
                onNodeWithText("Oldest first").assertIsDisplayed()
                onNodeWithText("Amount high to low").assertIsDisplayed()
                onNodeWithText("Amount low to high").assertIsDisplayed()
            }
        }
    }

    "selecting an option invokes onChange with that sort order" {
        onTestMain {
            runComposeUiTest {
                var selected: SortOrder? = null
                setContent {
                    SortDropdown(current = SortOrder.NewestFirst, onChange = { selected = it })
                }

                onNodeWithText("Sort: Newest first").performClick()
                onNodeWithText("Amount low to high").performClick()

                selected shouldBe SortOrder.AmountLowToHigh
            }
        }
    }

    "selecting an option collapses the menu" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    SortDropdown(current = SortOrder.NewestFirst, onChange = {})
                }

                onNodeWithText("Sort: Newest first").performClick()
                onNodeWithText("Oldest first").assertIsDisplayed()
                onNodeWithText("Oldest first").performClick()

                onNodeWithText("Amount low to high").assertDoesNotExist()
            }
        }
    }

    "label updates when current changes" {
        onTestMain {
            runComposeUiTest {
                var order by mutableStateOf(SortOrder.NewestFirst)
                setContent {
                    SortDropdown(current = order, onChange = { order = it })
                }

                onNodeWithText("Sort: Newest first").assertIsDisplayed()

                onNodeWithText("Sort: Newest first").performClick()
                onNodeWithText("Oldest first").performClick()

                onNodeWithText("Sort: Oldest first").assertIsDisplayed()
            }
        }
    }
})
