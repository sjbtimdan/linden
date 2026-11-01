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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.kotest.core.spec.style.StringSpec
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
})
