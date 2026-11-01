package org.sjbtimdan.linden.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class ThemeTest : StringSpec({

    "AMOLED uses a true black background and surface" {
        onTestMain {
            runComposeUiTest {
                var background: Color? = null
                var surface: Color? = null
                setContent {
                    LindenTheme(themeMode = ThemeMode.AMOLED) {
                        val scheme = MaterialTheme.colorScheme
                        SideEffect {
                            background = scheme.background
                            surface = scheme.surface
                        }
                    }
                }
                waitForIdle()

                background shouldBe Color.Black
                surface shouldBe Color.Black
            }
        }
    }

    "dark mode keeps its tinted background" {
        onTestMain {
            runComposeUiTest {
                var background: Color? = null
                setContent {
                    LindenTheme(themeMode = ThemeMode.DARK) {
                        val scheme = MaterialTheme.colorScheme
                        SideEffect { background = scheme.background }
                    }
                }
                waitForIdle()

                background shouldBe Color(0xFF101410)
            }
        }
    }
})
