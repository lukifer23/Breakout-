package com.breakoutplus.game

import org.junit.Assert.*
import org.junit.Test

class DailyChallengeCodecTest {
    private val legacy = """[{"id":"combo_3x","title":"Combo","description":"Reach 3x","type":"COMBO_MULTIPLIER","targetValue":3,"rewardType":"STREAK_BONUS","rewardValue":2,"progress":2,"completed":false,"rewardGranted":false,"dateGenerated":123}]"""

    @Test fun migratesLegacyWithoutInventingComboEvidence() {
        val c = DailyChallengeCodec.decode(legacy).single()
        assertEquals(0, c.progress)
        assertEquals("combo_3x", c.id)
        assertEquals(123L, c.dateGenerated)
        assertEquals(c, DailyChallengeCodec.decode(DailyChallengeCodec.encode(listOf(c))).single())
    }

    @Test fun preservesCompletedLegacyRewardReceipt() {
        val raw = legacy.replace("\"completed\":false", "\"completed\":true")
            .replace("\"rewardGranted\":false", "\"rewardGranted\":true")
        val c = DailyChallengeCodec.decode(raw).single()
        assertEquals(3, c.progress)
        assertTrue(c.rewardGranted)
    }

    @Test fun clampsMalformedValuesAndRejectsUnreadableOrUnknownSchemas() {
        val raw = legacy.replace("\"targetValue\":3", "\"targetValue\":-3")
        assertEquals(1, DailyChallengeCodec.decode(raw).single().targetValue)
        for (bad in listOf("not json", "[]", "{\"version\":999,\"challenges\":[]}"))
            assertThrows(Exception::class.java) { DailyChallengeCodec.decode(bad) }
    }
}
