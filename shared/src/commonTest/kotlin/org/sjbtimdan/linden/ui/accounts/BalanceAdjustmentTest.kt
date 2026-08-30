package org.sjbtimdan.linden.ui.accounts

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import kotlin.time.Instant

class BalanceAdjustmentTest : StringSpec({
    val account = Account(id = 1, name = "Main", currency = Currency.CHF, initialBalance = 10_000)
    val category = Category(
        id = 1,
        name = "Groceries",
        type = CategoryType.Expense,
        icon = CategoryIcon.ShoppingCart,
    )
    val now = Instant.fromEpochMilliseconds(1_700_000_000_000)
    val zone = TimeZone.UTC

    "delta is target minus current" {
        balanceAdjustment(currentBalance = 10_000, targetBalance = 12_500).delta shouldBe 2_500
        balanceAdjustment(currentBalance = 10_000, targetBalance = 9_000).delta shouldBe -1_000
        balanceAdjustment(currentBalance = 10_000, targetBalance = 10_000).delta shouldBe 0
    }

    "a positive delta produces an income entry of the absolute amount" {
        val entry = adjustmentEntry(balanceAdjustment(10_000, 12_500), account, category, now, zone)
        entry shouldBe IncomeEntry(
            id = 0,
            category = category,
            description = null,
            account = account,
            amount = 2_500,
            createdAt = now,
            createdZone = zone,
        )
    }

    "a negative delta produces an expense entry of the absolute amount" {
        val entry = adjustmentEntry(balanceAdjustment(10_000, 9_000), account, category, now, zone)
        entry shouldBe ExpenseEntry(
            id = 0,
            category = category,
            description = null,
            account = account,
            amount = 1_000,
            createdAt = now,
            createdZone = zone,
        )
    }

    "a zero delta produces no entry" {
        adjustmentEntry(balanceAdjustment(10_000, 10_000), account, category, now, zone).shouldBeNull()
    }
})
