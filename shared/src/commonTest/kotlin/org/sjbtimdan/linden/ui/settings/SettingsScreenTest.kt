package org.sjbtimdan.linden.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.ui.withSettingsViewModel

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest : StringSpec({
    "clicking Light segment sets theme" {
        withSettingsViewModel(initialTheme = ThemeMode.SYSTEM) { viewModel ->
            setContent { SettingsScreen(viewModel) }
            onNodeWithText("System").assertIsSelected()
            onNodeWithText("Light").performClick()
            onNodeWithText("Light").assertIsSelected()

            viewModel.themeMode.value shouldBe ThemeMode.LIGHT
        }
    }

    "clicking Categories row triggers navigation" {
        withSettingsViewModel { viewModel ->
            var navigatedToCategories = false

            setContent {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToCategories = { navigatedToCategories = true },
                )
            }
            onNodeWithText("Categories").performClick()
            navigatedToCategories shouldBe true
        }
    }

    "clicking Accounts row triggers navigation" {
        withSettingsViewModel { viewModel ->
            var navigatedToAccounts = false

            setContent {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToAccounts = { navigatedToAccounts = true },
                )
            }
            onNodeWithText("Accounts").performClick()
            navigatedToAccounts shouldBe true
        }
    }
})
