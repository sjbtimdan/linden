package org.sjbtimdan.linden.data

import app.cash.sqldelight.db.SqlDriver
import org.sjbtimdan.linden.db.LindenDatabase
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
)

suspend fun createAppDependencies(driver: SqlDriver): AppDependencies {
    val database = createLindenDatabase(driver)
    val initialTheme = SettingsDao(database.settingsQueries).getTheme()
    return AppDependencies(database, initialTheme)
}
