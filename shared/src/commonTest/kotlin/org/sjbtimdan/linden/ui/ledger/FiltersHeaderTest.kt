package org.sjbtimdan.linden.ui.ledger

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

@OptIn(ExperimentalTestApi::class)
class FiltersHeaderTest : StringSpec({
    "collapsed header shows the expand action and search shortcut" {
        onTestMain {
            runComposeUiTest {
                var searchClicked = false
                setContent {
                    FiltersHeader(
                        expanded = false,
                        onToggle = {},
                        onSearchClick = { searchClicked = true },
                    )
                }

                onNodeWithText("Filters & search").assertIsDisplayed()
                onNodeWithContentDescription("Expand filters").assertIsDisplayed()
                onNodeWithContentDescription("Search").performClick()

                searchClicked shouldBe true
            }
        }
    }

    "expanded header shows the collapse action and no search shortcut" {
        onTestMain {
            runComposeUiTest {
                var toggled = false
                setContent {
                    FiltersHeader(expanded = true, onToggle = { toggled = true })
                }

                onNodeWithContentDescription("Collapse filters").assertIsDisplayed()
                onNodeWithTag("filtersHeader").performClick()

                toggled shouldBe true
                onNodeWithContentDescription("Search").assertDoesNotExist()
            }
        }
    }
})
