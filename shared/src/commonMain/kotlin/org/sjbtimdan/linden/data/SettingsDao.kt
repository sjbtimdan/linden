package org.sjbtimdan.linden.data

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.sjbtimdan.linden.SettingsQueries
import org.sjbtimdan.linden.model.AppLanguage
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.util.asOneOrNullFlow

const val THEME_KEY = "theme"
const val CURRENCY_KEY = "currency"
const val HIDE_ENTRY_TOTAL_KEY = "hideEntryTotal"
const val AUTO_UPDATE_RATES_KEY = "autoUpdateRates"
const val LANGUAGE_KEY = "language"
const val RATES_SEEN_KEY = "ratesSeen"
const val CAPTURE_RULES_KEY = "captureRules"
const val PENDING_CAPTURES_KEY = "pendingCaptures"
const val CAPTURE_SEEN_KEY = "captureSeenId"

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

    fun themeFlow(): Flow<ThemeMode> = rawFlow(THEME_KEY)
        .map { stored ->
            stored?.let { value -> ThemeMode.entries.firstOrNull { it.name == value } } ?: ThemeMode.SYSTEM
        }

    suspend fun getDefaultCurrency(): Currency {
        val entity = queries.selectByKey(CURRENCY_KEY).awaitAsOneOrNull()
        return if (entity == null) {
            Currency.CHF
        } else {
            parseCurrency(entity.value_) ?: Currency.CHF
        }
    }

    /** True once the user has chosen a default currency (false on a fresh install). */
    suspend fun hasDefaultCurrency(): Boolean = queries.selectByKey(CURRENCY_KEY).awaitAsOneOrNull() != null

    suspend fun setDefaultCurrency(currency: Currency) {
        queries.insertOrReplace(CURRENCY_KEY, currency.name)
    }

    fun defaultCurrencyFlow(): Flow<Currency> = rawFlow(CURRENCY_KEY)
        .map { value -> value?.let(::parseCurrency) ?: Currency.CHF }

    suspend fun getHideEntryTotal(): Boolean {
        val entity = queries.selectByKey(HIDE_ENTRY_TOTAL_KEY).awaitAsOneOrNull()
        return entity?.value_?.toBoolean() == true
    }

    suspend fun setHideEntryTotal(hidden: Boolean) {
        queries.insertOrReplace(HIDE_ENTRY_TOTAL_KEY, hidden.toString())
    }

    fun hideEntryTotalFlow(): Flow<Boolean> = rawFlow(HIDE_ENTRY_TOTAL_KEY)
        .map { it?.toBoolean() == true }

    suspend fun setAutoUpdateRates(enabled: Boolean) {
        queries.insertOrReplace(AUTO_UPDATE_RATES_KEY, enabled.toString())
    }

    suspend fun getLanguage(): AppLanguage {
        val entity = queries.selectByKey(LANGUAGE_KEY).awaitAsOneOrNull() ?: return AppLanguage.SYSTEM
        return AppLanguage.fromTag(entity.value_)
    }

    suspend fun setLanguage(language: AppLanguage) {
        queries.insertOrReplace(LANGUAGE_KEY, language.tag ?: "system")
    }

    fun languageFlow(): Flow<AppLanguage> = rawFlow(LANGUAGE_KEY)
        .map { stored -> AppLanguage.fromTag(stored) }

    fun autoUpdateRatesFlow(): Flow<Boolean> = rawFlow(AUTO_UPDATE_RATES_KEY)
        .map { it?.toBoolean() ?: true }

    suspend fun setRatesSeen(seen: Boolean) {
        queries.insertOrReplace(RATES_SEEN_KEY, seen.toString())
    }

    fun ratesSeenFlow(): Flow<Boolean> = rawFlow(RATES_SEEN_KEY)
        .map { it?.toBoolean() == true }

    /** Stored value of a single settings key; null while the key is absent. */
    suspend fun getRaw(key: String): String? = queries.selectByKey(key).awaitAsOneOrNull()?.value_

    suspend fun setRaw(key: String, value: String) {
        queries.insertOrReplace(key, value)
    }

    /** Reactive raw value of a single settings key; null while the key is absent. */
    fun rawFlow(key: String): Flow<String?> = queries.selectByKey(key)
        .asOneOrNullFlow()
        .map { it?.value_ }

    private fun parseCurrency(code: String): Currency? = try {
        Currency.fromCode(code)
    } catch (_: IllegalStateException) {
        null
    }
}
