package org.sjbtimdan.linden.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private data class Named(override val id: Long, override val name: String) : NamedEntity

class NamedEntityTest : StringSpec({
    "uniqueName returns the trimmed name when it is free" {
        uniqueName(listOf(Named(1, "Main")), "  Savings ") shouldBe "Savings"
    }

    "uniqueName rejects a blank name" {
        uniqueName(listOf(Named(1, "Main")), "   ") shouldBe null
    }

    "uniqueName rejects a case-insensitive duplicate" {
        uniqueName(listOf(Named(1, "Main")), "MAIN") shouldBe null
    }

    "uniqueName lets an entity keep its own name" {
        uniqueName(listOf(Named(1, "Main")), "Main", excludingId = 1) shouldBe "Main"
    }

    "uniqueName still rejects another entity's name when excluding" {
        uniqueName(listOf(Named(1, "Main"), Named(2, "Savings")), "Savings", excludingId = 1) shouldBe null
    }
})
