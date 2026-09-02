package org.sjbtimdan.linden.predictions

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
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
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class QuickEntryPredictorTest : StringSpec({
    // 2027-01-19T08:00:00Z, a Tuesday
    val now = Instant.fromEpochMilliseconds(1_800_000_000_000L)
    val main = Account(1, "Main", Currency.CHF)
    val savings = Account(2, "Savings", Currency.CHF)
    val credit = Account(3, "Credit", Currency.CHF)
    val food = Category(1, "Food", CategoryType.Expense)
    val transport = Category(2, "Transport", CategoryType.Expense)
    val leisure = Category(3, "Leisure", CategoryType.Expense)
    val timeZone = TimeZone.UTC

    fun predict(
        entries: List<Entry>,
        input: FieldPredictionInput = FieldPredictionInput(EntryType.Expense, null, null, null, null),
    ) = predictQuickEntries(entries, input, now, timeZone, QUICK_ENTRY_TOP_N)

    fun expense(
        id: Long,
        description: String?,
        createdAt: Instant,
        category: Category = food,
        account: Account = main,
        amount: Long = 450,
    ) = ExpenseEntry(id, category, description, account, amount, createdAt = createdAt, createdZone = TimeZone.UTC)

    context("predictQuickEntries") {
        "returns empty for no entries" {
            predict(emptyList()).shouldBeEmpty()
        }

        "returns empty when every description appears only once" {
            val entries = listOf(
                expense(1, "Coffee", now),
                expense(2, "Train", now),
            )
            predict(entries).shouldBeEmpty()
        }

        "only considers entries of the same type" {
            val entries = listOf(
                expense(1, "Coffee", now.minus(1.days)),
                expense(2, "Coffee", now.minus(1.days)),
                IncomeEntry(3, food, "Salary", main, 450, createdAt = now.minus(1.days), createdZone = TimeZone.UTC),
            )
            predict(entries).map { it.entry.description }.shouldContainExactly("Coffee")
        }

        "ignores entries without a description" {
            val entries = listOf(
                expense(1, null, now.minus(1.days)),
                expense(2, "", now.minus(1.days)),
                expense(3, "   ", now.minus(1.days)),
                expense(4, "Coffee", now.minus(1.days)),
                expense(5, "Coffee", now.minus(1.days)),
            )
            predict(entries).map { it.entry.description }.shouldContainExactly("Coffee")
        }

        "ranks same-hour above near-hour above far-hour" {
            val entries = listOf(
                expense(1, "Coffee", now.minus(1.days)),
                expense(2, "Coffee", now.minus(1.days)),
                expense(3, "Train", now.minus(1.days).minus(1.hours)),
                expense(4, "Train", now.minus(1.days).minus(1.hours)),
                expense(5, "Cinema", now.minus(1.days).minus(6.hours)),
                expense(6, "Cinema", now.minus(1.days).minus(6.hours)),
            )
            predict(entries).map { it.entry.description }
                .shouldContainExactly("Coffee", "Train", "Cinema")
        }

        "prefers the same weekday over a different one" {
            val entries = listOf(
                expense(1, "Coffee", now.minus(7.days)),
                expense(2, "Coffee", now.minus(7.days)),
                expense(3, "Train", now.minus(8.days)),
                expense(4, "Train", now.minus(8.days)),
                expense(5, "Cinema", now.minus(9.days)),
                expense(6, "Cinema", now.minus(9.days)),
            )
            predict(entries).map { it.entry.description }
                .shouldContainExactly("Coffee", "Train", "Cinema")
        }

        "ranks recent entries above old ones" {
            val entries = listOf(
                expense(1, "Coffee", now.minus(60.days)),
                expense(2, "Coffee", now.minus(60.days)),
                expense(3, "Train", now.minus(3.days)),
                expense(4, "Train", now.minus(3.days)),
            )
            predict(entries).map { it.entry.description }.shouldContainExactly("Train", "Coffee")
        }

        "considers entries beyond the prediction horizon but recency still matters" {
            val entries = listOf(
                expense(1, "Coffee", now.minus(210.days)),
                expense(2, "Coffee", now.minus(210.days)),
                expense(3, "Train", now.minus(4.days)),
                expense(4, "Train", now.minus(4.days)),
            )
            predict(entries).map { it.entry.description }.shouldContainExactly("Train", "Coffee")
        }

        "prefers field matches within the same time tier" {
            val entries = listOf(
                expense(1, "Coffee", now.minus(1.days)),
                expense(2, "Coffee", now.minus(1.days)),
                expense(3, "Train", now.minus(1.days)),
                expense(4, "Train", now.minus(1.days)),
                expense(5, "Cinema", now.minus(1.days).minus(3.hours)),
                expense(6, "Cinema", now.minus(1.days).minus(3.hours)),
            )
            val input = FieldPredictionInput(EntryType.Expense, null, null, 450, "Coffee")
            predict(entries, input).map { it.entry.description }
                .shouldContainExactly("Coffee", "Train", "Cinema")
        }

        "frequent entries rank above rare ones" {
            val entries = (1L..5L).map { expense(it, "Coffee", now.minus(1.days)) } +
                (6L..7L).map { expense(it, "Generali", now.minus(1.days)) }
            predict(entries).map { it.entry.description }
                .shouldContainExactly("Coffee", "Generali")
        }

        "deduplicates recurring entries" {
            val entries = (1L..6L).map { expense(it, "Coffee", now.minus(1.days)) } + listOf(
                expense(7, "Train", now.minus(1.days)),
                expense(8, "Train", now.minus(1.days)),
                expense(9, "Lunch", now.minus(1.days)),
                expense(10, "Lunch", now.minus(1.days)),
                expense(11, "Dinner", now.minus(1.days)),
                expense(12, "Dinner", now.minus(1.days)),
                expense(13, "Cinema", now.minus(1.days)),
                expense(14, "Cinema", now.minus(1.days)),
            )
            predict(entries).map { it.entry.description }
                .shouldContainExactly("Coffee", "Train", "Lunch", "Dinner", "Cinema")
        }

        "deduplicates by description and hour of day, not amount or account" {
            val entries = listOf(
                expense(1, "Coffee", now.minus(1.days), amount = 450),
                expense(2, "Coffee", now.minus(1.days).minus(7.days), amount = 480),
                expense(3, "Coffee", now.minus(1.days).minus(14.days), amount = 500, account = savings),
            )
            val result = predict(entries)
            result.shouldHaveSize(1)
            result.single().entry.id shouldBe 1L
        }

        "keeps same-description entries at different hours" {
            val entries = listOf(
                expense(1, "Coffee", now.minus(1.days)),
                expense(2, "Coffee", now.minus(1.days)),
                expense(3, "Coffee", now.minus(1.days).minus(5.hours)),
                expense(4, "Coffee", now.minus(1.days).minus(5.hours)),
            )
            predict(entries).map { it.entry.description }.shouldContainExactly("Coffee", "Coffee")
        }

        "caps results at the top five" {
            val entries = (1L..10L).flatMap { i ->
                listOf(
                    expense(i, "Entry $i", now.minus(1.days)),
                    expense(i + 10, "Entry $i", now.minus(1.days)),
                )
            }
            predict(entries).shouldHaveSize(QUICK_ENTRY_TOP_N)
        }

        "deduplicates same-description transfers regardless of target" {
            val transfers = listOf(
                TransferEntry(
                    1, null, "Move money", main, 10_000,
                    createdAt = now.minus(1.days), createdZone = TimeZone.UTC, toAccount = savings, toAmount = 9_500,
                ),
                TransferEntry(
                    2, null, "Move money", main, 10_000,
                    createdAt = now.minus(1.days), createdZone = TimeZone.UTC, toAccount = credit, toAmount = 9_500,
                ),
            )
            predict(transfers, FieldPredictionInput(EntryType.Transfer, null, null, null, null))
                .shouldHaveSize(1)
        }

        "boosts a monthly bill near its day of month even when weekday and month drift" {
            // now = 2027-02-02T08:00:00Z (Tuesday, day 2)
            val billNow = Instant.fromEpochMilliseconds(1_800_000_000_000L + 18.days.inWholeMilliseconds)
            // Service charge recurs on the 2nd; the most recent occurrence is in a
            // different month and weekday, so only the day-of-month matches now.
            val entries = listOf(
                expense(1, "Service Charge", billNow.minus(31.days)), // 2027-01-02, Saturday
                expense(2, "Service Charge", billNow.minus(62.days)),
                expense(3, "Service Charge", billNow.minus(93.days)),
                expense(4, "Coffee", billNow.minus(17.days)), // 2027-01-16, Saturday
                expense(5, "Coffee", billNow.minus(17.days)),
                expense(6, "Coffee", billNow.minus(17.days)),
            )
            predictQuickEntries(
                entries,
                FieldPredictionInput(EntryType.Expense, null, null, null, null),
                billNow,
                timeZone,
                QUICK_ENTRY_TOP_N,
            ).map { it.entry.description }
                .shouldContainExactly("Service Charge", "Coffee")
        }

        "excludes a recurring description entered today" {
            // Service Charge recurs monthly, but the user just entered it today.
            val entries = listOf(
                expense(1, "Service Charge", now), // entered today
                expense(2, "Service Charge", now.minus(30.days)),
                expense(3, "Service Charge", now.minus(60.days)),
                expense(4, "Coffee", now.minus(1.days)),
                expense(5, "Coffee", now.minus(2.days)),
            )
            predict(entries).map { it.entry.description }
                .shouldContainExactly("Coffee")
        }

        "excludes any description entered today, not just recurring ones" {
            val entries = listOf(
                expense(1, "Coffee", now), // entered today
                expense(2, "Coffee", now.minus(1.days)),
                expense(3, "Train", now.minus(1.days)),
                expense(4, "Train", now.minus(2.days)),
            )
            predict(entries).map { it.entry.description }
                .shouldContainExactly("Train")
        }

        "still suggests a description entered on a previous day" {
            val entries = listOf(
                expense(1, "Coffee", now.minus(1.days)),
                expense(2, "Coffee", now.minus(2.days)),
            )
            predict(entries).map { it.entry.description }
                .shouldContainExactly("Coffee")
        }
    }
})
