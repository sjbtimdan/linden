package org.sjbtimdan.linden.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@OptIn(ExperimentalTestApi::class)
class SlidingPillTabsTest : StringSpec({

    "the selected tab has the selected state and a click moves the selection" {
        onTestMain {
            runComposeUiTest {
                var selected by mutableStateOf(0)
                setContent {
                    SlidingPillTabs(
                        tabs = listOf(
                            SlidingPillTab(label = "Entries", tag = "tab-0"),
                            SlidingPillTab(label = "Accounts", tag = "tab-1"),
                            SlidingPillTab(label = "Categories", tag = "tab-2"),
                        ),
                        selectedIndex = selected,
                        onSelect = { selected = it },
                    )
                }

                onNodeWithTag("tab-0").assertIsSelected()
                onNodeWithTag("tab-1").assertIsNotSelected()
                onNodeWithTag("tab-1").performClick()
                onNodeWithTag("tab-1").assertIsSelected()
                selected shouldBe 1
            }
        }
    }

    "every tab shows its label and an optional leading icon stays tag-addressable" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    SlidingPillTabs(
                        tabs = listOf(
                            SlidingPillTab(label = "Expense", icon = Icons.Filled.Add, tag = "tab-0"),
                            SlidingPillTab(label = "Income", tag = "tab-1"),
                        ),
                        selectedIndex = 0,
                        onSelect = {},
                    )
                }

                onNodeWithTag("tab-0").assertTextEquals("Expense").assertIsDisplayed()
                onNodeWithTag("tab-1").assertTextEquals("Income").assertIsDisplayed()
            }
        }
    }
})
