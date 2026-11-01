package org.sjbtimdan.linden.ui.ledger

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.datetime.LocalDate
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.onTestMain

class LedgerFilterStateTest : StringSpec({
    "starts on the current month with no filters" {
        onTestMain {
            val scope = testScope()
            val state = LedgerFilterState(scope, LocalDate(2026, 1, 15))

            state.periodSelection.value shouldBe PeriodSelection(LedgerPeriod.Month, LocalDate(2026, 1, 15))
            state.viewMode.value shouldBe LedgerViewMode.Entries
            state.searchQuery.value shouldBe ""
            state.typeFilter.value shouldBe null
            state.amountFilter.value shouldBe null
            state.categoryFilter.value shouldBe null
            state.accountFilter.value shouldBe null
            scope.cancel()
        }
    }

    "periodWindow follows the selected period" {
        onTestMain {
            val scope = testScope()
            val state = LedgerFilterState(scope, LocalDate(2026, 1, 15))

            state.periodWindow.value shouldBe PeriodWindow(LocalDate(2026, 1, 1), LocalDate(2026, 1, 31))

            state.setPeriod(LedgerPeriod.Year)

            state.periodWindow.value shouldBe PeriodWindow(LocalDate(2026, 1, 1), LocalDate(2026, 12, 31))
            scope.cancel()
        }
    }

    "leaving the entries view clears category and account filters" {
        onTestMain {
            val scope = testScope()
            val state = LedgerFilterState(scope, LocalDate(2026, 1, 15))
            state.setCategoryFilter(5)
            state.setAccountFilter(7)

            state.setViewMode(LedgerViewMode.Accounts)

            state.viewMode.value shouldBe LedgerViewMode.Accounts
            state.categoryFilter.value shouldBe null
            state.accountFilter.value shouldBe null
            scope.cancel()
        }
    }

    "drilling into a category or account switches to the entries view" {
        onTestMain {
            val scope = testScope()
            val state = LedgerFilterState(scope, LocalDate(2026, 1, 15))

            state.setViewMode(LedgerViewMode.Categories)
            state.openCategory(42)

            state.viewMode.value shouldBe LedgerViewMode.Entries
            state.categoryFilter.value shouldBe 42

            state.setViewMode(LedgerViewMode.Accounts)
            state.openAccount(9)

            state.viewMode.value shouldBe LedgerViewMode.Entries
            state.accountFilter.value shouldBe 9
            scope.cancel()
        }
    }

    "period navigation moves the anchor" {
        onTestMain {
            val scope = testScope()
            val state = LedgerFilterState(scope, LocalDate(2026, 1, 15))

            state.goToNextPeriod()

            state.periodSelection.value.anchor shouldBe LocalDate(2026, 2, 15)

            state.goToPreviousPeriod()
            state.goToPreviousPeriod()

            state.periodSelection.value.anchor shouldBe LocalDate(2025, 12, 15)
            scope.cancel()
        }
    }

    "the categories view treats a Transfer filter as All" {
        onTestMain {
            val scope = testScope()
            val state = LedgerFilterState(scope, LocalDate(2026, 1, 15))

            state.setTypeFilter(EntryType.Expense)

            state.typeFilterForCategories.value shouldBe EntryType.Expense

            state.setTypeFilter(EntryType.Transfer)

            state.typeFilterForCategories.value shouldBe null
            scope.cancel()
        }
    }
})

private fun testScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
