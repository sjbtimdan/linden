package org.sjbtimdan.linden.predictions

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class UpcomingRecurringTest : StringSpec({
    // 2027-01-15T08:00:00Z
    val now = Instant.fromEpochMilliseconds(1_800_000_000_000L)
    val account = Account(1, "Main", Currency.CHF)
    val category = Category(1, "Food", CategoryType.Expense)

    fun expense(id: Long, description: String?, createdAt: Instant, amount: Long = 450) =
        ExpenseEntry(id, category, description, account, amount, createdAt = createdAt, createdZone = TimeZone.UTC)

    fun upcoming(entries: List<Entry>, type: EntryType = EntryType.Expense) =
        predictUpcomingRecurring(entries, type, now, TimeZone.UTC)

    "returns empty for no entries" {
        upcoming(emptyList()).shouldBeEmpty()
    }

    "returns empty when no description recurs" {
        val entries = listOf(
            expense(1, "Coffee", now.minus(1.days)),
            expense(2, "Coffee", now.minus(2.days)),
        )
        upcoming(entries).shouldBeEmpty()
    }

    "returns a monthly bill due today" {
        val entries = listOf(
            expense(1, "Rent", now.minus(90.days)),
            expense(2, "Rent", now.minus(60.days)),
            expense(3, "Rent", now.minus(30.days)),
        )
        val result = upcoming(entries)
        result.shouldHaveSize(1)
        result.single().entry.description shouldBe "Rent"
        result.single().cadence shouldBe RecurrenceCadence.Monthly
    }

    "returns a monthly bill due within the horizon" {
        // 25 days ago is 2026-12-21; one calendar month later is 2027-01-21.
        val entries = listOf(
            expense(1, "Rent", now.minus(85.days)),
            expense(2, "Rent", now.minus(55.days)),
            expense(3, "Rent", now.minus(25.days)),
        )
        upcoming(entries).single().nextDue shouldBe now.plus(6.days)
    }

    "hides a monthly bill whose next occurrence is beyond the horizon" {
        val entries = listOf(
            expense(1, "Rent", now.minus(65.days)),
            expense(2, "Rent", now.minus(35.days)),
            expense(3, "Rent", now.minus(5.days)),
        )
        upcoming(entries).shouldBeEmpty()
    }

    "returns a monthly bill overdue within the allowance" {
        val entries = listOf(
            expense(1, "Rent", now.minus(92.days)),
            expense(2, "Rent", now.minus(62.days)),
            expense(3, "Rent", now.minus(32.days)),
        )
        upcoming(entries).shouldHaveSize(1)
    }

    "hides a monthly bill overdue beyond the allowance" {
        val entries = listOf(
            expense(1, "Rent", now.minus(95.days)),
            expense(2, "Rent", now.minus(65.days)),
            expense(3, "Rent", now.minus(35.days)),
        )
        upcoming(entries).shouldBeEmpty()
    }

    "returns a weekly bill due today" {
        val entries = listOf(
            expense(1, "Yoga", now.minus(21.days)),
            expense(2, "Yoga", now.minus(14.days)),
            expense(3, "Yoga", now.minus(7.days)),
        )
        upcoming(entries).single().cadence shouldBe RecurrenceCadence.Weekly
    }

    "hides a weekly bill whose next occurrence is a full week away" {
        val entries = listOf(
            expense(1, "Yoga", now.minus(14.days)),
            expense(2, "Yoga", now.minus(7.days)),
            expense(3, "Yoga", now),
        )
        upcoming(entries).shouldBeEmpty()
    }

    "ranks by due date, most imminent first" {
        val entries = listOf(
            expense(1, "Later", now.minus(88.days)),
            expense(2, "Later", now.minus(58.days)),
            expense(3, "Later", now.minus(28.days)),
            expense(4, "Sooner", now.minus(92.days)),
            expense(5, "Sooner", now.minus(62.days)),
            expense(6, "Sooner", now.minus(32.days)),
        )
        upcoming(entries).map { it.entry.description } shouldBe listOf("Sooner", "Later")
    }

    "caps results at the top N" {
        val entries = (1L..10L).flatMap { i ->
            listOf(
                expense(i * 3, "Bill $i", now.minus(90.days)),
                expense(i * 3 + 1, "Bill $i", now.minus(60.days)),
                expense(i * 3 + 2, "Bill $i", now.minus(30.days)),
            )
        }
        upcoming(entries).shouldHaveSize(UPCOMING_TOP_N)
    }
})
