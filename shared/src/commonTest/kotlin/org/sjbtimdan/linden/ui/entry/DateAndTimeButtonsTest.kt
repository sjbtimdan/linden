package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.ui.onTestMain
import kotlin.time.Instant

private val createdAt = Instant.parse("2026-08-10T14:30:00Z")
private val createdZone = TimeZone.UTC

@OptIn(ExperimentalTestApi::class)
class DateAndTimeButtonsTest : StringSpec({
    "shows the date and time buttons" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    DateAndTimeButtons(
                        createdAt = createdAt,
                        createdZone = createdZone,
                        onChange = {},
                    )
                }

                onNodeWithText("Date & time").assertIsDisplayed()
                onNodeWithText("10 Aug 2026").assertIsDisplayed()
                onNodeWithText("14:30").assertIsDisplayed()
            }
        }
    }

    "changing the date in the date picker keeps the time of day" {
        onTestMain {
            runComposeUiTest {
                var changedTo: Instant? = null
                setContent {
                    DateAndTimeButtons(
                        createdAt = createdAt,
                        createdZone = createdZone,
                        onChange = { changedTo = it },
                    )
                }

                onNodeWithText("10 Aug 2026").performClick()
                waitForIdle()
                onNodeWithText("Saturday, August 15, 2026").performClick()
                onNodeWithText("OK").performClick()
                waitForIdle()

                changedTo shouldBe Instant.parse("2026-08-15T14:30:00Z")
            }
        }
    }

    "cancelling the date picker does not call onChange" {
        onTestMain {
            runComposeUiTest {
                var changedTo: Instant? = null
                setContent {
                    DateAndTimeButtons(
                        createdAt = createdAt,
                        createdZone = createdZone,
                        onChange = { changedTo = it },
                    )
                }

                onNodeWithText("10 Aug 2026").performClick()
                waitForIdle()
                onNodeWithText("Cancel").performClick()
                waitForIdle()

                changedTo.shouldBeNull()
            }
        }
    }

    "changing the time in the time picker keeps the date" {
        onTestMain {
            runComposeUiTest {
                var changedTo: Instant? = null
                setContent {
                    DateAndTimeButtons(
                        createdAt = createdAt,
                        createdZone = createdZone,
                        onChange = { changedTo = it },
                    )
                }

                onNodeWithText("14:30").performClick()
                waitForIdle()
                onNodeWithContentDescription("15 hours").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithContentDescription("30 minutes").fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithText("OK").performClick()
                waitForIdle()

                changedTo shouldBe Instant.parse("2026-08-10T15:30:00Z")
            }
        }
    }

    "cancelling the time picker does not call onChange" {
        onTestMain {
            runComposeUiTest {
                var changedTo: Instant? = null
                setContent {
                    DateAndTimeButtons(
                        createdAt = createdAt,
                        createdZone = createdZone,
                        onChange = { changedTo = it },
                    )
                }

                onNodeWithText("14:30").performClick()
                waitForIdle()
                onNodeWithText("Cancel").performClick()
                waitForIdle()

                changedTo.shouldBeNull()
            }
        }
    }
})
