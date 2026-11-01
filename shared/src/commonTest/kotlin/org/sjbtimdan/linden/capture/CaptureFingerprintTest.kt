package org.sjbtimdan.linden.capture

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private fun content(text: String = "You paid CHF 12.50 at COOP") = CaptureNotification(title = "Bank", text = text)

class CaptureFingerprintTest : StringSpec({

    "the same notification version has the same fingerprint" {
        captureFingerprint("key", content()) shouldBe captureFingerprint("key", content())
    }

    "a different notification key changes the fingerprint" {
        captureFingerprint("key-a", content()) shouldNotBe captureFingerprint("key-b", content())
    }

    "changed notification text changes the fingerprint" {
        captureFingerprint("key", content()) shouldNotBe captureFingerprint("key", content("You paid CHF 8.00 at COOP"))
    }

    "a difference hidden in another captured field still changes the fingerprint" {
        val base = CaptureNotification(title = "Bank", text = "You paid CHF 12.50 at COOP")
        val withBigText = base.copy(bigText = "You paid CHF 12.50 at COOP")
        val withSubText = base.copy(subText = "Card ending 1234")

        captureFingerprint("key", base) shouldNotBe captureFingerprint("key", withBigText)
        captureFingerprint("key", base) shouldNotBe captureFingerprint("key", withSubText)
    }
})
