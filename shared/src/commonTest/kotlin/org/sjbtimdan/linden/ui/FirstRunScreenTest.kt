package org.sjbtimdan.linden.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.AppLanguage
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.ui.capture.NotificationAccess
import java.util.Locale

/**
 * Runs a first-run test and restores the JVM locale afterwards: the screen
 * applies the language override during composition, which mutates the default
 * locale.
 */
@OptIn(ExperimentalTestApi::class)
private fun runFirstRunTest(block: suspend ComposeUiTest.() -> Unit) {
    onTestMain {
        runComposeUiTest {
            val originalLocale = Locale.getDefault()
            try {
                block()
            } finally {
                Locale.setDefault(originalLocale)
            }
        }
    }
}

/** Clicks the wizard's Continue/Get started button [times] times. */
@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.advance(times: Int = 1) {
    repeat(times) { onNodeWithTag("firstRunContinue").performClick() }
}

private fun access(supported: Boolean, granted: Boolean = false, onOpen: () -> Unit = {}) =
    NotificationAccess(supported = supported, granted = granted, openSettings = onOpen)

@OptIn(ExperimentalTestApi::class)
class FirstRunScreenTest : StringSpec({

    "firstRunSteps omits capture when unsupported and includes it when supported" {
        firstRunSteps(captureSupported = false) shouldBe
            listOf(FirstRunStep.Welcome, FirstRunStep.Language, FirstRunStep.Currency, FirstRunStep.Theme)

        firstRunSteps(captureSupported = true) shouldBe
            listOf(
                FirstRunStep.Welcome,
                FirstRunStep.Language,
                FirstRunStep.Currency,
                FirstRunStep.Theme,
                FirstRunStep.Capture,
            )
    }

    "shows the welcome copy and features on the first step" {
        runFirstRunTest {
            setContent {
                FirstRunScreen(onComplete = { _, _, _ -> })
            }

            onNodeWithText("Welcome to Linden").assertIsDisplayed()
            onNodeWithText("Track expenses and income across accounts and currencies.").assertIsDisplayed()
            onNodeWithText("Multiple accounts and currencies").assertIsDisplayed()
            onNodeWithText("Stored on your device").assertIsDisplayed()
            onNodeWithText("Fast entry").assertIsDisplayed()
            onNodeWithText("Ledger").assertIsDisplayed()
            onNodeWithText("Get started").assertIsDisplayed()
        }
    }

    "advances through the steps with the pre-selected language, currency and theme" {
        runFirstRunTest {
            setContent {
                FirstRunScreen(onComplete = { _, _, _ -> })
            }

            advance()
            onNodeWithText("Choose your language").assertIsDisplayed()
            onNodeWithTag("firstRunLanguage-ENGLISH").assertIsSelected()

            advance()
            onNodeWithText("Choose your currency").assertIsDisplayed()
            onNodeWithTag("firstRunCurrency-CHF").assertIsSelected()

            advance()
            onNodeWithText("Choose your theme").assertIsDisplayed()
            onNodeWithTag("firstRunTheme-SYSTEM").assertIsSelected()
        }
    }

    "completes with the pre-selected choices by advancing to the end" {
        runFirstRunTest {
            var completed: Triple<AppLanguage, Currency, ThemeMode>? = null
            setContent {
                FirstRunScreen { language, currency, theme ->
                    completed = Triple(language, currency, theme)
                }
            }

            advance(4)

            completed shouldBe Triple(AppLanguage.ENGLISH, Currency.CHF, ThemeMode.SYSTEM)
        }
    }

    "completes with the selected currency" {
        runFirstRunTest {
            var completed: Currency? = null
            setContent {
                FirstRunScreen { _, currency, _ -> completed = currency }
            }

            advance(2)
            onNodeWithTag("firstRunCurrency-USD").performClick()
            advance(2)

            completed shouldBe Currency.USD
        }
    }

    "completes with the selected language and theme" {
        runFirstRunTest {
            var completed: Triple<AppLanguage, Currency, ThemeMode>? = null
            setContent {
                FirstRunScreen { language, currency, theme ->
                    completed = Triple(language, currency, theme)
                }
            }

            advance()
            onNodeWithTag("firstRunLanguage-FRENCH").performClick()
            advance(2)
            onNodeWithTag("firstRunTheme-DARK").performClick()
            advance()

            completed shouldBe Triple(AppLanguage.FRENCH, Currency.CHF, ThemeMode.DARK)
        }
    }

    "completes with the AMOLED theme" {
        runFirstRunTest {
            var completed: ThemeMode? = null
            setContent {
                FirstRunScreen { _, _, theme -> completed = theme }
            }

            advance(3)
            onNodeWithTag("firstRunTheme-AMOLED").performClick()
            advance()

            completed shouldBe ThemeMode.AMOLED
        }
    }

    "picking a language previews it immediately" {
        runFirstRunTest {
            setContent {
                FirstRunScreen(onComplete = { _, _, _ -> })
            }

            advance()
            onNodeWithTag("firstRunLanguage-FRENCH").performClick()

            onNodeWithText("Continuer").assertIsDisplayed()
        }
    }

    "back returns to the previous step" {
        runFirstRunTest {
            setContent {
                FirstRunScreen(onComplete = { _, _, _ -> })
            }

            advance()
            onNodeWithText("Choose your language").assertIsDisplayed()
            onNodeWithTag("firstRunBack").performClick()

            onNodeWithText("Welcome to Linden").assertIsDisplayed()
        }
    }

    "payment capture is introduced with its grant on its own step when supported" {
        runFirstRunTest {
            var opened = false
            setContent {
                FirstRunScreen(
                    onComplete = { _, _, _ -> },
                    access = access(supported = true, onOpen = { opened = true }),
                )
            }

            advance(4)
            onNodeWithText("Payment capture").assertIsDisplayed()
            onNodeWithTag("firstRunCaptureAccess").assertIsDisplayed()
            onNodeWithTag("firstRunCaptureFind").assertIsDisplayed()
            onNodeWithTag("firstRunCaptureAllowed").assertDoesNotExist()

            onNodeWithTag("firstRunCaptureAccess").performClick()

            opened shouldBe true
        }
    }

    "payment capture is not offered where the platform cannot capture" {
        runFirstRunTest {
            setContent {
                FirstRunScreen(onComplete = { _, _, _ -> }, access = access(supported = false))
            }

            advance(4)

            onNodeWithText("Payment capture").assertDoesNotExist()
            onNodeWithTag("firstRunCaptureAccess").assertDoesNotExist()
        }
    }

    "granted notification access replaces the button with its state" {
        runFirstRunTest {
            setContent {
                FirstRunScreen(
                    onComplete = { _, _, _ -> },
                    access = access(supported = true, granted = true),
                )
            }

            advance(4)
            onNodeWithTag("firstRunCaptureAllowed").assertIsDisplayed()
            onNodeWithTag("firstRunCaptureAccess").assertDoesNotExist()
            onNodeWithTag("firstRunCaptureFind").assertDoesNotExist()
        }
    }
})
