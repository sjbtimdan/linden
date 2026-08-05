package org.sjbtimdan.linden.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val dir = java.io.File(System.getProperty("user.home"), ".linden")
        dir.mkdirs()
        return JdbcSqliteDriver("jdbc:sqlite:${dir.absolutePath}/linden.db")
    }
}
