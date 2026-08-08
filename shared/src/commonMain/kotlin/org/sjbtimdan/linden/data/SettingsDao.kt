package org.sjbtimdan.linden.data

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import org.sjbtimdan.linden.SettingsQueries
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode

const val THEME_KEY = "theme"
const val CURRENCY_KEY = "currency"

class SettingsDao(private val queries: SettingsQueries) {
    suspend fun getTheme(): ThemeMode {
        val entity = queries.selectByKey(THEME_KEY).awaitAsOneOrNull() ?: return ThemeMode.SYSTEM
        return try {
            ThemeMode.valueOf(entity.value_)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    suspend fun setTheme(mode: ThemeMode) {
        queries.insertOrReplace(THEME_KEY, mode.name)
    }

    suspend fun getDefaultCurrency(): Currency {
        val entity = queries.selectByKey(CURRENCY_KEY).awaitAsOneOrNull()
            ?: return Currency.CHF
        return try {
            Currency.fromCode(entity.value_)
        } catch (_: IllegalStateException) {
            Currency.CHF
        }
    }

    suspend fun setDefaultCurrency(currency: Currency) {
        queries.insertOrReplace(CURRENCY_KEY, currency.name)
    }
}
