package org.sjbtimdan.linden.ui.insights

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.ui.onTestMain

private val seriesColors = listOf(Color(0xFFD64535), Color(0xFF27A059))

@OptIn(ExperimentalTestApi::class)
class MonthlyTrendChartTest : StringSpec({

    "barHeightFraction maps the tallest value to full height" {
        barHeightFraction(200L, 200L) shouldBe 1f
    }

    "barHeightFraction scales shorter bars proportionally" {
        barHeightFraction(100L, 200L) shouldBe 0.5f
    }

    "barHeightFraction never shrinks a positive bar below the visible minimum" {
        barHeightFraction(1L, 200L) shouldBe MIN_VISIBLE_BAR_FRACTION
    }

    "barHeightFraction returns zero for a zero value" {
        barHeightFraction(0L, 200L) shouldBe 0f
    }

    "barHeightFraction never exceeds one" {
        barHeightFraction(300L, 200L) shouldBe 1f
    }

    "barHeightFraction is zero when every value is zero" {
        barHeightFraction(0L, 0L) shouldBe 0f
    }

    "barHeightFraction ignores negative values" {
        barHeightFraction(-5L, 200L) shouldBe 0f
    }

    "renders one column per bar with its label" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    MonthlyTrendChart(
                        bars = trendBars(),
                        seriesColors = seriesColors,
                        selectedIndex = 0,
                        onSelect = {},
                        modifier = Modifier,
                    )
                }

                onNodeWithTag(MONTHLY_TREND_CHART_TAG).assertIsDisplayed()
                trendBars().forEach { bar ->
                    onNodeWithText(bar.label).assertIsDisplayed()
                }
            }
        }
    }

    "renders an all-zero chart without bars or crash" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    MonthlyTrendChart(
                        bars = trendBars(valueMultiplier = 0L),
                        seriesColors = seriesColors,
                        selectedIndex = 0,
                        onSelect = {},
                        modifier = Modifier,
                    )
                }

                onNodeWithTag(MONTHLY_TREND_CHART_TAG).assertIsDisplayed()
                onNodeWithText("Jan").assertIsDisplayed()
            }
        }
    }

    "clicking a column reports its index" {
        onTestMain {
            runComposeUiTest {
                var selected = 0
                setContent {
                    MonthlyTrendChart(
                        bars = trendBars(),
                        seriesColors = seriesColors,
                        selectedIndex = selected,
                        onSelect = { selected = it },
                        modifier = Modifier,
                    )
                }

                onNodeWithTag(TREND_BAR_TAG_PREFIX + 1).performClick()

                selected shouldBe 1
            }
        }
    }

    "clicking the selected column still reports its index" {
        onTestMain {
            runComposeUiTest {
                var selected = 1
                setContent {
                    MonthlyTrendChart(
                        bars = trendBars(),
                        seriesColors = seriesColors,
                        selectedIndex = selected,
                        onSelect = { selected = it },
                        modifier = Modifier,
                    )
                }

                onNodeWithTag(TREND_BAR_TAG_PREFIX + 1).performClick()

                selected shouldBe 1
            }
        }
    }

    "renders columns of several series without crashing" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    MonthlyTrendChart(
                        bars = trendBars(),
                        seriesColors = seriesColors,
                        selectedIndex = 2,
                        onSelect = {},
                        modifier = Modifier,
                    )
                }

                onNodeWithTag(MONTHLY_TREND_CHART_TAG).assertIsDisplayed()
            }
        }
    }

    "scales against the tallest value of any series" {
        // Expense series peaks at 200 in Feb, income at 150 in Mar: both scale to 1f.
        barHeightFraction(150L, 200L) shouldBe 0.75f
        barHeightFraction(200L, 200L) shouldBe 1f
    }
})

private fun trendBars(valueMultiplier: Long = 1L) = listOf(
    MonthlyTrendBar(label = "Jan", values = listOf(100L * valueMultiplier, 50L * valueMultiplier)),
    MonthlyTrendBar(
        label = "Feb",
        values = listOf(200L * valueMultiplier, 0L * valueMultiplier),
        isCurrent = true,
    ),
    MonthlyTrendBar(label = "Mar", values = listOf(50L * valueMultiplier, 150L * valueMultiplier)),
)
