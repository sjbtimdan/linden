package org.sjbtimdan.linden.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@OptIn(ExperimentalTestApi::class)
class LayoutTest : StringSpec({
    "the content column is capped and centred on a wide window" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    Box(Modifier.requiredSize(width = 1000.dp, height = 800.dp)) {
                        Box(Modifier.screenContainer()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("content"),
                            )
                        }
                    }
                }

                val bounds = onNodeWithTag("content").getUnclippedBoundsInRoot()
                // The column is 720 wide minus its 8dp edge padding on each side,
                // centred in the 1000dp window.
                (bounds.right - bounds.left) shouldBe 704.dp
                bounds.left shouldBe 148.dp
            }
        }
    }

    "the content column still fills a narrow window" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    Box(Modifier.requiredSize(width = 360.dp, height = 800.dp)) {
                        Box(Modifier.screenContainer()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("content"),
                            )
                        }
                    }
                }

                val bounds = onNodeWithTag("content").getUnclippedBoundsInRoot()
                (bounds.right - bounds.left) shouldBe 344.dp
                bounds.left shouldBe 8.dp
            }
        }
    }
})
