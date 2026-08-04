package org.sjbtimdan.linden.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.ThemeMode

class SettingsDaoTest : StringSpec({
    "getTheme returns SYSTEM when no setting exists" {
        val database = lindenDatabase()
        val dao = SettingsDao(database.settingsQueries)
        dao.getTheme() shouldBe ThemeMode.SYSTEM
    }

    "setTheme then getTheme round-trips correctly" {
        val database = lindenDatabase()
        val dao = SettingsDao(database.settingsQueries)
        dao.setTheme(ThemeMode.DARK)
        dao.getTheme() shouldBe ThemeMode.DARK
    }
})
