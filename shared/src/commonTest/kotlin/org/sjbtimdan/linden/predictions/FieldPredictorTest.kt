package org.sjbtimdan.linden.predictions

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class FieldPredictorTest : StringSpec({
    // 2027-01-19T08:00:00Z, a Tuesday
    val now = Instant.fromEpochMilliseconds(1_800_000_000_000L)
    val main = Account(1, "Main", Currency.CHF)
    val savings = Account(2, "Savings", Currency.CHF)
    val food = Category(1, "Food", CategoryType.Expense)
    val transport = Category(2, "Transport", CategoryType.Expense)
    val leisure = Category(3, "Leisure", CategoryType.Expense)
    val timeZone = TimeZone.UTC

    fun input(
        type: EntryType = EntryType.Expense,
        categoryId: Long? = null,
        accountId: Long? = null,
        amount: Long? = null,
        description: String? = null,
    ) = FieldPredictionInput(type, categoryId, accountId, amount, description)

    fun predictAccounts(entries: List<Entry>, predictionInput: FieldPredictionInput, topN: Int = PREDICTION_TOP_N) =
        predictAccounts(entries, predictionInput, now, timeZone, topN)

    fun predictCategories(entries: List<Entry>, predictionInput: FieldPredictionInput, topN: Int = PREDICTION_TOP_N) =
        predictCategories(entries, predictionInput, now, timeZone, topN)

    context("predictCategories") {
        "ranks by frequency when no signals are given" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now),
                expense(2, food, "Lunch", main, 450, now),
                expense(3, transport, "Train", main, 300, now),
            )
            predictCategories(entries, input()).shouldContainExactly(food.id, transport.id)
        }

        "frequency ranking favours recent entries" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now.minus(150.days)),
                expense(2, transport, "Train", main, 300, now.minus(5.days)),
            )
            predictCategories(entries, input()).shouldContainExactly(transport.id, food.id)
        }

        "frequency ranking only considers entries of the same type" {
            val income = IncomeEntry(1, food, "Salary", main, 450, createdAt = now, createdZone = TimeZone.UTC)
            val entries = listOf(
                expense(2, transport, "Train", main, 300, now),
                income,
            )
            predictCategories(entries, input(type = EntryType.Income)).shouldContainExactly(food.id)
        }

        "frequency ranking excludes entries older than six months" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now),
                expense(2, transport, "Train", main, 300, now.minus(200.days)),
            )
            predictCategories(entries, input()).shouldContainExactly(food.id)
        }

        "frequency ranking caps results at top N" {
            val entries = (1L..12L).map { id ->
                expense(id, Category(id, "Category $id", CategoryType.Expense), "Desc", main, 450, now)
            }
            predictCategories(entries, input()).shouldHaveSize(PREDICTION_TOP_N)
        }

        "treats a blank description as no signal" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now),
                expense(2, transport, "Train", main, 300, now),
            )
            predictCategories(entries, input(description = "   ")).shouldContainExactly(food.id, transport.id)
            predictAccounts(entries, input(description = "   ")).shouldContainExactly(main.id)
        }

        "predicts the category used with the given account" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now),
                expense(2, transport, "Train", savings, 300, now),
            )
            predictCategories(entries, input(accountId = main.id)).shouldContainExactly(food.id)
        }

        "matches on amount" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now),
                expense(2, transport, "Train", main, 999, now),
            )
            predictCategories(entries, input(amount = 450)).shouldContainExactly(food.id)
        }

        "matches on description alone and ranks exact above partial" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now),
                expense(2, transport, "Coffee shop", main, 450, now),
            )
            predictCategories(entries, input(description = "Coffee"))
                .shouldContainExactly(food.id, transport.id)
        }

        "only considers entries of the same type" {
            val income = IncomeEntry(1, food, "Salary", main, 450, createdAt = now, createdZone = TimeZone.UTC)
            val entries = listOf(expense(2, transport, "Train", main, 450, now))
            predictCategories(entries + income, input(type = EntryType.Income, amount = 450))
                .shouldContainExactly(food.id)
        }

        "excludes entries older than six months" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now.minus(5.days)),
                expense(2, transport, "Train", main, 450, now.minus(200.days)),
            )
            predictCategories(entries, input(amount = 450)).shouldContainExactly(food.id)
        }

        "favours recent entries" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now.minus(150.days)),
                expense(2, transport, "Train", main, 450, now.minus(5.days)),
            )
            predictCategories(entries, input(accountId = main.id))
                .shouldContainExactly(transport.id, food.id)
        }

        "ranks repeated usage above single usage" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now),
                expense(2, food, "Lunch", main, 450, now),
                expense(3, transport, "Train", main, 450, now),
            )
            predictCategories(entries, input(accountId = main.id))
                .shouldContainExactly(food.id, transport.id)
        }

        "ranks same-hour candidates above near-hour above far-hour" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now),
                expense(2, transport, "Train", main, 450, now.minus(1.hours)),
                expense(3, leisure, "Cinema", main, 450, now.minus(6.hours)),
            )
            predictCategories(entries, input(accountId = main.id))
                .shouldContainExactly(food.id, transport.id, leisure.id)
        }

        "favours candidates on the same weekday" {
            // B is one weekday off (4 days back) but more recent than C (7 days
            // back, same weekday as now), yet ranks below it.
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now),
                expense(2, transport, "Train", main, 450, now.minus(4.days)),
                expense(3, leisure, "Cinema", main, 450, now.minus(7.days)),
            )
            predictCategories(entries, input(accountId = main.id))
                .shouldContainExactly(food.id, leisure.id, transport.id)
        }

        "favours candidates in the same month" {
            // B shares the weekday and hour but falls in the previous month.
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now),
                expense(2, transport, "Train", main, 450, now.minus(28.days)),
                expense(3, leisure, "Cinema", main, 450, now.minus(7.days)),
            )
            predictCategories(entries, input(accountId = main.id))
                .shouldContainExactly(food.id, leisure.id, transport.id)
        }

        "caps results at the top N" {
            val entries = (1L..12L).map { id ->
                expense(id, Category(id, "Category $id", CategoryType.Expense), "Description", main, 450, now)
            }
            predictCategories(entries, input(accountId = main.id)).shouldHaveSize(PREDICTION_TOP_N)
        }
    }

    context("predictAccounts") {
        "ranks by frequency when no signals are given" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now),
                expense(2, food, "Lunch", main, 450, now),
                expense(3, food, "Dinner", savings, 450, now),
            )
            predictAccounts(entries, input()).shouldContainExactly(main.id, savings.id)
        }

        "frequency ranking favours recent entries" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now.minus(150.days)),
                expense(2, food, "Lunch", savings, 450, now.minus(5.days)),
            )
            predictAccounts(entries, input()).shouldContainExactly(savings.id, main.id)
        }

        "frequency ranking only considers entries of the same type" {
            val income = IncomeEntry(1, food, "Salary", savings, 450, createdAt = now, createdZone = TimeZone.UTC)
            val entries = listOf(
                expense(2, food, "Coffee", main, 450, now),
                income,
            )
            predictAccounts(entries, input(type = EntryType.Income)).shouldContainExactly(savings.id)
        }

        "frequency ranking excludes entries older than six months" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now),
                expense(2, food, "Lunch", savings, 450, now.minus(200.days)),
            )
            predictAccounts(entries, input()).shouldContainExactly(main.id)
        }

        "frequency ranking caps results at top N" {
            val entries = (1L..12L).map { id ->
                expense(id, food, "Desc", Account(id, "Account $id", Currency.CHF), 450, now)
            }
            predictAccounts(entries, input()).shouldHaveSize(PREDICTION_TOP_N)
        }

        "predicts the account used with the given category and amount" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now),
                expense(2, food, "Lunch", savings, 999, now),
            )
            predictAccounts(entries, input(categoryId = food.id, amount = 450))
                .shouldContainExactly(main.id, savings.id)
        }

        "matches on description alone" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now),
                expense(2, food, "Tea", savings, 450, now),
            )
            predictAccounts(entries, input(description = "Coffee")).shouldContainExactly(main.id)
        }

        "only considers entries of the same type" {
            val income = IncomeEntry(1, food, "Salary", savings, 450, createdAt = now, createdZone = TimeZone.UTC)
            val entries = listOf(expense(2, food, "Coffee", main, 450, now))
            predictAccounts(entries + income, input(type = EntryType.Income, amount = 450))
                .shouldContainExactly(savings.id)
        }

        "favours recent entries" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now.minus(150.days)),
                expense(2, food, "Lunch", savings, 450, now.minus(5.days)),
            )
            predictAccounts(entries, input(categoryId = food.id))
                .shouldContainExactly(savings.id, main.id)
        }

        "ranks repeated usage above single usage" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now),
                expense(2, food, "Lunch", main, 450, now),
                expense(3, food, "Dinner", savings, 450, now),
            )
            predictAccounts(entries, input(categoryId = food.id))
                .shouldContainExactly(main.id, savings.id)
        }

        "ranks same-hour candidates higher" {
            val entries = listOf(
                expense(1, food, "Coffee", main, 450, now),
                expense(2, food, "Lunch", savings, 450, now.minus(6.hours)),
            )
            predictAccounts(entries, input(categoryId = food.id))
                .shouldContainExactly(main.id, savings.id)
        }

        "caps results at the top N" {
            val entries = (1L..12L).map { id ->
                expense(id, food, "Description", Account(id, "Account $id", Currency.CHF), 450, now)
            }
            predictAccounts(entries, input(categoryId = food.id)).shouldHaveSize(PREDICTION_TOP_N)
        }
    }
})

private fun expense(
    id: Long,
    category: Category,
    description: String,
    account: Account,
    amount: Long,
    createdAt: Instant,
) = ExpenseEntry(id, category, description, account, amount, createdAt = createdAt, createdZone = TimeZone.UTC)
