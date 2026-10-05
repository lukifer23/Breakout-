package com.breakoutplus.game

import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class RewardLedgerTest {
    private fun challenge(id: String, type: RewardType) = DailyChallenge(id, id, id,
        ChallengeType.BRICKS_DESTROYED, 1, type, 5, progress = 1, completed = true, dateGenerated = 100)

    @Test fun cosmeticCommitCanBeReenteredBeforeAcknowledgement() {
        val challenge = challenge("cosmetic", RewardType.COSMETIC_UNLOCK)
        val committed = RewardLedger().grant(challenge, listOf("Aurora"))
        val file = Files.createTempFile("breakout-rewards", ".json")
        try {
            Files.write(file, RewardLedgerCodec.encode(committed).toByteArray())
            val restored = RewardLedgerCodec.decode(String(Files.readAllBytes(file)))
            assertEquals(1, restored.cosmeticTier)
            assertEquals(restored, restored.grant(challenge, listOf("Aurora")))
        } finally { Files.delete(file) }
    }

    @Test fun queuedBonusesSurviveReloadAndReserveExactlyOnce() {
        val challenge = challenge("score", RewardType.SCORE_MULTIPLIER)
        var ledger = RewardLedger().grant(challenge, emptyList())
        ledger = RewardLedgerCodec.decode(RewardLedgerCodec.encode(ledger))
        assertEquals(1, ledger.pending.size)
        ledger = ledger.reserve("run-a")
        val restored = RewardLedgerCodec.decode(RewardLedgerCodec.encode(ledger))
        assertEquals(restored, restored.reserve("run-a"))
        assertEquals(5, restored.reservations.getValue("run-a").scorePercent)
        assertEquals(0, restored.reserve("run-b").reservations.getValue("run-b").scorePercent)
        assertEquals(restored, restored.grant(challenge, emptyList()))
    }

    @Test fun themeGrantAndReceiptAreOneStateAndMaxTierHasRealFallback() {
        val theme = challenge("theme", RewardType.THEME_UNLOCK)
        val ledger = RewardLedger().grant(theme, listOf("Aurora", "Cobalt"))
        assertEquals(setOf("Aurora"), ledger.themes)
        assertEquals(1, ledger.granted.size)
        val max = RewardLedger(cosmeticTier = 3).grant(challenge("max", RewardType.COSMETIC_UNLOCK), emptyList())
        assertEquals(3, max.cosmeticTier)
        assertEquals(5, max.pending.single().value)
    }

    @Test fun unfinishedChallengesCannotGrantAndUnsupportedSchemaFails() {
        val challenge = challenge("unfinished", RewardType.STREAK_BONUS).copy(completed = false)
        assertEquals(RewardLedger(), RewardLedger().grant(challenge, emptyList()))
        assertThrows(IllegalArgumentException::class.java) { RewardLedgerCodec.decode("{\"version\":999}") }
    }
}
