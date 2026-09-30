package org.sjbtimdan.linden.ui.ledger

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.EntryType

class LedgerEntriesEmptyStateTest : StringSpec({
    fun resolve(
        searchQuery: String = "",
        typeFilter: EntryType? = null,
        categoryFilter: Long? = null,
        accountFilter: Long? = null,
        amountFilter: AmountFilter? = null,
        periodIsAll: Boolean = true,
        showFuture: Boolean = false,
        upcomingCount: Int = 0,
        hasAnyEntries: Boolean = true,
    ) = ledgerEntriesEmptyState(
        searchQuery = searchQuery,
        typeFilter = typeFilter,
        categoryFilter = categoryFilter,
        accountFilter = accountFilter,
        amountFilter = amountFilter,
        periodIsAll = periodIsAll,
        showFuture = showFuture,
        upcomingCount = upcomingCount,
        hasAnyEntries = hasAnyEntries,
    )

    "no entries at all shows the guided state" {
        resolve(hasAnyEntries = false) shouldBe LedgerEntriesEmptyState.Guided
    }

    "hidden upcoming entries explain the show-future toggle" {
        resolve(upcomingCount = 2) shouldBe LedgerEntriesEmptyState.UpcomingHidden
    }

    "an amount filter alone reports a filter miss, not an empty ledger" {
        resolve(amountFilter = AmountFilter(AmountOperator.GreaterThan, 1_000)) shouldBe
            LedgerEntriesEmptyState.Filtered
    }

    "a category or account filter alone reports a filter miss" {
        resolve(categoryFilter = 1L) shouldBe LedgerEntriesEmptyState.Filtered
        resolve(accountFilter = 1L) shouldBe LedgerEntriesEmptyState.Filtered
    }

    "a chip filter plus a search or type narrowing reports a plain miss" {
        resolve(searchQuery = "zzz") shouldBe LedgerEntriesEmptyState.NoMatch
        resolve(categoryFilter = 1L, searchQuery = "zzz") shouldBe LedgerEntriesEmptyState.NoMatch
        resolve(categoryFilter = 1L, typeFilter = EntryType.Expense) shouldBe LedgerEntriesEmptyState.NoMatch
    }

    "a chip filter on a narrowed period reports a plain miss" {
        resolve(categoryFilter = 1L, periodIsAll = false) shouldBe LedgerEntriesEmptyState.NoMatch
    }

    "an empty window without filters says there are no entries" {
        resolve(periodIsAll = true) shouldBe LedgerEntriesEmptyState.NoEntries
        resolve(periodIsAll = false) shouldBe LedgerEntriesEmptyState.NoMatch
    }
})
