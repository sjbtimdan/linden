package org.sjbtimdan.linden.data

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import org.sjbtimdan.linden.SettingsQueries
import org.sjbtimdan.linden.model.ThemeMode

const val THEME_KEY = "theme"

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
}
