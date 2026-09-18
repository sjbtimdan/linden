package org.sjbtimdan.linden.ui.ledger

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.ui.accounts.AccountWithBalance
import kotlin.time.Instant

class AccountBalancesAtEndTest : StringSpec({
    val cutoff = LocalDate(2026, 8, 31)
    val main = Account(1, "Main", Currency.CHF)
    val savings = Account(2, "Savings", Currency.EUR)
    val groceries = Category(1, "Groceries", CategoryType.Expense)

    "balance is the initial balance plus the net of entries up to the cutoff" {
        val entries = listOf(
            IncomeEntry(0, groceries, "Pay", main, 50_000, createdAt = on("2026-08-10")),
            ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = on("2026-08-15")),
        )

        accountBalancesAtEnd(entries, cutoff, listOf(main), TimeZone.UTC) shouldBe
            listOf(AccountWithBalance(main, 49_550L))
    }

    "entries after the cutoff are excluded" {
        val entries = listOf(
            IncomeEntry(0, groceries, "Pay", main, 50_000, createdAt = on("2026-08-31")),
            ExpenseEntry(0, groceries, "After", main, 450, createdAt = on("2026-09-01")),
        )

        accountBalancesAtEnd(entries, cutoff, listOf(main), TimeZone.UTC) shouldBe
            listOf(AccountWithBalance(main, 50_000L))
    }

    "transfers move money between the source and the target" {
        val entries = listOf(
            TransferEntry(
                0,
                null,
                null,
                main,
                10_000,
                toAccount = savings,
                toAmount = null,
                createdAt = on("2026-08-20"),
            ),
        )

        accountBalancesAtEnd(entries, cutoff, listOf(main, savings), TimeZone.UTC) shouldBe
            listOf(
                AccountWithBalance(main, -10_000L),
                AccountWithBalance(savings, 10_000L),
            )
    }

    "cross-currency transfers credit the received amount to the target" {
        val entries = listOf(
            TransferEntry(
                0,
                null,
                null,
                main,
                10_000,
                toAccount = savings,
                toAmount = 9_500,
                createdAt = on("2026-08-20"),
            ),
        )

        accountBalancesAtEnd(entries, cutoff, listOf(main, savings), TimeZone.UTC) shouldBe
            listOf(
                AccountWithBalance(main, -10_000L),
                AccountWithBalance(savings, 9_500L),
            )
    }

    "accounts without entries keep their initial balance" {
        val withInitial = main.copy(initialBalance = 1_000)

        accountBalancesAtEnd(emptyList(), cutoff, listOf(withInitial), TimeZone.UTC) shouldBe
            listOf(AccountWithBalance(withInitial, 1_000L))
    }

    "every account is returned, in order, even with a zero balance" {
        val entries = listOf(
            IncomeEntry(0, groceries, "Pay", main, 50_000, createdAt = on("2026-08-10")),
        )

        accountBalancesAtEnd(entries, cutoff, listOf(main, savings), TimeZone.UTC) shouldBe
            listOf(
                AccountWithBalance(main, 50_000L),
                AccountWithBalance(savings, 0L),
            )
    }

    "entries on the cutoff day itself are included" {
        val entries = listOf(
            IncomeEntry(0, groceries, "Pay", main, 50_000, createdAt = on("2026-08-31")),
        )

        accountBalancesAtEnd(entries, cutoff, listOf(main), TimeZone.UTC) shouldBe
            listOf(AccountWithBalance(main, 50_000L))
    }

    "classifies entries by the passed zone, not their created zone" {
        val la = TimeZone.of("America/Los_Angeles")
        val entries = listOf(
            IncomeEntry(
                0,
                groceries,
                "Pay",
                main,
                50_000,
                createdAt = Instant.parse("2026-08-16T00:30:00Z"),
                createdZone = TimeZone.UTC,
            ),
        )

        // 00:30 UTC on Aug 16 is still Aug 15 in Los Angeles: the entry belongs
        // to the cutoff day and must be included in the balance.
        accountBalancesAtEnd(entries, LocalDate(2026, 8, 15), listOf(main), la) shouldBe
            listOf(AccountWithBalance(main, 50_000L))
    }
})

/** 12:00 UTC so the date is unambiguous regardless of the entry's zone. */
private fun on(date: String): Instant = Instant.parse("${date}T12:00:00Z")
