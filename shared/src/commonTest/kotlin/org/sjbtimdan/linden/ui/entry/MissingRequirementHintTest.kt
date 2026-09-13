package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.onTestMain
import kotlin.time.Instant

private val main = Account(1, "Main", Currency.CHF)
private val savings = Account(2, "Savings", Currency.CHF)
private val groceries = Category(1, "Groceries", CategoryType.Expense)
private val salary = Category(2, "Salary", CategoryType.Income)

private fun draft(type: EntryType = EntryType.Expense, accountId: Long? = main.id): EntryDraft = EntryDraft(
    editing = null,
    type = type,
    amountText = "",
    categoryId = if (type == EntryType.Transfer) null else groceries.id,
    accountId = accountId,
    toAccountId = null,
    toAmountText = "",
    description = "",
    createdAt = Instant.parse("2026-08-10T14:30:00Z"),
    createdZone = TimeZone.UTC,
)

@OptIn(ExperimentalTestApi::class)
class MissingRequirementHintTest : StringSpec({

    "hidden when the form is valid or there is no draft" {
        missingRequirement(null, listOf(main), listOf(groceries)).shouldBeNull()
        missingRequirement(
            draft(accountId = main.id).copy(amountText = "4.50"),
            listOf(main),
            listOf(groceries),
        ).shouldBeNull()
    }

    "names the first missing requirement when the fields can be satisfied" {
        missingRequirement(draft(), listOf(main), listOf(groceries)) shouldBe MissingRequirement.AMOUNT
        missingRequirement(
            draft(accountId = null).copy(amountText = "4.50"),
            listOf(main),
            listOf(groceries),
        ) shouldBe
            MissingRequirement.ACCOUNT
    }

    "hidden when the form cannot be satisfied" {
        // No accounts: the empty dropdowns and their "+ New" chips explain the blocker.
        missingRequirement(draft(), emptyList(), listOf(groceries)).shouldBeNull()
        // Only income categories exist, so an expense has no category picker.
        missingRequirement(draft(), listOf(main), listOf(salary)).shouldBeNull()
        // A transfer needs two accounts; with one the To dropdown is empty.
        missingRequirement(draft(type = EntryType.Transfer), listOf(main), listOf(groceries)).shouldBeNull()
        // Two accounts: the transfer blocker is now the missing destination account.
        missingRequirement(
            draft(type = EntryType.Transfer).copy(amountText = "100"),
            listOf(main, savings),
            listOf(groceries),
        ) shouldBe MissingRequirement.DESTINATION_ACCOUNT
    }

    "renders the message in error styling" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    MissingRequirementHint(message = "Enter an amount")
                }

                onNodeWithText("Enter an amount").assertIsDisplayed()
            }
        }
    }
})
