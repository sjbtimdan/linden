package org.sjbtimdan.linden.data

import app.cash.sqldelight.db.SqlDriver
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull

class DatabaseDriverFactorySmokeTest : StringSpec({
    "createTestSqlDriver should produce a working SqlDriver" {
        val driver: SqlDriver = createTestSqlDriver()
        driver.shouldNotBeNull()
        driver.close()
    }
})
