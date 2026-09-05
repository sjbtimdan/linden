package org.sjbtimdan.linden.predictions

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.ExpenseEntry
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class RecurringDetectorTest : StringSpec({
    // 2027-01-15T08:00:00Z, a Friday
    val now = Instant.fromEpochMilliseconds(1_800_000_000_000L)
    val main = Account(1, "Main", Currency.CHF)
    val food = Category(1, "Food", CategoryType.Expense)

    fun expense(id: Long, description: String?, createdAt: Instant) =
        ExpenseEntry(id, food, description, main, 450, createdAt = createdAt, createdZone = TimeZone.UTC)

    fun detect(entries: List<Entry>, description: String) = recurringCadence(entries, description)

    context("recurringCadence") {
        "returns null for fewer than three occurrences" {
            val entries = listOf(
                expense(1, "Rent", now),
                expense(2, "Rent", now.minus(30.days)),
            )
            detect(entries, "Rent") shouldBe null
        }

        "detects a monthly cadence" {
            val entries = listOf(
                expense(1, "Rent", now),
                expense(2, "Rent", now.minus(30.days)),
                expense(3, "Rent", now.minus(60.days)),
            )
            detect(entries, "Rent") shouldBe RecurrenceCadence.Monthly
        }

        "detects a weekly cadence" {
            val entries = listOf(
                expense(1, "Yoga", now),
                expense(2, "Yoga", now.minus(7.days)),
                expense(3, "Yoga", now.minus(14.days)),
            )
            detect(entries, "Yoga") shouldBe RecurrenceCadence.Weekly
        }

        "tolerates small drift in the interval" {
            val entries = listOf(
                expense(1, "Rent", now),
                expense(2, "Rent", now.minus(31.days)),
                expense(3, "Rent", now.minus(59.days)),
            )
            detect(entries, "Rent") shouldBe RecurrenceCadence.Monthly
        }

        "returns null for irregular intervals" {
            val entries = listOf(
                expense(1, "Coffee", now),
                expense(2, "Coffee", now.minus(3.days)),
                expense(3, "Coffee", now.minus(40.days)),
            )
            detect(entries, "Coffee") shouldBe null
        }

        "matches descriptions case-insensitively" {
            val entries = listOf(
                expense(1, "Rent", now),
                expense(2, "rent", now.minus(30.days)),
                expense(3, "RENT", now.minus(60.days)),
            )
            detect(entries, "Rent") shouldBe RecurrenceCadence.Monthly
        }

        "ignores entries with other descriptions" {
            val entries = listOf(
                expense(1, "Rent", now),
                expense(2, "Rent", now.minus(30.days)),
                expense(3, "Rent", now.minus(60.days)),
                expense(4, "Coffee", now),
                expense(5, "Coffee", now.minus(7.days)),
                expense(6, "Coffee", now.minus(14.days)),
            )
            detect(entries, "Rent") shouldBe RecurrenceCadence.Monthly
        }

        "returns null when the description has no matching entries" {
            detect(listOf(expense(1, "Rent", now)), "Coffee") shouldBe null
        }
    }
})
