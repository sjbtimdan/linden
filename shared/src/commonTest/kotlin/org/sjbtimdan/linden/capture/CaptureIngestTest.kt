package org.sjbtimdan.linden.capture

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.time.TEST_NOW

private const val BANK = "com.example.bank"
private const val ZONE = "Europe/Zurich"
private val NOW = TEST_NOW.toEpochMilliseconds()

private fun candidate(
    packageName: String = BANK,
    key: String = "key-1",
    postTime: Long = NOW,
    content: CaptureNotification = CaptureNotification(title = "Paid", text = "CHF 12.50 at Coop"),
) = CaptureCandidate(packageName, key, postTime, content)

private suspend fun newRepository(): CaptureRepository =
    CaptureRepository(SettingsDao(lindenDatabase().settingsQueries))

private fun ingest(repository: CaptureRepository) = CaptureIngest(
    repository = repository,
    clock = FakeClock(),
    zoneId = { ZONE },
)

class CaptureIngestTest : StringSpec({

    "the first catch-up seeds the shade instead of importing it" {
        val repository = newRepository()
        repository.setRule(CaptureRule(BANK, accountId = 7))

        val result = ingest(repository).catchUp(listOf(candidate()))

        result shouldBe CaptureCatchUpResult(imported = 0, seeded = 1)
        repository.pending().shouldBeEmpty()
        repository.processedNotifications()?.size shouldBe 1
    }

    "a catch-up later imports an allowlisted payment" {
        val repository = newRepository()
        repository.rememberProcessed(emptyList())
        repository.setRule(CaptureRule(BANK, accountId = 7))

        val result = ingest(repository).catchUp(listOf(candidate()))

        result shouldBe CaptureCatchUpResult(imported = 1)
        val pending = repository.pending().single()
        pending.amountMinor shouldBe 1_250
        pending.currency shouldBe Currency.CHF
        pending.type shouldBe EntryType.Expense
        pending.capturedAtMillis shouldBe NOW
        pending.capturedAtZoneId shouldBe ZONE
    }

    "a catch-up skips an app without an enabled rule" {
        val repository = newRepository()
        repository.rememberProcessed(emptyList())

        val result = ingest(repository).catchUp(listOf(candidate(packageName = "com.example.other")))

        result.imported shouldBe 0
        repository.pending().shouldBeEmpty()
    }

    "a catch-up ignores notifications older than the window" {
        val repository = newRepository()
        repository.rememberProcessed(emptyList())
        repository.setRule(CaptureRule(BANK, accountId = 7))
        val tooOld = candidate(postTime = NOW - CATCH_UP_WINDOW_MILLIS - 1)

        val result = ingest(repository).catchUp(listOf(tooOld), nowMillis = NOW)

        result.imported shouldBe 0
        repository.pending().shouldBeEmpty()
    }

    "a catch-up imports oldest first so the newest capture is on top" {
        val repository = newRepository()
        repository.rememberProcessed(emptyList())
        repository.setRule(CaptureRule(BANK, accountId = 7))
        val older = candidate(key = "old", postTime = NOW - 2_000)
        val newer = candidate(key = "new", postTime = NOW - 1_000)

        ingest(repository).catchUp(listOf(newer, older))

        repository.pending().map { it.capturedAtMillis } shouldBe listOf(NOW - 1_000, NOW - 2_000)
    }

    "process remembers the fingerprint so a re-post cannot duplicate the capture" {
        val repository = newRepository()
        repository.setRule(CaptureRule(BANK, accountId = 7))
        val ingest = ingest(repository)

        ingest.process(candidate()) shouldNotBe null

        ingest.process(candidate()) shouldBe null
        repository.pending().size shouldBe 1
    }

    "process ignores a non-payment and leaves the store unseeded" {
        val repository = newRepository()
        repository.setRule(CaptureRule(BANK, accountId = 7))
        val otp = candidate(content = CaptureNotification(title = "Your code", text = "OTP CHF 12.50"))

        ingest(repository).process(otp) shouldBe null

        repository.pending().shouldBeEmpty()
        repository.processedNotifications() shouldBe null
    }

    "a disabled rule blocks processing" {
        val repository = newRepository()
        repository.setRule(CaptureRule(BANK, accountId = 7, enabled = false))

        ingest(repository).process(candidate()) shouldBe null

        repository.pending().shouldBeEmpty()
    }
})
