package org.sjbtimdan.linden.capture

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.time.TEST_NOW

private fun account(id: Long = 1L) = Account(id, "Main", Currency.CHF)

private fun category(id: Long = 1L, type: CategoryType = CategoryType.Expense) = Category(id, "Groceries", type)

private fun capture(description: String, packageName: String = "com.example.bank") = PendingCapture(
    packageName = packageName,
    amountMinor = 1250,
    currency = Currency.CHF,
    description = description,
    type = EntryType.Expense,
    capturedAtMillis = 1L,
)

class CaptureTemplateTest : StringSpec({

    "skeleton masks every digit run and lowercases the wording" {
        captureSkeleton("You paid CHF 12.50 at COOP 4070") shouldBe "you paid chf #.# at coop #"
    }

    "skeleton collapses whitespace runs and trims" {
        captureSkeleton("  Paid\n CHF\t\t12  ") shouldBe "paid chf #"
    }

    "skeleton masks non-ASCII digits and digits glued to words" {
        captureSkeleton("دفع ١٢٣.٤٥ ليرة") shouldBe "دفع #.# ليرة"
        captureSkeleton("CHF12.50") shouldBe "chf#.#"
    }

    "skeleton is capped at the description limit" {
        captureSkeleton("a".repeat(200)).length shouldBe 140
    }

    "match requires the same package and the same skeleton" {
        val template = CaptureTemplate(
            packageName = "com.example.bank",
            skeleton = captureSkeleton("You paid CHF 12.50 at COOP"),
            type = EntryType.Expense,
            categoryId = 1L,
            accountId = 2L,
        )

        matchCaptureTemplate(capture("You paid CHF 8.00 at COOP"), listOf(template)) shouldBe template
        matchCaptureTemplate(capture("You paid CHF 8.00 at COOP", packageName = "other.app"), listOf(template))
            .shouldBeNull()
        matchCaptureTemplate(capture("You paid CHF 8.00 at TESCO"), listOf(template)).shouldBeNull()
    }

    "match ignores a skeleton that carries too little wording" {
        val template = CaptureTemplate("com.example.bank", "# #", EntryType.Expense, 1L, 2L)

        captureSkeleton("12 34") shouldBe "# #"
        matchCaptureTemplate(capture("12 34"), listOf(template)).shouldBeNull()
    }

    "learn stores the skeleton, the user's fields and their wording" {
        val learned = captureTemplateFor(
            capture("You paid CHF 12.50 at COOP"),
            ExpenseEntry(0, category(7L), " COOP ", account(9L), 1250, createdAt = TEST_NOW),
        )

        learned shouldBe CaptureTemplate(
            packageName = "com.example.bank",
            skeleton = "you paid chf #.# at coop",
            type = EntryType.Expense,
            categoryId = 7L,
            accountId = 9L,
            description = "COOP",
        )
    }

    "learn stores an income classification" {
        val learned = captureTemplateFor(
            capture("Salary October"),
            IncomeEntry(0, category(3L, CategoryType.Income), null, account(4L), 500_000, createdAt = TEST_NOW),
        )

        learned?.type shouldBe EntryType.Income
        learned?.categoryId shouldBe 3L
        learned?.description.shouldBeNull()
    }

    "learn never remembers a transfer" {
        val transfer = TransferEntry(
            id = 0,
            category = null,
            description = "Moved",
            account = account(1L),
            amount = 1000,
            createdAt = TEST_NOW,
            toAccount = account(2L),
            toAmount = null,
        )

        captureTemplateFor(capture("Transfer out"), transfer).shouldBeNull()
    }

    "learn ignores a description that carries too little wording" {
        captureTemplateFor(capture("12"), ExpenseEntry(0, category(), "COOP", account(), 1250, createdAt = TEST_NOW))
            .shouldBeNull()
    }
})
