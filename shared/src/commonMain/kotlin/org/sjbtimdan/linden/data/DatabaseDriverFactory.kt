package org.sjbtimdan.linden.data

import app.cash.sqldelight.db.SqlDriver
import org.sjbtimdan.linden.db.LindenDatabase
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

suspend fun createLindenDatabase(driver: SqlDriver): LindenDatabase {
    LindenDatabase.Schema.create(driver).await()
    return LindenDatabase(driver)
}

data class AppDependencies(
    val database: LindenDatabase,
    val initialTheme: ThemeMode,
    val initialCurrency: Currency,
)

suspend fun createAppDependencies(driver: SqlDriver): AppDependencies {
    val database = createLindenDatabase(driver)
    val settingsDao = SettingsDao(database.settingsQueries)
    val initialTheme = settingsDao.getTheme()
    val initialCurrency = settingsDao.getDefaultCurrency()
    return AppDependencies(database, initialTheme, initialCurrency)
}
