package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class ActiveFilterChipsTest : StringSpec({
    "renders one removable chip per filter and reports its clear" {
        onTestMain {
            runComposeUiTest {
                var cleared: String? = null
                setChipsContent(
                    listOf(
                        ActiveFilter(name = "Coffee", testTag = "searchChip", onClear = { cleared = "search" }),
                        ActiveFilter(name = "Expense", testTag = "typeChip", onClear = { cleared = "type" }),
                    ),
                )

                onNodeWithTag("searchChip").assertIsDisplayed()
                onNodeWithTag("typeChip").assertIsDisplayed()

                onNodeWithTag("searchChip").performClick()

                cleared shouldBe "search"
            }
        }
    }

    "a single filter has no Clear all action" {
        onTestMain {
            runComposeUiTest {
                setChipsContent(listOf(ActiveFilter(name = "Coffee", testTag = "searchChip", onClear = {})))

                onNodeWithTag("searchChip").assertIsDisplayed()
                onNodeWithTag("clearAllFilters").assertDoesNotExist()
            }
        }
    }

    "Clear all appears with two filters and reports the clear" {
        onTestMain {
            runComposeUiTest {
                var clearedAll = false
                setChipsContent(
                    listOf(
                        ActiveFilter(name = "Coffee", testTag = "searchChip", onClear = {}),
                        ActiveFilter(name = "Expense", testTag = "typeChip", onClear = {}),
                    ),
                    onClearAll = { clearedAll = true },
                )

                onNodeWithTag("clearAllFilters").performClick()

                clearedAll shouldBe true
            }
        }
    }

    "no filters render nothing" {
        onTestMain {
            runComposeUiTest {
                setChipsContent(emptyList())

                onNodeWithTag("clearAllFilters").assertDoesNotExist()
                onNodeWithTag("searchChip").assertDoesNotExist()
            }
        }
    }

    "a coloured filter renders its leading dot" {
        onTestMain {
            runComposeUiTest {
                setChipsContent(
                    listOf(
                        ActiveFilter(
                            name = "Groceries",
                            testTag = "categoryChip",
                            onClear = {},
                            leadingColor = Color.Red,
                        ),
                    ),
                )

                onNodeWithTag("filterChipDot", useUnmergedTree = true).assertIsDisplayed()
            }
        }
    }

    "an uncoloured filter renders no dot" {
        onTestMain {
            runComposeUiTest {
                setChipsContent(listOf(ActiveFilter(name = "Expense", testTag = "typeChip", onClear = {})))

                onNodeWithTag("filterChipDot", useUnmergedTree = true).assertDoesNotExist()
            }
        }
    }
})

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.setChipsContent(filters: List<ActiveFilter>, onClearAll: () -> Unit = {}) {
    setContent {
        ActiveFilterChips(filters = filters, onClearAll = onClearAll)
    }
}
