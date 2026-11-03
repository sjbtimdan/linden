package org.sjbtimdan.linden.capture

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CaptureRecoveryTest : StringSpec({
    "requests a rebind even when no listener is connected" {
        var rebinds = 0
        val recovery = CaptureRecovery(listener = { null }, requestRebind = { rebinds++ })

        recovery.recover()

        rebinds shouldBe 1
    }

    "re-scans the shade when a listener is connected" {
        var recovered = 0
        val listener = CaptureRecoveryListener { recovered++ }
        val recovery = CaptureRecovery(listener = { listener }, requestRebind = {})

        recovery.recover()

        recovered shouldBe 1
    }

    "requests the rebind before re-scanning" {
        val calls = mutableListOf<String>()
        val listener = CaptureRecoveryListener { calls += "recover" }
        val recovery = CaptureRecovery(
            listener = { listener },
            requestRebind = { calls += "rebind" },
        )

        recovery.recover()

        calls shouldBe listOf("rebind", "recover")
    }
})
