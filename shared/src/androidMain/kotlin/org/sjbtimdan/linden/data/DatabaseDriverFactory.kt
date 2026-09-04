package org.sjbtimdan.linden.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.sjbtimdan.linden.db.LindenDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val schema = LindenDatabase.Schema.synchronous()
        return AndroidSqliteDriver(
            schema,
            context,
            "linden.db",
            callback = object : AndroidSqliteDriver.Callback(schema) {
                override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // Open databases stamped by a newer app version instead of crashing: all
                    // schema changes so far are additive, so the extra columns are ignored safely.
                }
            },
        )
    }
}
