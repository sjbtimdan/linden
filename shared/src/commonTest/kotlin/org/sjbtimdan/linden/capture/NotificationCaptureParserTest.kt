package org.sjbtimdan.linden.capture

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType

private fun notification(
    title: String? = null,
    text: String? = null,
    bigText: String? = null,
    subText: String? = null,
) = CaptureNotification(title = title, text = text, bigText = bigText, subText = subText)

class NotificationCaptureParserTest : StringSpec({

    "parses a card payment with the currency code before the amount" {
        parseCaptureNotification(notification(text = "You spent CHF 24.50 at COOP")) shouldBe
            ParsedCapture(2450, Currency.CHF, "You spent CHF 24.50 at COOP", EntryType.Expense)
    }

    "parses an amount that precedes the currency code" {
        parseCaptureNotification(notification(text = "Payment 24.50 CHF to Migros")) shouldBe
            ParsedCapture(2450, Currency.CHF, "Payment 24.50 CHF to Migros", EntryType.Expense)
    }

    "parses comma decimals" {
        parseCaptureNotification(notification(text = "Paiement de 24,50 EUR chez CARREFOUR")) shouldBe
            ParsedCapture(2450, Currency.EUR, "Paiement de 24,50 EUR chez CARREFOUR", EntryType.Expense)
    }

    "parses apostrophe grouping" {
        parseCaptureNotification(notification(text = "Sie haben CHF 1'234.56 bei COOP bezahlt.")) shouldBe
            ParsedCapture(123456, Currency.CHF, "Sie haben CHF 1'234.56 bei COOP bezahlt.", EntryType.Expense)
    }

    "parses dotted grouping with comma decimals" {
        parseCaptureNotification(notification(text = "Zahlung: 1.234,56 EUR bei REWE")) shouldBe
            ParsedCapture(123456, Currency.EUR, "Zahlung: 1.234,56 EUR bei REWE", EntryType.Expense)
    }

    "parses NBSP grouping" {
        parseCaptureNotification(notification(text = "CHF 1\u00A0234.56")) shouldBe
            ParsedCapture(123456, Currency.CHF, "CHF 1\u00A0234.56", EntryType.Expense)
    }

    "parses a symbol after the amount" {
        parseCaptureNotification(notification(text = "Pagamento di 24,50 € presso COOP")) shouldBe
            ParsedCapture(2450, Currency.EUR, "Pagamento di 24,50 € presso COOP", EntryType.Expense)
    }

    "parses a symbol before the amount" {
        parseCaptureNotification(notification(text = "€24.50 at Migros")) shouldBe
            ParsedCapture(2450, Currency.EUR, "€24.50 at Migros", EntryType.Expense)
    }

    "treats ambiguous symbols as an unknown currency" {
        parseCaptureNotification(notification(text = "Card purchase $24.50")) shouldBe
            ParsedCapture(2450, null, "Card purchase $24.50", EntryType.Expense)
    }

    "prefers the longer symbol when one contains another" {
        parseCaptureNotification(notification(text = "消费 CN¥24.50 于 COOP")) shouldBe
            ParsedCapture(2450, Currency.CNY, "消费 CN¥24.50 于 COOP", EntryType.Expense)
    }

    "parses rupiah with dot grouping at whole units" {
        parseCaptureNotification(notification(text = "Pembayaran Rp24.500")) shouldBe
            ParsedCapture(2_450_000, Currency.IDR, "Pembayaran Rp24.500", EntryType.Expense)
    }

    "cleans a trailing separator after the amount" {
        parseCaptureNotification(notification(text = "CHF 24.50, at COOP")) shouldBe
            ParsedCapture(2450, Currency.CHF, "CHF 24.50, at COOP", EntryType.Expense)
    }

    "reads a leading minus as a debit" {
        parseCaptureNotification(notification(text = "CHF \u221224.50")) shouldBe
            ParsedCapture(2450, Currency.CHF, "CHF \u221224.50", EntryType.Expense)
    }

    "reads a leading plus as a credit" {
        parseCaptureNotification(notification(text = "CHF +100.00")) shouldBe
            ParsedCapture(10000, Currency.CHF, "CHF +100.00", EntryType.Income)
    }

    "reads credited wording as income" {
        parseCaptureNotification(notification(text = "You received CHF 50.00 from Anna")) shouldBe
            ParsedCapture(5000, Currency.CHF, "You received CHF 50.00 from Anna", EntryType.Income)
    }

    "rejects declined payments" {
        parseCaptureNotification(notification(text = "Your payment of CHF 24.50 was declined")) shouldBe null
    }

    "rejects failed payments" {
        parseCaptureNotification(notification(text = "CHF 24.50 payment failed")) shouldBe null
    }

    "rejects one-time-password notifications even when they carry an amount" {
        parseCaptureNotification(notification(text = "Use OTP 123456 to confirm your CHF 24.50 payment")) shouldBe null
    }

    "rejects an OTP code without any currency" {
        parseCaptureNotification(notification(text = "Your verification code is 123456")) shouldBe null
    }

    "rejects amounts without a currency marker" {
        parseCaptureNotification(notification(text = "You paid 24.50 at COOP")) shouldBe null
    }

    "rejects a zero amount" {
        parseCaptureNotification(notification(text = "CHF 0.00")) shouldBe null
    }

    "does not match currency codes inside longer words" {
        parseCaptureNotification(notification(text = "USDT 24.50")) shouldBe null
    }

    "uses the first currency amount in the text" {
        parseCaptureNotification(notification(text = "You paid CHF 24.50; available balance CHF 1,500.00")) shouldBe
            ParsedCapture(2450, Currency.CHF, "You paid CHF 24.50; available balance CHF 1,500.00", EntryType.Expense)
    }

    "prefers the expanded text over the short text" {
        parseCaptureNotification(
            notification(text = "Payment", bigText = "Payment of CHF 24.50 at COOP"),
        ) shouldBe ParsedCapture(2450, Currency.CHF, "Payment of CHF 24.50 at COOP", EntryType.Expense)
    }

    "falls back to the title for the description" {
        parseCaptureNotification(notification(title = "CHF 24.50", text = "Migros")) shouldBe
            ParsedCapture(2450, Currency.CHF, "Migros", EntryType.Expense)
    }

    "joins multiline bodies into one description line" {
        parseCaptureNotification(notification(text = "Payment of CHF 24.50\nat COOP")) shouldBe
            ParsedCapture(2450, Currency.CHF, "Payment of CHF 24.50 at COOP", EntryType.Expense)
    }

    "returns null for blank notifications" {
        parseCaptureNotification(CaptureNotification()) shouldBe null
        parseCaptureNotification(notification(title = " ", text = "")) shouldBe null
    }
})
