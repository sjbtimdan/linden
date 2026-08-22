package org.sjbtimdan.linden.data

import app.cash.sqldelight.db.SqlDriver
import org.sjbtimdan.linden.db.LindenDatabase

@Suppress("KotlinNoActualForExpect")
expect fun createTestSqlDriver(): SqlDriver

suspend fun lindenDatabase(): LindenDatabase = createLindenDatabase(createTestSqlDriver())
