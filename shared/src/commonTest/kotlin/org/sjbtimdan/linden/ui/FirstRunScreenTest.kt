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
import io.kotest.matchers.shouldNotBe
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

    "firstRunSteps starts on the language and omits capture when unsupported" {
        firstRunSteps(captureSupported = false) shouldBe
            listOf(FirstRunStep.Language, FirstRunStep.Welcome, FirstRunStep.Currency, FirstRunStep.Theme)

        firstRunSteps(captureSupported = true) shouldBe
            listOf(
                FirstRunStep.Language,
                FirstRunStep.Welcome,
                FirstRunStep.Currency,
                FirstRunStep.Theme,
                FirstRunStep.Capture,
            )
    }

    "starts on the language step then shows the welcome copy and features" {
        runFirstRunTest {
            setContent {
                FirstRunScreen(onComplete = { _, _, _, _ -> })
            }

            onNodeWithText("Choose your language").assertIsDisplayed()
            onNodeWithTag("firstRunBack").assertDoesNotExist()
            onNodeWithText("Continue").assertIsDisplayed()

            advance()
            onNodeWithText("Welcome to Linden").assertIsDisplayed()
            onNodeWithText("Linden helps you to track your income and expenses.").assertIsDisplayed()
            onNodeWithText("Multiple currencies").assertIsDisplayed()
            onNodeWithText("Stored on your device").assertIsDisplayed()
            onNodeWithText("Fast entry").assertIsDisplayed()
            onNodeWithText("Ledger").assertIsDisplayed()
            onNodeWithText("Get started").assertIsDisplayed()
        }
    }

    "advances through the steps with the pre-selected language, currency and theme" {
        runFirstRunTest {
            setContent {
                FirstRunScreen(onComplete = { _, _, _, _ -> })
            }

            onNodeWithText("Choose your language").assertIsDisplayed()
            onNodeWithTag("firstRunLanguage-ENGLISH").assertIsSelected()

            advance()
            onNodeWithText("Welcome to Linden").assertIsDisplayed()

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
                FirstRunScreen { language, currency, theme, _ ->
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
                FirstRunScreen { _, currency, _, _ -> completed = currency }
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
                FirstRunScreen { language, currency, theme, _ ->
                    completed = Triple(language, currency, theme)
                }
            }

            onNodeWithTag("firstRunLanguage-FRENCH").performClick()
            advance(3)
            onNodeWithTag("firstRunTheme-DARK").performClick()
            advance()

            completed shouldBe Triple(AppLanguage.FRENCH, Currency.CHF, ThemeMode.DARK)
        }
    }

    "completes with the AMOLED theme" {
        runFirstRunTest {
            var completed: ThemeMode? = null
            setContent {
                FirstRunScreen { _, _, theme, _ -> completed = theme }
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
                FirstRunScreen(onComplete = { _, _, _, _ -> })
            }

            onNodeWithTag("firstRunLanguage-FRENCH").performClick()

            onNodeWithText("Continuer").assertIsDisplayed()
        }
    }

    "the steps after the language choice render in the picked language" {
        runFirstRunTest {
            setContent {
                FirstRunScreen(onComplete = { _, _, _, _ -> })
            }

            onNodeWithTag("firstRunLanguage-FRENCH").performClick()

            advance()
            onNodeWithText("Bienvenue dans Linden").assertIsDisplayed()

            advance()
            onNodeWithText("Choisissez votre devise").assertIsDisplayed()

            advance()
            onNodeWithText("Choisissez votre thème").assertIsDisplayed()
        }
    }

    "back returns to the language step" {
        runFirstRunTest {
            setContent {
                FirstRunScreen(onComplete = { _, _, _, _ -> })
            }

            advance()
            onNodeWithText("Welcome to Linden").assertIsDisplayed()
            onNodeWithTag("firstRunBack").performClick()

            onNodeWithText("Choose your language").assertIsDisplayed()
        }
    }

    "payment capture is introduced with its grant on its own step when supported" {
        runFirstRunTest {
            var opened = false
            setContent {
                FirstRunScreen(
                    onComplete = { _, _, _, _ -> },
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
                FirstRunScreen(onComplete = { _, _, _, _ -> }, access = access(supported = false))
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
                    onComplete = { _, _, _, _ -> },
                    access = access(supported = true, granted = true),
                )
            }

            advance(4)
            onNodeWithTag("firstRunCaptureAllowed").assertIsDisplayed()
            onNodeWithTag("firstRunCaptureAccess").assertDoesNotExist()
            onNodeWithTag("firstRunCaptureFind").assertDoesNotExist()
        }
    }

    "the capture step explains the setup and how apps are added" {
        runFirstRunTest {
            setContent {
                FirstRunScreen(
                    onComplete = { _, _, _, _ -> },
                    access = access(supported = true),
                )
            }

            advance(4)

            onNodeWithText("How it works").assertIsDisplayed()
            onNodeWithText("Grant access to Linden.").assertIsDisplayed()
            onNodeWithText("Finish setup.").assertIsDisplayed()
            onNodeWithText(
                "Open Settings → Payment capture, tap Add app, pick a banking app, " +
                    "then choose the account its payments post to.",
            ).assertIsDisplayed()
            onNodeWithText("You can add or remove apps any time in Settings → Payment capture.")
                .assertIsDisplayed()
            onNodeWithTag("firstRunCaptureChooseApps").assertDoesNotExist()
        }
    }

    "choose apps now completes setup and asks for payment capture" {
        runFirstRunTest {
            var completed: Triple<AppLanguage, Currency, ThemeMode>? = null
            var openCapture: Boolean? = null
            setContent {
                FirstRunScreen(
                    onComplete = { language, currency, theme, capture ->
                        completed = Triple(language, currency, theme)
                        openCapture = capture
                    },
                    access = access(supported = true, granted = true),
                )
            }

            advance(4)
            onNodeWithTag("firstRunCaptureChooseApps").performClick()

            completed shouldBe Triple(AppLanguage.ENGLISH, Currency.CHF, ThemeMode.SYSTEM)
            openCapture shouldBe true
        }
    }

    "continue from the capture step leaves payment capture closed" {
        runFirstRunTest {
            var openCapture: Boolean? = null
            setContent {
                FirstRunScreen(
                    onComplete = { _, _, _, capture -> openCapture = capture },
                    access = access(supported = true, granted = true),
                )
            }

            advance(5)

            openCapture shouldBe false
        }
    }

    "system back is disabled on the first step" {
        runFirstRunTest {
            var backEnabled: Boolean? = null
            setContent {
                FirstRunScreen(
                    onComplete = { _, _, _, _ -> },
                    systemBackHandler = { enabled, _ -> backEnabled = enabled },
                )
            }

            onNodeWithText("Choose your language").assertIsDisplayed()
            onNodeWithTag("firstRunBack").assertDoesNotExist()

            backEnabled shouldBe false
        }
    }

    "system back walks the wizard back instead of leaving it" {
        runFirstRunTest {
            var systemBack: (() -> Unit)? = null
            setContent {
                FirstRunScreen(
                    onComplete = { _, _, _, _ -> },
                    access = access(supported = true),
                    systemBackHandler = { enabled, onBack -> if (enabled) systemBack = onBack },
                )
            }

            advance(4)
            onNodeWithText("Payment capture").assertIsDisplayed()
            waitForIdle()

            systemBack shouldNotBe null
            systemBack!!.invoke()
            waitForIdle()

            onNodeWithText("Choose your theme").assertIsDisplayed()
        }
    }
})
