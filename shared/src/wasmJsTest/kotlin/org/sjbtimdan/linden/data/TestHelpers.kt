package org.sjbtimdan.linden.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.createDefaultWebWorkerDriver

actual fun createTestSqlDriver(): SqlDriver = createDefaultWebWorkerDriver()
