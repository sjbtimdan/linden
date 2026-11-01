package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.accounts.AccountWithBalance
import org.sjbtimdan.linden.ui.onTestMain

private val main = Account(1, "Main", Currency.CHF)
private val savings = Account(2, "Savings", Currency.USD)
private val hidden = Account(3, "Hidden", Currency.CHF, hidden = true)

@OptIn(ExperimentalTestApi::class)
class LedgerAccountsViewTest : StringSpec({
    "shows guided, hidden and no-match empty states" {
        onTestMain {
            runComposeUiTest {
                var action = false
                setContent {
                    LedgerAccountsView(
                        accounts = emptyList(),
                        balances = emptyList(),
                        conversions = emptyMap(),
                        defaultCurrency = Currency.CHF,
                        hideAmounts = false,
                        searchQuery = "",
                        canAdjustBalance = true,
                        currentBalances = emptyMap(),
                        onEmptyAction = { action = true },
                        onAccountClick = {},
                        onAdjustBalance = {},
                    )
                }

                onNodeWithText("No accounts yet.").assertIsDisplayed()
                onNodeWithText("Create an account").performClick()
                action shouldBe true
            }

            runComposeUiTest {
                setContent {
                    LedgerAccountsView(
                        accounts = listOf(hidden),
                        balances = emptyList(),
                        conversions = emptyMap(),
                        defaultCurrency = Currency.CHF,
                        hideAmounts = false,
                        searchQuery = "",
                        canAdjustBalance = true,
                        currentBalances = emptyMap(),
                        onEmptyAction = {},
                        onAccountClick = {},
                        onAdjustBalance = {},
                    )
                }

                onNodeWithText("All accounts are hidden.").assertIsDisplayed()
                onNodeWithText("Manage accounts").assertIsDisplayed()
            }

            runComposeUiTest {
                setContent {
                    LedgerAccountsView(
                        accounts = listOf(main),
                        balances = listOf(AccountWithBalance(main, 1_000)),
                        conversions = emptyMap(),
                        defaultCurrency = Currency.CHF,
                        hideAmounts = false,
                        searchQuery = "zzz",
                        canAdjustBalance = true,
                        currentBalances = emptyMap(),
                        onEmptyAction = {},
                        onAccountClick = {},
                        onAdjustBalance = {},
                    )
                }

                onNodeWithText("No accounts match.").assertIsDisplayed()
                onNodeWithText("Create an account").assertDoesNotExist()
            }
        }
    }

    "filters rows by name and forwards the clicked account" {
        onTestMain {
            runComposeUiTest {
                var clicked: AccountWithBalance? = null
                setContent {
                    LedgerAccountsView(
                        accounts = listOf(main, savings),
                        balances = listOf(
                            AccountWithBalance(main, 1_000),
                            AccountWithBalance(savings, 2_000),
                        ),
                        conversions = emptyMap(),
                        defaultCurrency = Currency.CHF,
                        hideAmounts = false,
                        searchQuery = "Sav",
                        canAdjustBalance = true,
                        currentBalances = emptyMap(),
                        onEmptyAction = {},
                        onAccountClick = { clicked = it },
                        onAdjustBalance = {},
                    )
                }

                onNodeWithText("Savings").assertIsDisplayed()
                onNodeWithText("Main").assertDoesNotExist()
                onNodeWithText("Savings").performClick()

                clicked?.account shouldBe savings
            }
        }
    }

    "builds the Adjust Balance state from the current balance" {
        onTestMain {
            runComposeUiTest {
                var adjusted: AdjustBalanceDialogState? = null
                setContent {
                    LedgerAccountsView(
                        accounts = listOf(main),
                        balances = listOf(AccountWithBalance(main, 1_000)),
                        conversions = emptyMap(),
                        defaultCurrency = Currency.CHF,
                        hideAmounts = false,
                        searchQuery = "",
                        canAdjustBalance = true,
                        currentBalances = mapOf(main.id to 1_234L),
                        onEmptyAction = {},
                        onAccountClick = {},
                        onAdjustBalance = { adjusted = it },
                    )
                }

                onNodeWithContentDescription("More options").performClick()
                onNodeWithText("Adjust Balance").performClick()

                adjusted?.account?.account shouldBe main
                adjusted?.currentBalance shouldBe 1_234L
                adjusted?.targetBalanceText shouldBe "12.34"
            }
        }
    }
})
