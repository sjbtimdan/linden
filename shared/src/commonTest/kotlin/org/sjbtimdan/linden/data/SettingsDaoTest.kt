package org.sjbtimdan.linden.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.model.Currency
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

    "getDefaultCurrency returns CHF when no setting exists" {
        val database = lindenDatabase()
        val dao = SettingsDao(database.settingsQueries)
        dao.getDefaultCurrency() shouldBe Currency.CHF
    }

    "setDefaultCurrency then getDefaultCurrency round-trips correctly" {
        val database = lindenDatabase()
        val dao = SettingsDao(database.settingsQueries)
        dao.setDefaultCurrency(Currency.EUR)
        dao.getDefaultCurrency() shouldBe Currency.EUR
    }

    "getDefaultCurrency falls back to CHF for an unknown stored value" {
        val database = lindenDatabase()
        val dao = SettingsDao(database.settingsQueries)
        database.settingsQueries.insertOrReplace(CURRENCY_KEY, "NOPE")
        dao.getDefaultCurrency() shouldBe Currency.CHF
    }

    "defaultCurrencyFlow emits CHF by default and follows updates" {
        val database = lindenDatabase()
        val dao = SettingsDao(database.settingsQueries)
        dao.defaultCurrencyFlow().first() shouldBe Currency.CHF
        dao.setDefaultCurrency(Currency.USD)
        dao.defaultCurrencyFlow().first() shouldBe Currency.USD
    }
})
