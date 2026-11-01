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
const val CAPTURE_TEMPLATES_KEY = "captureTemplates"
const val CAPTURE_PROCESSED_KEY = "captureProcessed"

class SettingsDao(private val queries: SettingsQueries) {
    private class Setting<T>(
        val key: String,
        val defaultValue: T,
        val encode: (T) -> String,
        val decode: (String) -> T,
    )

    private val themeSetting = Setting(
        key = THEME_KEY,
        defaultValue = ThemeMode.SYSTEM,
        encode = { it.name },
        decode = { value -> ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.SYSTEM },
    )

    private val currencySetting = Setting(
        key = CURRENCY_KEY,
        defaultValue = Currency.CHF,
        encode = { it.name },
        decode = { value -> parseCurrency(value) ?: Currency.CHF },
    )

    private val hideEntryTotalSetting = Setting(
        key = HIDE_ENTRY_TOTAL_KEY,
        defaultValue = false,
        encode = { it.toString() },
        decode = { it.toBoolean() },
    )

    private val autoUpdateRatesSetting = Setting(
        key = AUTO_UPDATE_RATES_KEY,
        defaultValue = true,
        encode = { it.toString() },
        decode = { it.toBoolean() },
    )

    private val languageSetting = Setting(
        key = LANGUAGE_KEY,
        defaultValue = AppLanguage.SYSTEM,
        encode = { it.tag ?: "system" },
        decode = { value -> AppLanguage.fromTag(value) },
    )

    private val ratesSeenSetting = Setting(
        key = RATES_SEEN_KEY,
        defaultValue = false,
        encode = { it.toString() },
        decode = { it.toBoolean() },
    )

    suspend fun getTheme(): ThemeMode = read(themeSetting)

    suspend fun setTheme(mode: ThemeMode) = write(themeSetting, mode)

    fun themeFlow(): Flow<ThemeMode> = observe(themeSetting)

    suspend fun getDefaultCurrency(): Currency = read(currencySetting)

    /** True once the user has chosen a default currency (false on a fresh install). */
    suspend fun hasDefaultCurrency(): Boolean = queries.selectByKey(CURRENCY_KEY).awaitAsOneOrNull() != null

    suspend fun setDefaultCurrency(currency: Currency) = write(currencySetting, currency)

    fun defaultCurrencyFlow(): Flow<Currency> = observe(currencySetting)

    suspend fun getHideEntryTotal(): Boolean = read(hideEntryTotalSetting)

    suspend fun setHideEntryTotal(hidden: Boolean) = write(hideEntryTotalSetting, hidden)

    fun hideEntryTotalFlow(): Flow<Boolean> = observe(hideEntryTotalSetting)

    suspend fun setAutoUpdateRates(enabled: Boolean) = write(autoUpdateRatesSetting, enabled)

    suspend fun getLanguage(): AppLanguage = read(languageSetting)

    suspend fun setLanguage(language: AppLanguage) = write(languageSetting, language)

    fun languageFlow(): Flow<AppLanguage> = observe(languageSetting)

    fun autoUpdateRatesFlow(): Flow<Boolean> = observe(autoUpdateRatesSetting)

    suspend fun setRatesSeen(seen: Boolean) = write(ratesSeenSetting, seen)

    fun ratesSeenFlow(): Flow<Boolean> = observe(ratesSeenSetting)

    /** Stored value of a single settings key; null while the key is absent. */
    suspend fun getRaw(key: String): String? = queries.selectByKey(key).awaitAsOneOrNull()?.value_

    suspend fun setRaw(key: String, value: String) {
        queries.insertOrReplace(key, value)
    }

    /** Reactive raw value of a single settings key; null while the key is absent. */
    fun rawFlow(key: String): Flow<String?> = queries.selectByKey(key)
        .asOneOrNullFlow()
        .map { it?.value_ }

    private suspend fun <T> read(setting: Setting<T>): T =
        queries.selectByKey(setting.key).awaitAsOneOrNull()?.value_?.let(setting.decode) ?: setting.defaultValue

    private suspend fun <T> write(setting: Setting<T>, value: T) {
        queries.insertOrReplace(setting.key, setting.encode(value))
    }

    private fun <T> observe(setting: Setting<T>): Flow<T> =
        rawFlow(setting.key).map { stored -> stored?.let(setting.decode) ?: setting.defaultValue }
}

private fun parseCurrency(code: String): Currency? = try {
    Currency.fromCode(code)
} catch (_: IllegalStateException) {
    null
}
