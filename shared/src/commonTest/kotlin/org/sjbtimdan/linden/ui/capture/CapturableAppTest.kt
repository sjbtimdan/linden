package org.sjbtimdan.linden.ui.capture

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private const val OWN_PACKAGE = "org.sjbtimdan.linden"

class CapturableAppTest : StringSpec({

    "lists an app with a usable label" {
        capturableApp("com.example.bank", "Example Bank", OWN_PACKAGE) shouldBe
            CapturableApp("com.example.bank", "Example Bank")
    }

    "drops an app whose label is null" {
        capturableApp("com.example.bank", null, OWN_PACKAGE) shouldBe null
    }

    "drops an app whose label is blank" {
        capturableApp("com.example.bank", "   ", OWN_PACKAGE) shouldBe null
    }

    "drops an app with no package name" {
        capturableApp(null, "Example Bank", OWN_PACKAGE) shouldBe null
    }

    "drops Linden itself" {
        capturableApp(OWN_PACKAGE, "Linden", OWN_PACKAGE) shouldBe null
    }
})
