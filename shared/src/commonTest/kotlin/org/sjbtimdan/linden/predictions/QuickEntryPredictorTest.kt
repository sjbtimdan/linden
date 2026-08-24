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

        "only considers entries of the same type" {
            val entries = listOf(
                expense(1, "Coffee", now),
                IncomeEntry(2, food, "Salary", main, 450, createdAt = now, createdZone = TimeZone.UTC),
            )
            predict(entries).shouldContainExactly(listOf(expense(1, "Coffee", now)))
        }

        "ignores entries without a description" {
            val entries = listOf(
                expense(1, null, now),
                expense(2, "", now),
                expense(3, "   ", now),
                expense(4, "Coffee", now),
            )
            predict(entries).shouldContainExactly(listOf(expense(4, "Coffee", now)))
        }

        "ranks same-hour above near-hour above far-hour" {
            val entries = listOf(
                expense(1, "Coffee", now),
                expense(2, "Train", now.minus(1.hours)),
                expense(3, "Cinema", now.minus(6.hours)),
            )
            predict(entries).map { it.description }
                .shouldContainExactly("Coffee", "Train", "Cinema")
        }

        "prefers the same weekday over a different one" {
            // B and C are both at 08:00 but a weekday off; ids order the tie.
            val entries = listOf(
                expense(1, "Coffee", now.minus(7.days)),
                expense(2, "Train", now.minus(8.days)),
                expense(3, "Cinema", now.minus(9.days)),
            )
            predict(entries).map { it.description }
                .shouldContainExactly("Coffee", "Train", "Cinema")
        }

        "does not rank by recency" {
            // The older same-weekday entry outranks the more recent one.
            val entries = listOf(
                expense(1, "Coffee", now.minus(7.days)),
                expense(2, "Train", now.minus(3.days)),
            )
            predict(entries).map { it.description }.shouldContainExactly("Coffee", "Train")
        }

        "considers entries beyond the prediction horizon" {
            // 210 days is ~7 months and a Tuesday, so the old entry still wins.
            val entries = listOf(
                expense(1, "Coffee", now.minus(210.days)),
                expense(2, "Train", now.minus(4.days)),
            )
            predict(entries).map { it.description }.shouldContainExactly("Coffee", "Train")
        }

        "prefers field matches within the same time tier" {
            val entries = listOf(
                expense(1, "Coffee", now),
                expense(2, "Train", now),
                expense(3, "Cinema", now.minus(3.hours)),
            )
            val input = FieldPredictionInput(EntryType.Expense, null, null, 450, "Coffee")
            predict(entries, input).map { it.description }
                .shouldContainExactly("Coffee", "Train", "Cinema")
        }

        "deduplicates recurring entries" {
            val entries = (1L..6L).map { expense(it, "Coffee", now) } + listOf(
                expense(7, "Train", now),
                expense(8, "Lunch", now),
                expense(9, "Dinner", now),
                expense(10, "Cinema", now),
            )
            predict(entries).map { it.description }
                .shouldContainExactly("Coffee", "Train", "Lunch", "Dinner", "Cinema")
        }

        "deduplicates by description and hour of day, not amount or account" {
            // The daily coffee in one chip, even as the price and account drift.
            val entries = listOf(
                expense(1, "Coffee", now, amount = 450),
                expense(2, "Coffee", now.minus(7.days), amount = 480),
                expense(3, "Coffee", now.minus(14.days), amount = 500, account = savings),
            )
            val result = predict(entries)
            result.shouldHaveSize(1)
            result.single().id shouldBe 1L
        }

        "keeps same-description entries at different hours" {
            val entries = listOf(
                expense(1, "Coffee", now),
                expense(2, "Coffee", now.minus(5.hours)),
            )
            predict(entries).map { it.description }.shouldContainExactly("Coffee", "Coffee")
        }

        "caps results at the top five" {
            val entries = (1L..10L).map { expense(it, "Entry $it", now) }
            predict(entries).shouldHaveSize(QUICK_ENTRY_TOP_N)
        }

        "deduplicates same-description transfers regardless of target" {
            val transfers = listOf(
                TransferEntry(
                    1, null, "Move money", main, 10_000,
                    createdAt = now, createdZone = TimeZone.UTC, toAccount = savings, toAmount = 9_500,
                ),
                TransferEntry(
                    2, null, "Move money", main, 10_000,
                    createdAt = now, createdZone = TimeZone.UTC, toAccount = credit, toAmount = 9_500,
                ),
            )
            predict(transfers, FieldPredictionInput(EntryType.Transfer, null, null, null, null))
                .shouldHaveSize(1)
        }
    }
})
