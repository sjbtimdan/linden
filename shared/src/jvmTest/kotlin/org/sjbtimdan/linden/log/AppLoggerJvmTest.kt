package org.sjbtimdan.linden.log

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class AppLoggerJvmTest : StringSpec({
    "sanitizedFailure keeps the frames and type but drops the message and cause" {
        val original = IllegalStateException(
            "secret /Users/me/private.json",
            RuntimeException("cause text"),
        )

        val sanitized = sanitizedFailure(AppFailure.ImportFailed, original)

        sanitized.message shouldBe "Linden import (IllegalStateException)"
        sanitized.cause shouldBe null
        sanitized.stackTrace shouldBe original.stackTrace
    }

    "DeduplicatingAppLogger reports each failure kind once" {
        val recording = RecordingAppLogger()
        val logger = DeduplicatingAppLogger(recording)

        logger.report(AppFailure.WriteFailed, IllegalStateException("first"))
        logger.report(AppFailure.WriteFailed, IllegalStateException("second"))
        logger.report(AppFailure.Startup, IllegalStateException("third"))

        recording.reports shouldBe listOf(AppFailure.WriteFailed, AppFailure.Startup)
    }

    "ConsoleAppLogger writes the failure to its sink" {
        val lines = mutableListOf<String>()

        ConsoleAppLogger { lines += it }.report(AppFailure.WriteFailed, IllegalStateException("boom"))

        lines.size shouldBe 1
        lines.single() shouldContain "write"
        lines.single() shouldContain "boom"
    }
})
