package org.sjbtimdan.linden.ui.entry

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/** Drives the model from a compact key sequence: digits, '.', + - * /, '=', 'c' (clear), 'b' (backspace). */
private fun CalculatorModel.type(keys: String): CalculatorModel {
    keys.forEach { key ->
        when (key) {
            in '0'..'9' -> onDigit(key)
            '.' -> onDot()
            '+' -> onOperator(CalculatorOp.Add)
            '-' -> onOperator(CalculatorOp.Subtract)
            '*' -> onOperator(CalculatorOp.Multiply)
            '/' -> onOperator(CalculatorOp.Divide)
            '=' -> onEquals()
            'c' -> onClear()
            'b' -> onBackspace()
        }
    }
    return this
}

class CalculatorModelTest : StringSpec({

    "divides and rounds the display to two decimals" {
        val model = CalculatorModel(null).type("100/3=")
        model.display shouldBe "33.33"
        model.commitValue shouldBe "33.33"
    }

    "keeps chained operations exact" {
        CalculatorModel(null).type("100/3*3=").display shouldBe "100.00"
        CalculatorModel(null).type("100/3*3=").commitValue shouldBe "100.00"
    }

    "adds decimals without floating point artifacts" {
        CalculatorModel(null).type("0.1+0.2=").display shouldBe "0.30"
    }

    "evaluates left to right without operator precedence" {
        CalculatorModel(null).type("1+2*3=").display shouldBe "9.00"
    }

    "chains through a negative intermediate" {
        CalculatorModel(null).type("10-20+30=").display shouldBe "20.00"
    }

    "rejects a negative result at commit" {
        val model = CalculatorModel(null).type("5-10=")
        model.display shouldBe "-5.00"
        model.commitValue.shouldBeNull()
    }

    "rejects zero and empty at commit" {
        CalculatorModel(null).type("0").commitValue.shouldBeNull()
        CalculatorModel(null).commitValue.shouldBeNull()
        CalculatorModel(0).commitValue.shouldBeNull()
    }

    "shows an error on division by zero" {
        val model = CalculatorModel(null).type("5/0=")
        model.display shouldBe "Err"
        model.commitValue.shouldBeNull()
    }

    "clears an error with C" {
        CalculatorModel(null).type("5/0=c").display shouldBe "0.00"
    }

    "typing after equals starts a fresh entry" {
        val model = CalculatorModel(null).type("2+3=7")
        model.display shouldBe "7"
        model.commitValue shouldBe "7.00"
    }

    "typing after an error starts fresh" {
        CalculatorModel(null).type("5/0=7").display shouldBe "7"
    }

    "repeats the last operand when equals is pressed without a second entry" {
        CalculatorModel(null).type("100+=").display shouldBe "200.00"
    }

    "backspace removes the last digit" {
        CalculatorModel(null).type("123bb").display shouldBe "1"
    }

    "swallows leading zeros" {
        CalculatorModel(null).type("007").display shouldBe "7"
        CalculatorModel(null).type("00.5").display shouldBe "0.5"
    }

    "ignores a second decimal point" {
        CalculatorModel(null).type("1.2.3").display shouldBe "1.23"
        CalculatorModel(null).type(".").display shouldBe "0."
    }

    "rounds half up at commit" {
        CalculatorModel(null).type("1.999=").display shouldBe "2.00"
        CalculatorModel(null).type("1/8=").display shouldBe "0.13"
        CalculatorModel(null).type("2/3=").display shouldBe "0.67"
    }

    "treats a trailing decimal point as a whole number" {
        CalculatorModel(null).type("1.=").display shouldBe "1.00"
    }

    "prefills from minor units" {
        val model = CalculatorModel(10_000)
        model.display shouldBe "100.00"
        model.commitValue shouldBe "100.00"
    }

    "ignores equals with nothing entered" {
        val model = CalculatorModel(null).type("=")
        model.display shouldBe "0.00"
        model.commitValue.shouldBeNull()
    }

    "chains from a result after equals" {
        CalculatorModel(null).type("2+3=+4=").display shouldBe "9.00"
    }

    "replaces the pending operator" {
        CalculatorModel(null).type("5+*3=").display shouldBe "15.00"
    }

    "caps the number of typed digits" {
        CalculatorModel(null).type("1234567890123456").display shouldBe "12345678901234"
    }

    "shows an error when arithmetic overflows" {
        CalculatorModel(null).type("99999999999999*99999999999999=").display shouldBe "Err"
    }
})
