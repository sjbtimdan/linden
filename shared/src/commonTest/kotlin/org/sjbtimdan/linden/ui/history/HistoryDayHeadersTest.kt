package org.sjbtimdan.linden.ui.history

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry

class HistoryDayHeadersTest : StringSpec({
    val account = Account(1, "Main", Currency.CHF)
    val category = Category(1, "Groceries", CategoryType.Expense)

    fun expense(id: Long, epochMillis: Long, zone: TimeZone = TimeZone.UTC): ExpenseEntry =
        ExpenseEntry(
            id = id,
            category = category,
            description = "Coffee",
            account = account,
            amount = 450,
            createdAt = Instant.fromEpochMilliseconds(epochMillis),
            createdZone = zone,
        )

    val day = 86_400_000L

    "empty list produces no items" {
        historyListItems(emptyList(), showDayHeaders = true) shouldBe emptyList()
    }

    "entries on a single day produce one header" {
        val items = historyListItems(
            listOf(expense(1, 0), expense(2, day - 1)),
            showDayHeaders = true,
        )
        items.map { it.key } shouldBe listOf("day-1970-01-01", 1L, 2L)
        items.first().shouldBeInstanceOf<DayHeaderItem>().label shouldBe "1 Jan 1970"
    }

    "entries on multiple days produce a header per day" {
        val items = historyListItems(
            listOf(
                expense(1, 0),
                expense(2, day - 1),
                expense(3, day),
                expense(4, 2 * day - 1),
                expense(5, 2 * day),
            ),
            showDayHeaders = true,
        )
        items.map { it.key } shouldBe listOf(
            "day-1970-01-01", 1L, 2L,
            "day-1970-01-02", 3L, 4L,
            "day-1970-01-03", 5L,
        )
    }

    "different zones landing on the same local day produce one header" {
        val la = TimeZone.of("America/Los_Angeles")
        val items = historyListItems(
            listOf(
                expense(1, 0, TimeZone.UTC),
                expense(2, 8 * 3_600_000, la),
                expense(3, 32 * 3_600_000, la),
            ),
            showDayHeaders = true,
        )
        items.map { it.key } shouldBe listOf("day-1970-01-01", 1L, 2L, "day-1970-01-02", 3L)
    }

    "non-chronological input produces a header per contiguous run" {
        val items = historyListItems(
            listOf(expense(1, day), expense(2, 0), expense(3, 2 * day)),
            showDayHeaders = true,
        )
        items.map { it.key } shouldBe listOf(
            "day-1970-01-02", 1L,
            "day-1970-01-01", 2L,
            "day-1970-01-03", 3L,
        )
    }

    "no headers when showDayHeaders is false" {
        val items = historyListItems(
            listOf(expense(1, 0), expense(2, day)),
            showDayHeaders = false,
        )
        items.map { it.key } shouldBe listOf(1L, 2L)
    }
})
