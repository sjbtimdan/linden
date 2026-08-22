package org.sjbtimdan.linden.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        return parseCurrency(entity.value_) ?: Currency.CHF
    }

    suspend fun setDefaultCurrency(currency: Currency) {
        queries.insertOrReplace(CURRENCY_KEY, currency.name)
    }

    fun defaultCurrencyFlow(): Flow<Currency> = queries.selectAll()
        .asFlow()
        .map { rows ->
            rows.awaitAsList()
                .firstOrNull { it.key == CURRENCY_KEY }
                ?.let { row -> parseCurrency(row.value_) }
                ?: Currency.CHF
        }

    private fun parseCurrency(code: String): Currency? = try {
        Currency.fromCode(code)
    } catch (_: IllegalStateException) {
        null
    }
}
