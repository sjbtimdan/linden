package org.sjbtimdan.linden.capture

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private const val NOW = 1_700_000_000_000L
private const val BANK = "com.example.bank"
private val ALLOWED = setOf(BANK)

private fun candidate(packageName: String = BANK, key: String = "key", postTime: Long = NOW) = CaptureCandidate(
    packageName = packageName,
    key = key,
    postTime = postTime,
    content = CaptureNotification(title = "Paid", text = "CHF 12.50 at Coop"),
)

class CaptureCandidatesTest : StringSpec({

    "drops notifications from apps that are not allowlisted" {
        val result = recentCaptureCandidates(
            candidates = listOf(candidate(packageName = "com.example.other")),
            allowedPackages = ALLOWED,
            nowMillis = NOW,
        )

        result.shouldBeEmpty()
    }

    "keeps an allowlisted notification inside the window" {
        val inside = candidate(postTime = NOW - 1)

        val result = recentCaptureCandidates(listOf(inside), ALLOWED, NOW)

        result shouldBe listOf(inside)
    }

    "accepts a notification posted exactly at the window edge" {
        val edge = candidate(key = "edge", postTime = NOW - CATCH_UP_WINDOW_MILLIS)

        val result = recentCaptureCandidates(listOf(edge), ALLOWED, NOW)

        result shouldBe listOf(edge)
    }

    "drops a notification one millisecond older than the window" {
        val tooOld = candidate(key = "old", postTime = NOW - CATCH_UP_WINDOW_MILLIS - 1)

        val result = recentCaptureCandidates(listOf(tooOld), ALLOWED, NOW)

        result.shouldBeEmpty()
    }

    "returns survivors oldest first" {
        val older = candidate(key = "old", postTime = NOW - 2_000)
        val newer = candidate(key = "new", postTime = NOW - 1_000)

        val result = recentCaptureCandidates(listOf(newer, older), ALLOWED, NOW)

        result.map { it.key } shouldBe listOf("old", "new")
    }

    "keeps the input order for equal timestamps" {
        val first = candidate(key = "first", postTime = NOW - 1_000)
        val second = candidate(key = "second", postTime = NOW - 1_000)

        val result = recentCaptureCandidates(listOf(first, second), ALLOWED, NOW)

        result.map { it.key } shouldBe listOf("first", "second")
    }
})
