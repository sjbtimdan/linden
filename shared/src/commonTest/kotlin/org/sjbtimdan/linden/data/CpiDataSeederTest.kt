package org.sjbtimdan.linden.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.inflation.CpiSeedData
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.onTestMain

class CpiDataSeederTest : StringSpec({

    "seeds the bundled snapshot for every currency" {
        onTestMain {
            val database = lindenDatabase()
            CpiDataSeeder(database).seedIfEmpty()

            val series = CpiDao(database.cpiQueries).seriesFlow().first { it.isNotEmpty() }

            series.keys shouldBe Currency.entries.toSet()
            Currency.entries.forEach { currency ->
                val expected = CpiSeedData.series.getValue(currency.name)
                val stored = series.getValue(currency)
                stored.firstMonth shouldBe expected.firstMonth
                stored.latestMonth shouldBe expected.latestMonth
                stored.values.toList() shouldBe expected.values.toList()
            }
        }
    }

    "a second seed is a no-op so stored rows are never overwritten" {
        onTestMain {
            val database = lindenDatabase()
            val seeder = CpiDataSeeder(database)
            seeder.seedIfEmpty()
            database.cpiQueries.insertOrReplace("CHF", "2030-01", 1)
            seeder.seedIfEmpty()

            database.cpiQueries.selectByCurrency("CHF").executeAsList().last().month shouldBe "2030-01"
        }
    }
})
