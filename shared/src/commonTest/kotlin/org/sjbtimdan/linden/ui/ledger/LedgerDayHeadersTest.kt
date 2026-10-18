package org.sjbtimdan.linden.ui.ledger

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.IncomeEntry
import kotlin.time.Instant

class LedgerDayHeadersTest : StringSpec({
    val account = Account(1, "Main", Currency.CHF)
    val category = Category(1, "Groceries", CategoryType.Expense)

    val salary = Category(2, "Salary", CategoryType.Income)

    fun expense(id: Long, epochMillis: Long, zone: TimeZone = TimeZone.UTC): ExpenseEntry = ExpenseEntry(
        id = id,
        category = category,
        description = "Coffee",
        account = account,
        amount = 450,
        createdAt = Instant.fromEpochMilliseconds(epochMillis),
        createdZone = zone,
    )

    fun income(id: Long, epochMillis: Long): IncomeEntry = IncomeEntry(
        id = id,
        category = salary,
        description = "Salary",
        account = account,
        amount = 1_000,
        createdAt = Instant.fromEpochMilliseconds(epochMillis),
        createdZone = TimeZone.UTC,
    )

    val day = 86_400_000L

    "empty list produces no items" {
        ledgerListItems(emptyList(), TimeZone.UTC, Currency.CHF, emptyList()) shouldBe emptyList()
    }

    "entries on a single day produce one header" {
        val items = ledgerListItems(
            listOf(expense(1, 0), expense(2, day - 1)),
            TimeZone.UTC,
            Currency.CHF,
            emptyList(),
        )
        items.map { it.key } shouldBe listOf("day-1970-01-01", 1L, 2L)
        items.first().shouldBeInstanceOf<DayHeaderItem>().let { header ->
            header.label shouldBe "Jan 1, 1970"
            header.totalMinor shouldBe -900
        }
    }

    "entries on multiple days produce a header per day" {
        val items = ledgerListItems(
            listOf(
                expense(1, 0),
                expense(2, day - 1),
                expense(3, day),
                expense(4, 2 * day - 1),
                expense(5, 2 * day),
            ),
            TimeZone.UTC,
            Currency.CHF,
            emptyList(),
        )
        items.map { it.key } shouldBe listOf(
            "day-1970-01-01",
            1L,
            2L,
            "day-1970-01-02",
            3L,
            4L,
            "day-1970-01-03",
            5L,
        )
        items.filterIsInstance<DayHeaderItem>().map { it.totalMinor } shouldBe
            listOf(-900L, -900L, -450L)
    }

    "entries are grouped by the classification zone, not their created zone" {
        val la = TimeZone.of("America/Los_Angeles")
        val items = ledgerListItems(
            listOf(
                expense(1, 0, TimeZone.UTC),
                expense(2, 8 * 3_600_000, la),
                expense(3, 32 * 3_600_000, la),
            ),
            TimeZone.UTC,
            Currency.CHF,
            emptyList(),
        )
        items.map { it.key } shouldBe listOf("day-1970-01-01", 1L, 2L, "day-1970-01-02", 3L)
    }

    "non-chronological input produces a header per contiguous run" {
        val items = ledgerListItems(
            listOf(expense(1, day), expense(2, 0), expense(3, 2 * day)),
            TimeZone.UTC,
            Currency.CHF,
            emptyList(),
        )
        items.map { it.key } shouldBe listOf(
            "day-1970-01-02",
            1L,
            "day-1970-01-01",
            2L,
            "day-1970-01-03",
            3L,
        )
    }

    "nets income against expenses for the day" {
        val items = ledgerListItems(
            listOf(expense(1, 0), income(2, 1)),
            TimeZone.UTC,
            Currency.CHF,
            emptyList(),
        )

        items.filterIsInstance<DayHeaderItem>().single().totalMinor shouldBe 550
    }

    "leaves the day total unknown when a foreign currency has no rate" {
        val usdAccount = Account(2, "USD", Currency.USD)
        val usdEntry = ExpenseEntry(
            id = 1,
            category = category,
            description = null,
            account = usdAccount,
            amount = 1_000,
            createdAt = Instant.fromEpochMilliseconds(0),
            createdZone = TimeZone.UTC,
        )

        val items = ledgerListItems(
            listOf(usdEntry),
            TimeZone.UTC,
            Currency.CHF,
            emptyList(),
        )

        items.filterIsInstance<DayHeaderItem>().single().totalMinor shouldBe null
    }

    "converts a foreign day total into the default currency" {
        val usdAccount = Account(2, "USD", Currency.USD)
        val usdEntry = ExpenseEntry(
            id = 1,
            category = category,
            description = null,
            account = usdAccount,
            amount = 1_000,
            createdAt = Instant.fromEpochMilliseconds(0),
            createdZone = TimeZone.UTC,
        )

        val items = ledgerListItems(
            listOf(usdEntry),
            TimeZone.UTC,
            Currency.CHF,
            listOf(FxRate(Currency.CHF, Currency.USD, 2.0, "2026-01-01")),
        )

        items.filterIsInstance<DayHeaderItem>().single().totalMinor shouldBe -500
    }
})
