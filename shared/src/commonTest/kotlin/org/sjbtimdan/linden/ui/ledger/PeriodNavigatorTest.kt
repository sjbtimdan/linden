package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class PeriodNavigatorTest : StringSpec({
    "shows the month label of the anchor" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    PeriodNavigator(
                        period = LedgerPeriod.Month,
                        anchor = LocalDate(2026, 8, 15),
                        onPeriodChange = {},
                        onPrevious = {},
                        onNext = {},
                    )
                }

                onNodeWithText("Aug 2026").assertIsDisplayed()
            }
        }
    }

    "shows the day label of the anchor" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    PeriodNavigator(
                        period = LedgerPeriod.Day,
                        anchor = LocalDate(2026, 8, 15),
                        onPeriodChange = {},
                        onPrevious = {},
                        onNext = {},
                    )
                }

                onNodeWithText("15 Aug 2026").assertIsDisplayed()
            }
        }
    }

    "shows All and disables the arrows for the all period" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    PeriodNavigator(
                        period = LedgerPeriod.All,
                        anchor = LocalDate(2026, 8, 15),
                        onPeriodChange = {},
                        onPrevious = {},
                        onNext = {},
                    )
                }

                onNodeWithText("All").assertIsDisplayed()
                onNodeWithContentDescription("Previous period").assertIsNotEnabled()
                onNodeWithContentDescription("Next period").assertIsNotEnabled()
            }
        }
    }

    "previous arrow reports navigation on bounded periods" {
        onTestMain {
            runComposeUiTest {
                var previous = 0
                var next = 0
                setContent {
                    PeriodNavigator(
                        period = LedgerPeriod.Month,
                        anchor = LocalDate(2026, 8, 15),
                        onPeriodChange = {},
                        onPrevious = { previous++ },
                        onNext = { next++ },
                    )
                }

                onNodeWithContentDescription("Previous period").performClick()
                onNodeWithContentDescription("Next period").performClick()

                previous shouldBe 1
                next shouldBe 1
            }
        }
    }

    "the period dropdown lists all granularities and reports a selection" {
        onTestMain {
            runComposeUiTest {
                var selected: LedgerPeriod? = null
                setContent {
                    PeriodNavigator(
                        period = LedgerPeriod.Month,
                        anchor = LocalDate(2026, 8, 15),
                        onPeriodChange = { selected = it },
                        onPrevious = {},
                        onNext = {},
                    )
                }

                onNodeWithTag("periodLabel").performClick()

                LedgerPeriod.entries.forEach { option ->
                    onNodeWithText(option.name).assertIsDisplayed()
                }

                onNodeWithText("Year").performClick()

                selected shouldBe LedgerPeriod.Year
            }
        }
    }

    "future toggle is hidden without a toggle callback" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    PeriodNavigator(
                        period = LedgerPeriod.Month,
                        anchor = LocalDate(2026, 8, 15),
                        onPeriodChange = {},
                        onPrevious = {},
                        onNext = {},
                    )
                }

                onNodeWithTag("showFutureToggle").assertDoesNotExist()
            }
        }
    }

    "future toggle reflects showFuture and reports toggles" {
        onTestMain {
            runComposeUiTest {
                var toggled = 0
                setContent {
                    PeriodNavigator(
                        period = LedgerPeriod.Month,
                        anchor = LocalDate(2026, 8, 15),
                        onPeriodChange = {},
                        onPrevious = {},
                        onNext = {},
                        showFuture = false,
                        onToggleShowFuture = { toggled++ },
                    )
                }

                onNodeWithContentDescription("Show future entries").assertIsDisplayed()
                onNodeWithTag("showFutureToggle").performClick()

                toggled shouldBe 1
            }
        }
    }

    "future toggle advertises the hide action while future entries are shown" {
        onTestMain {
            runComposeUiTest {
                var toggled = 0
                setContent {
                    PeriodNavigator(
                        period = LedgerPeriod.All,
                        anchor = LocalDate(2026, 8, 15),
                        onPeriodChange = {},
                        onPrevious = {},
                        onNext = {},
                        showFuture = true,
                        onToggleShowFuture = { toggled++ },
                    )
                }

                // The toggle stays usable even when the arrows are disabled.
                onNodeWithContentDescription("Hide future entries").assertIsDisplayed()
                onNodeWithTag("showFutureToggle").performClick()

                toggled shouldBe 1
            }
        }
    }
})
