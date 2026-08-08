package org.sjbtimdan.linden.ui.settings

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.ui.onTestMain

class SettingsViewModelTest : StringSpec({
    "setThemeMode(LIGHT) updates the database" {
        onTestMain {
            val database = lindenDatabase()
            val dao = SettingsDao(database.settingsQueries)
            val viewModel = SettingsViewModel(dao, initialTheme = ThemeMode.SYSTEM)

            viewModel.themeMode.value shouldBe ThemeMode.SYSTEM

            viewModel.setThemeMode(ThemeMode.LIGHT)

            dao.getTheme() shouldBe ThemeMode.LIGHT
        }
    }
})
