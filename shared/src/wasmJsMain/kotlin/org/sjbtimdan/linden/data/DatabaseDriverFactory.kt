package org.sjbtimdan.linden.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return WebWorkerDriver(Worker("sql.js/worker.sql-wasm.js"))
    }
}
