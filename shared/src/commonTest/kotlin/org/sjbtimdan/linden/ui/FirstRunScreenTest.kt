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

@OptIn(ExperimentalTestApi::class)
class FirstRunScreenTest : StringSpec({
    "shows the welcome copy with the pre-selected language, currency and theme" {
        runFirstRunTest {
            setContent {
                FirstRunScreen(onComplete = { _, _, _ -> })
            }

            onNodeWithText("Welcome to Linden").assertIsDisplayed()
            onNodeWithTag("firstRunLanguage-ENGLISH").assertIsSelected()
            onNodeWithTag("firstRunCurrency-CHF").assertIsSelected()
            onNodeWithTag("firstRunTheme-SYSTEM").assertIsSelected()
            onNodeWithTag("firstRunContinue").assertIsDisplayed()
        }
    }

    "completes with the pre-selected choices without any interaction" {
        runFirstRunTest {
            var completed: Triple<AppLanguage, Currency, ThemeMode>? = null
            setContent {
                FirstRunScreen { language, currency, theme ->
                    completed = Triple(language, currency, theme)
                }
            }

            onNodeWithTag("firstRunContinue").performClick()

            completed shouldBe Triple(AppLanguage.ENGLISH, Currency.CHF, ThemeMode.SYSTEM)
        }
    }

    "completes with the selected currency" {
        runFirstRunTest {
            var completed: Currency? = null
            setContent {
                FirstRunScreen { _, currency, _ -> completed = currency }
            }

            onNodeWithTag("firstRunCurrency-USD").performClick()
            onNodeWithTag("firstRunContinue").performClick()

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

            onNodeWithTag("firstRunLanguage-FRENCH").performClick()
            onNodeWithTag("firstRunTheme-DARK").performClick()
            onNodeWithTag("firstRunContinue").performClick()

            completed shouldBe Triple(AppLanguage.FRENCH, Currency.CHF, ThemeMode.DARK)
        }
    }

    "picking a language previews it immediately" {
        runFirstRunTest {
            setContent {
                FirstRunScreen(onComplete = { _, _, _ -> })
            }

            onNodeWithTag("firstRunLanguage-FRENCH").performClick()

            onNodeWithText(
                "Choisissez votre langue, votre devise et votre thème. " +
                    "Vous pourrez les modifier plus tard dans les Paramètres.",
            ).assertIsDisplayed()
        }
    }
})
