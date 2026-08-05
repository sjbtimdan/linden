package org.sjbtimdan.linden.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.model.ThemeMode

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest : StringSpec({
    "clicking Light segment sets theme" {
        runComposeUiTest {
            val database = lindenDatabase()
            val dao = SettingsDao(database.settingsQueries)
            val viewModel = SettingsViewModel(dao)

            setContent { SettingsScreen(viewModel) }
            onNodeWithText("System").assertIsSelected()
            onNodeWithText("Light").performClick()
            onNodeWithText("Light").assertIsSelected()

            viewModel.themeMode.value shouldBe ThemeMode.LIGHT
        }
    }
})
