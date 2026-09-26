package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        // The form shows Category above Account, so an empty form names Category first.
        missingRequirement(
            draft(accountId = null).copy(amountText = "4.50", categoryId = null),
            listOf(main),
            listOf(groceries),
        ) shouldBe
            MissingRequirement.CATEGORY
        missingRequirement(
            draft(accountId = null).copy(amountText = "4.50"),
            listOf(main),
            listOf(groceries),
        ) shouldBe
            MissingRequirement.ACCOUNT
    }

    "reports a create-action blocker when the form cannot be satisfied" {
        // No accounts: nothing to pick, the hint must offer creating one.
        missingRequirement(draft(), emptyList(), listOf(groceries)) shouldBe
            MissingRequirement.NO_ACCOUNTS
        // Only income categories exist, so an expense has no category picker.
        missingRequirement(draft(), listOf(main), listOf(salary)) shouldBe
            MissingRequirement.NO_CATEGORY
        // A transfer needs two accounts; with one the To dropdown is empty.
        missingRequirement(draft(type = EntryType.Transfer), listOf(main), listOf(groceries)) shouldBe
            MissingRequirement.NO_ACCOUNTS
        // Two accounts: the transfer blocker is now the missing destination account.
        missingRequirement(
            draft(type = EntryType.Transfer).copy(amountText = "100"),
            listOf(main, savings),
            listOf(groceries),
        ) shouldBe MissingRequirement.DESTINATION_ACCOUNT
    }

    "only the create-action blockers are marked as create actions" {
        MissingRequirement.NO_ACCOUNTS.isCreateAction shouldBe true
        MissingRequirement.NO_CATEGORY.isCreateAction shouldBe true
        MissingRequirement.entries.filterNot { it.isCreateAction }.forEach { requirement ->
            requirement.isCreateAction shouldBe false
        }
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

    "renders as a tappable action when onClick is given" {
        onTestMain {
            runComposeUiTest {
                var clicked = false
                setContent {
                    MissingRequirementHint(message = "Add an account to continue", onClick = { clicked = true })
                }

                onNodeWithTag("missingRequirementAction").assertIsDisplayed()
                onNodeWithText("Add an account to continue").performClick()
                clicked shouldBe true
            }
        }
    }
})
