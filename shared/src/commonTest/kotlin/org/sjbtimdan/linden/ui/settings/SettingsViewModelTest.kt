package org.sjbtimdan.linden.ui.settings

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.model.ThemeMode

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest : StringSpec({
    "setThemeMode(LIGHT) updates the database" {
        Dispatchers.setMain(Dispatchers.Unconfined)
        try {
            val database = lindenDatabase()
            val dao = SettingsDao(database.settingsQueries)
            val viewModel = SettingsViewModel(dao)

            viewModel.themeMode.value shouldBe ThemeMode.SYSTEM

            viewModel.setThemeMode(ThemeMode.LIGHT)

            dao.getTheme() shouldBe ThemeMode.LIGHT
        } finally {
            Dispatchers.resetMain()
        }
    }
})
