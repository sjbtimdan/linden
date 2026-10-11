package org.sjbtimdan.linden.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.onTestMain

class CpiDaoTest : StringSpec({

    "seriesFlow groups stored months into per-currency series in month order" {
        onTestMain {
            val database = lindenDatabase()
            val dao = CpiDao(database.cpiQueries)
            database.cpiQueries.insertOrReplace("CHF", "2020-02", 10_100)
            database.cpiQueries.insertOrReplace("CHF", "2020-01", 10_000)
            database.cpiQueries.insertOrReplace("USD", "2020-01", 20_000)

            val series = dao.seriesFlow().first { it.isNotEmpty() }

            series.keys shouldBe setOf(Currency.CHF, Currency.USD)
            val chf = series.getValue(Currency.CHF)
            chf.firstMonth shouldBe "2020-01"
            chf.latestMonth shouldBe "2020-02"
            chf.values.toList() shouldBe listOf(10_000, 10_100)
        }
    }
})
