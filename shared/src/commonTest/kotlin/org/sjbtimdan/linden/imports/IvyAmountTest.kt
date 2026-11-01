package org.sjbtimdan.linden.imports

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.SerializationException

class IvyAmountTest : StringSpec({
    "parseMinorUnits converts decimals to minor units without floating-point arithmetic" {
        parseMinorUnits("45.50") shouldBe 4_550
        parseMinorUnits("19.99") shouldBe 1_999
        parseMinorUnits("0.29") shouldBe 29
        parseMinorUnits("3200.0") shouldBe 320_000
        parseMinorUnits("1000") shouldBe 100_000
        parseMinorUnits("5.7") shouldBe 570
        parseMinorUnits("-5.75") shouldBe -575
        parseMinorUnits(" 12.34 ") shouldBe 1_234
    }

    "parseMinorUnits rounds amounts with more than two decimal places half-up" {
        parseMinorUnits("19.999") shouldBe 2_000
        parseMinorUnits("19.994") shouldBe 1_999
        parseMinorUnits("19.995") shouldBe 2_000
        parseMinorUnits("9.999") shouldBe 1_000
        parseMinorUnits("5.9999") shouldBe 600
        parseMinorUnits("-19.999") shouldBe -2_000
        parseMinorUnits("0.004") shouldBe 0
        parseMinorUnits("0.005") shouldBe 1
    }

    "parseMinorUnits accepts the exponent notation a JVM serializer may emit" {
        parseMinorUnits("1.0E7") shouldBe 1_000_000_000
        parseMinorUnits("1e3") shouldBe 100_000
        parseMinorUnits("1.23E-2") shouldBe 1
        parseMinorUnits("-1.5E2") shouldBe -15_000
        parseMinorUnits("1.0E+3") shouldBe 100_000
    }

    "parseMinorUnits rounds exponent notation half-up" {
        parseMinorUnits("1.2345E1") shouldBe 1_235
        parseMinorUnits("1.235E0") shouldBe 124
    }

    "parseMinorUnits rejects malformed or overflowing amounts" {
        shouldThrow<SerializationException> { parseMinorUnits("abc") }
        shouldThrow<SerializationException> { parseMinorUnits("1.2.3") }
        shouldThrow<SerializationException> { parseMinorUnits("") }
        shouldThrow<SerializationException> { parseMinorUnits("99999999999999999999") }
        shouldThrow<SerializationException> { parseMinorUnits("1.0E") }
        shouldThrow<SerializationException> { parseMinorUnits("1.0E1.5") }
        shouldThrow<SerializationException> { parseMinorUnits("1.0E9999") }
    }
})
