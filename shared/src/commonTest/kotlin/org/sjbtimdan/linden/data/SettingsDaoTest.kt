package org.sjbtimdan.linden.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.model.AppLanguage
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

    "themeFlow emits SYSTEM by default, follows updates and falls back on unknown values" {
        val database = lindenDatabase()
        val dao = SettingsDao(database.settingsQueries)
        dao.themeFlow().first() shouldBe ThemeMode.SYSTEM
        dao.setTheme(ThemeMode.LIGHT)
        dao.themeFlow().first() shouldBe ThemeMode.LIGHT
        database.settingsQueries.insertOrReplace(THEME_KEY, "NOPE")
        dao.themeFlow().first() shouldBe ThemeMode.SYSTEM
    }

    "languageFlow emits SYSTEM by default, follows updates and falls back on unknown values" {
        val database = lindenDatabase()
        val dao = SettingsDao(database.settingsQueries)
        dao.languageFlow().first() shouldBe AppLanguage.SYSTEM
        dao.setLanguage(AppLanguage.FRENCH)
        dao.languageFlow().first() shouldBe AppLanguage.FRENCH
        database.settingsQueries.insertOrReplace(LANGUAGE_KEY, "nope")
        dao.languageFlow().first() shouldBe AppLanguage.SYSTEM
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

    "hasDefaultCurrency is false on a fresh database and true once set" {
        val database = lindenDatabase()
        val dao = SettingsDao(database.settingsQueries)
        dao.hasDefaultCurrency() shouldBe false
        dao.setDefaultCurrency(Currency.EUR)
        dao.hasDefaultCurrency() shouldBe true
    }

    "defaultCurrencyFlow emits CHF by default and follows updates" {
        val database = lindenDatabase()
        val dao = SettingsDao(database.settingsQueries)
        dao.defaultCurrencyFlow().first() shouldBe Currency.CHF
        dao.setDefaultCurrency(Currency.USD)
        dao.defaultCurrencyFlow().first() shouldBe Currency.USD
    }

    "autoUpdateRatesFlow emits true by default and follows updates" {
        val database = lindenDatabase()
        val dao = SettingsDao(database.settingsQueries)
        dao.autoUpdateRatesFlow().first() shouldBe true
        dao.setAutoUpdateRates(false)
        dao.autoUpdateRatesFlow().first() shouldBe false
    }

    "ratesSeenFlow emits false by default and follows updates" {
        val database = lindenDatabase()
        val dao = SettingsDao(database.settingsQueries)
        dao.ratesSeenFlow().first() shouldBe false
        dao.setRatesSeen(true)
        dao.ratesSeenFlow().first() shouldBe true
    }
})
