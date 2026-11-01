package org.sjbtimdan.linden.ui.ledger

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.ExpenseEntry

class LedgerWindowTest : StringSpec({
    "hideFutureBound cuts a window spanning today at today" {
        hideFutureBound(
            start = LocalDate(2026, 8, 1),
            end = LocalDate(2026, 8, 31),
            today = LocalDate(2026, 8, 15),
        ) shouldBe LocalDate(2026, 8, 15)
    }

    "hideFutureBound keeps the end of a wholly past window" {
        hideFutureBound(
            start = LocalDate(2026, 7, 1),
            end = LocalDate(2026, 7, 31),
            today = LocalDate(2026, 8, 15),
        ) shouldBe LocalDate(2026, 7, 31)
    }

    "hideFutureBound keeps the end of a wholly future window" {
        hideFutureBound(
            start = LocalDate(2026, 9, 1),
            end = LocalDate(2026, 9, 30),
            today = LocalDate(2026, 8, 15),
        ) shouldBe LocalDate(2026, 9, 30)
    }

    "hideFutureBound bounds an unbounded window at today" {
        hideFutureBound(start = null, end = null, today = LocalDate(2026, 8, 15)) shouldBe LocalDate(2026, 8, 15)
    }

    "isInWindow accepts a row inside a wholly past window" {
        entryOn(LocalDate(2026, 7, 10)).isInWindow(
            start = LocalDate(2026, 7, 1),
            end = LocalDate(2026, 7, 31),
            today = LocalDate(2026, 8, 15),
            showFuture = false,
            zone = TimeZone.UTC,
        ) shouldBe true
    }

    "isInWindow rejects a row before the window start" {
        entryOn(LocalDate(2026, 6, 30)).isInWindow(
            start = LocalDate(2026, 7, 1),
            end = LocalDate(2026, 7, 31),
            today = LocalDate(2026, 8, 15),
            showFuture = false,
            zone = TimeZone.UTC,
        ) shouldBe false
    }

    "isInWindow accepts a future row in a wholly future window" {
        entryOn(LocalDate(2026, 9, 15)).isInWindow(
            start = LocalDate(2026, 9, 1),
            end = LocalDate(2026, 9, 30),
            today = LocalDate(2026, 8, 15),
            showFuture = false,
            zone = TimeZone.UTC,
        ) shouldBe true
    }

    "isInWindow hides a future row while the window spans today" {
        entryOn(LocalDate(2026, 8, 16)).isInWindow(
            start = LocalDate(2026, 8, 1),
            end = LocalDate(2026, 8, 31),
            today = LocalDate(2026, 8, 15),
            showFuture = false,
            zone = TimeZone.UTC,
        ) shouldBe false
    }

    "isInWindow includes a future row when showFuture is on" {
        entryOn(LocalDate(2026, 8, 16)).isInWindow(
            start = LocalDate(2026, 8, 1),
            end = LocalDate(2026, 8, 31),
            today = LocalDate(2026, 8, 15),
            showFuture = true,
            zone = TimeZone.UTC,
        ) shouldBe true
    }

    "isInWindow treats All as unbounded but still hides the future unless shown" {
        val future = entryOn(LocalDate(2026, 8, 16))
        future.isInWindow(
            start = null,
            end = null,
            today = LocalDate(2026, 8, 15),
            showFuture = false,
            zone = TimeZone.UTC,
        ) shouldBe false
        future.isInWindow(
            start = null,
            end = null,
            today = LocalDate(2026, 8, 15),
            showFuture = true,
            zone = TimeZone.UTC,
        ) shouldBe true
    }

    "sqlLowerBound backs up one day from UTC midnight" {
        val date = LocalDate(2026, 1, 10)
        val midnight = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        date.sqlLowerBound() shouldBe midnight - 86_400_000L
    }

    "sqlUpperBound moves two days past UTC midnight" {
        val date = LocalDate(2026, 1, 10)
        val midnight = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        date.sqlUpperBound() shouldBe midnight + 2 * 86_400_000L
    }
})

private fun entryOn(date: LocalDate, zone: TimeZone = TimeZone.UTC): Entry = ExpenseEntry(
    id = 0,
    category = Category(1, "Groceries", CategoryType.Expense),
    description = null,
    account = Account(1, "Main", Currency.CHF),
    amount = 100,
    createdAt = date.atStartOfDayIn(zone),
    createdZone = zone,
)
