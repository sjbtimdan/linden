package org.sjbtimdan.linden.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@OptIn(ExperimentalTestApi::class)
class PressFeedbackTest : StringSpec({

    "a tap fires the haptic once" {
        onTestMain {
            runComposeUiTest {
                var haptics = 0
                setContent {
                    val source = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .pressFeedback(source, haptic = { haptics++ })
                            .clickable(interactionSource = source, indication = null) {}
                            .testTag("card"),
                    )
                }

                onNodeWithTag("card").performTouchInput {
                    down(center)
                    up()
                }
                waitForIdle()

                haptics shouldBe 1
            }
        }
    }

    "a cancelled press does not fire the haptic" {
        onTestMain {
            runComposeUiTest {
                var haptics = 0
                setContent {
                    val source = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .pressFeedback(source, haptic = { haptics++ })
                            .clickable(interactionSource = source, indication = null) {}
                            .testTag("card"),
                    )
                }

                onNodeWithTag("card").performTouchInput {
                    down(center)
                    cancel()
                }
                waitForIdle()

                haptics shouldBe 0
            }
        }
    }
})
