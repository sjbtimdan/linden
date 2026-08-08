package org.sjbtimdan.linden.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Currency

class AccountDaoTest : StringSpec({
    "CRUD operations should work" {
        val database = lindenDatabase()
        val dao = AccountDao(database.accountQueries)

        dao.getAll().first() shouldBe emptyList()
        dao.create("Main", Currency.CHF)
        val allCreated = dao.getAll().first()
        allCreated shouldBe listOf(Account(
            id = allCreated.first().id,
            name = "Main",
            currency = Currency.CHF
        ))
        val updated = allCreated.first().copy(name = "Savings", currency = Currency.EUR)
        dao.update(updated)
        dao.getAll().first() shouldBe listOf(updated)
    }

    "multiple accounts are ordered by name" {
        val database = lindenDatabase()
        val dao = AccountDao(database.accountQueries)

        dao.create("B", Currency.USD)
        dao.create("A", Currency.SGD)
        dao.create("C", Currency.HKD)

        val names = dao.getAll().first().map { it.name }
        names shouldBe listOf("A", "B", "C")
    }

    "Currency.fromCode returns the enum for a known code" {
        Currency.entries.forEach { currency ->
            Currency.fromCode(currency.name) shouldBe currency
        }
    }

    "Currency.fromCode fails fast for an unknown code" {
        shouldThrow<IllegalStateException> {
            Currency.fromCode("XYZ")
        }
    }
})
