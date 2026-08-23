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
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class DescriptionPredictorTest : StringSpec({
    val now = Instant.fromEpochMilliseconds(1_800_000_000_000L)
    val main = Account(1, "Main", Currency.CHF)
    val savings = Account(2, "Savings", Currency.CHF)
    val food = Category(1, "Food", CategoryType.Expense)
    val timeZone = TimeZone.UTC

    fun input(
        type: EntryType = EntryType.Expense,
        categoryId: Long? = null,
        accountId: Long? = null,
        amount: Long? = null,
    ) = DescriptionPredictionInput(type, categoryId, accountId, amount)

    fun predict(entries: List<Entry>, predictionInput: DescriptionPredictionInput, topN: Int = PREDICTION_TOP_N) =
        predictDescriptions(entries, predictionInput, now, timeZone, topN)

    "returns empty when no inputs are given" {
        predict(listOf(expense(1, food, "Coffee", main, 450, now)), input()).shouldBeEmpty()
    }

    "returns empty when no candidate matches any input" {
        val entries = listOf(expense(1, food, "Coffee", main, 450, now))
        predict(entries, input(accountId = savings.id, amount = 999)).shouldBeEmpty()
    }

    "matches on category alone when account and amount are missing" {
        val entries = listOf(
            expense(1, food, "Coffee", main, 450, now),
            expense(2, food, "Bread", main, 300, now),
        )
        predict(entries, input(categoryId = food.id))
            .shouldContainExactly("Bread", "Coffee")
    }

    "matches on account alone when category and amount are missing" {
        val entries = listOf(
            expense(1, food, "Coffee", main, 450, now),
            expense(2, food, "Bread", savings, 300, now),
        )
        predict(entries, input(accountId = main.id))
            .shouldContainExactly("Coffee")
    }

    "matches on amount alone when category and account are missing" {
        val entries = listOf(
            expense(1, food, "Coffee", main, 450, now),
            expense(2, food, "Bread", main, 300, now),
        )
        predict(entries, input(amount = 450))
            .shouldContainExactly("Coffee")
    }

    "only considers entries of the same type" {
        val income = IncomeEntry(1, food, "Salary", main, 450, createdAt = now, createdZone = TimeZone.UTC)
        val entries = listOf(expense(2, food, "Coffee", main, 450, now))
        predict(entries, input(amount = 450)).shouldContainExactly("Coffee")
        predict(entries + income, input(type = EntryType.Income, amount = 450))
            .shouldContainExactly("Salary")
    }

    "excludes entries older than six months" {
        val entries = listOf(
            expense(1, food, "Recent", main, 450, now.minus(5.days)),
            expense(2, food, "Ancient", main, 450, now.minus(200.days)),
        )
        predict(entries, input(categoryId = food.id, accountId = main.id, amount = 450))
            .shouldContainExactly("Recent")
    }

    "excludes blank descriptions" {
        val entries = listOf(
            expense(1, food, "Coffee", main, 450, now),
            expense(2, food, "   ", main, 450, now),
        )
        predict(entries, input(categoryId = food.id, accountId = main.id, amount = 450))
            .shouldContainExactly("Coffee")
    }

    "favours recent entries" {
        val entries = listOf(
            expense(1, food, "Tea", main, 450, now.minus(150.days)),
            expense(2, food, "Coffee", main, 450, now.minus(5.days)),
        )
        predict(entries, input(categoryId = food.id, accountId = main.id, amount = 450))
            .shouldContainExactly("Coffee", "Tea")
    }

    "ranks exact amount matches above near matches above no amount match" {
        val entries = listOf(
            expense(1, food, "Far", main, 600, now),
            expense(2, food, "Near", main, 480, now),
            expense(3, food, "Exact", main, 450, now),
        )
        predict(entries, input(categoryId = food.id, accountId = main.id, amount = 450))
            .shouldContainExactly("Exact", "Near", "Far")
    }

    "scores a near amount within tolerance" {
        val entries = listOf(
            expense(1, food, "Coffee", main, 480, now),
            expense(2, food, "Tea", main, 550, now),
        )
        // 480 is within 10% of 450 and ranks first; 550 is not
        predict(entries, input(categoryId = food.id, accountId = main.id, amount = 450))
            .shouldContainExactly("Coffee", "Tea")
    }

    "groups descriptions case-insensitively and sums their scores" {
        val entries = listOf(
            expense(1, food, "Coffee", main, 450, now),
            expense(2, food, "coffee", main, 450, now),
            expense(3, food, "Tea", main, 450, now),
        )
        val result = predict(entries, input(categoryId = food.id, accountId = main.id, amount = 450))
        result shouldHaveSize 2
        result.first().lowercase() shouldBe "coffee"
    }

    "keeps the original casing of the highest-scoring occurrence" {
        val entries = listOf(
            expense(1, food, "coffee", main, 451, now),
            expense(2, food, "Coffee", main, 450, now),
        )
        predict(entries, input(categoryId = food.id, accountId = main.id, amount = 450))
            .shouldContainExactly("Coffee")
    }

    "caps results at the top N" {
        val entries = (1L..12L).map { id ->
            expense(id, food, "Description $id", main, 450, now)
        }
        predict(entries, input(categoryId = food.id, accountId = main.id, amount = 450))
            .shouldHaveSize(PREDICTION_TOP_N)
    }

    "repeated descriptions rank above single occurrences" {
        val entries = listOf(
            expense(1, food, "Coffee", main, 450, now),
            expense(2, food, "Coffee", main, 450, now),
            expense(3, food, "Tea", main, 450, now),
        )
        predict(entries, input(categoryId = food.id, accountId = main.id, amount = 450))
            .shouldContainExactly("Coffee", "Tea")
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
