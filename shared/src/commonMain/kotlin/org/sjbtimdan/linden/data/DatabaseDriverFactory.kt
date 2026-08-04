package org.sjbtimdan.linden.data

import app.cash.sqldelight.db.SqlDriver
import org.sjbtimdan.linden.db.LindenDatabase

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

suspend fun createLindenDatabase(driver: SqlDriver): LindenDatabase {
    LindenDatabase.Schema.create(driver).await()
    return LindenDatabase(driver)
}
