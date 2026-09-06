package org.sjbtimdan.linden.ui.ledger

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency

private val groceries = Category(1, "Groceries", CategoryType.Expense)
private val restaurant = Category(2, "Restaurant", CategoryType.Expense)
private val transport = Category(3, "Transport", CategoryType.Expense)
private val mainAccount = Account(1, "Main", Currency.CHF)
private val coopAccount = Account(2, "Coop", Currency.CHF)
private val savingsAccount = Account(3, "Savings", Currency.CHF)

class FilterSuggestionsTest : StringSpec({
    "a blank query suggests nothing" {
        rankFilterSuggestions("   ", listOf(groceries), listOf(mainAccount)) shouldBe emptyList()
    }

    "a query matching no name suggests nothing" {
        rankFilterSuggestions("zzz", listOf(groceries), listOf(mainAccount)) shouldBe emptyList()
    }

    "an empty category list still suggests matching accounts" {
        rankFilterSuggestions("co", emptyList(), listOf(mainAccount, coopAccount))
            .shouldContainExactly(FilterSuggestion(FilterSuggestionKind.Account, coopAccount.id, "Coop"))
    }

    "matches are case-insensitive substrings" {
        rankFilterSuggestions("GROC", listOf(groceries), emptyList())
            .shouldContainExactly(FilterSuggestion(FilterSuggestionKind.Category, groceries.id, "Groceries"))
    }

    "prefix matches rank above later substring matches" {
        rankFilterSuggestions("ra", listOf(transport, restaurant), emptyList()).shouldContainExactly(
            FilterSuggestion(FilterSuggestionKind.Category, restaurant.id, "Restaurant"),
            FilterSuggestion(FilterSuggestionKind.Category, transport.id, "Transport"),
        )
    }

    "equal-rank matches stay alphabetical" {
        // "Coop" (account), "Groceries" and "Transport" all contain "o".
        rankFilterSuggestions("o", listOf(groceries, restaurant, transport), listOf(coopAccount))
            .shouldContainExactly(
                FilterSuggestion(FilterSuggestionKind.Account, coopAccount.id, "Coop"),
                FilterSuggestion(FilterSuggestionKind.Category, groceries.id, "Groceries"),
                FilterSuggestion(FilterSuggestionKind.Category, transport.id, "Transport"),
            )
    }

    "an active category filter is not suggested again" {
        rankFilterSuggestions(
            "r",
            listOf(groceries, restaurant),
            listOf(mainAccount),
            activeCategoryId = restaurant.id,
        ).shouldContainExactly(FilterSuggestion(FilterSuggestionKind.Category, groceries.id, "Groceries"))
    }

    "an active account filter is not suggested again" {
        val optic = Account(4, "Optic", Currency.CHF)
        // Both accounts match "op"; the active one is dropped, the other stays.
        rankFilterSuggestions(
            "op",
            emptyList(),
            listOf(coopAccount, optic),
            activeAccountId = optic.id,
        ).shouldContainExactly(FilterSuggestion(FilterSuggestionKind.Account, coopAccount.id, "Coop"))
    }

    "the same name in both lists suggests both kinds" {
        val company = Category(4, "Coop", CategoryType.Expense)
        rankFilterSuggestions("coop", listOf(company), listOf(coopAccount))
            .shouldContainExactly(
                FilterSuggestion(FilterSuggestionKind.Account, coopAccount.id, "Coop"),
                FilterSuggestion(FilterSuggestionKind.Category, company.id, "Coop"),
            )
    }

    "results are capped at the limit" {
        val many = (1..10).map { Category(it.toLong(), "Shopping $it", CategoryType.Expense) }
        rankFilterSuggestions("shopping", many, emptyList(), limit = 3).size shouldBe 3
        rankFilterSuggestions("shopping", many, emptyList(), limit = 3)
            .shouldContainExactly(
                FilterSuggestion(FilterSuggestionKind.Category, 1, "Shopping 1"),
                FilterSuggestion(FilterSuggestionKind.Category, 10, "Shopping 10"),
                FilterSuggestion(FilterSuggestionKind.Category, 2, "Shopping 2"),
            )
    }
})
