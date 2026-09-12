package org.sjbtimdan.linden

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.createLindenDatabase
import org.sjbtimdan.linden.data.createTestSqlDriver
import org.sjbtimdan.linden.model.Currency

class AppDependenciesTest : StringSpec({
    "createAppDependencies reports firstRun and defers seeding on a fresh database" {
        val dependencies = createAppDependencies(createTestSqlDriver())

        dependencies.firstRun shouldBe true
        AccountDao(dependencies.database.accountQueries).getAll().first() shouldBe emptyList()
    }

    "createAppDependencies seeds and reports not-first-run once a currency is set" {
        val driver = createTestSqlDriver()
        val database = createLindenDatabase(driver)
        SettingsDao(database.settingsQueries).setDefaultCurrency(Currency.EUR)

        val dependencies = createAppDependencies(driver)

        dependencies.firstRun shouldBe false
        val accounts = AccountDao(dependencies.database.accountQueries).getAll().first()
        accounts.map { it.currency } shouldBe listOf(Currency.EUR, Currency.EUR)
    }
})
