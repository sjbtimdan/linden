package org.sjbtimdan.linden.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.sjbtimdan.linden.db.LindenDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val dir = java.io.File(System.getProperty("user.home"), ".linden")
        dir.mkdirs()
        return createDriverAt(java.io.File(dir, "linden.db"))
    }

    /**
     * Opens (and, for existing files, migrates) the database at [dbFile]; exposed for tests.
     *
     * Unlike the Android driver, [JdbcSqliteDriver] never migrates by itself, so the
     * schema is handed to it here: a brand-new (or empty) file is created from the
     * current schema and stamped; an existing file is migrated from its stored
     * `user_version` up to the current schema, stamping as it goes. Older desktop
     * builds never stamped the version, so their files read 0 — [migrateEmptySchema]
     * treats those as an old database to migrate rather than an empty one to create.
     */
    internal fun createDriverAt(dbFile: java.io.File): SqlDriver {
        dbFile.parentFile?.mkdirs()
        val migratedFromEmpty = dbFile.exists() && dbFile.length() > 0
        return JdbcSqliteDriver(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            schema = LindenDatabase.Schema.synchronous(),
            migrateEmptySchema = migratedFromEmpty,
        )
    }
}
