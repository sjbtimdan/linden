package org.sjbtimdan.linden.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.Screen
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.ui.theme.LindenTheme

@OptIn(ExperimentalTestApi::class)
class FloatingNavigationBarTest : StringSpec({

    "the selected tab follows taps" {
        onTestMain {
            runComposeUiTest {
                var selected by mutableStateOf<Screen>(Screen.Entry)
                setContent {
                    LindenTheme(themeMode = ThemeMode.LIGHT) {
                        val hazeState = rememberHazeState()
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .hazeSource(hazeState),
                            )
                            FloatingNavigationBar(
                                selected = selected,
                                onSelect = { selected = it },
                                hazeState = hazeState,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    }
                }

                onNodeWithText("Entry").assertIsSelected()
                onNodeWithText("Ledger").assertIsNotSelected()

                onNodeWithText("Ledger").performClick()
                waitForIdle()

                onNodeWithText("Ledger").assertIsSelected()
                onNodeWithText("Entry").assertIsNotSelected()
            }
        }
    }

    "the pill is capped and centred on a wide window" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    LindenTheme(themeMode = ThemeMode.LIGHT) {
                        val hazeState = rememberHazeState()
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .hazeSource(hazeState),
                            )
                            FloatingNavigationBar(
                                selected = Screen.Entry,
                                onSelect = {},
                                hazeState = hazeState,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    }
                }

                val rootWidth = onRoot().getUnclippedBoundsInRoot().let { it.right - it.left }
                val bounds = onNodeWithTag("bottomNavPill").getUnclippedBoundsInRoot()
                (bounds.right - bounds.left) shouldBe FloatingNavigationBarMaxWidth
                bounds.left shouldBe (rootWidth - FloatingNavigationBarMaxWidth) / 2f
            }
        }
    }
})
