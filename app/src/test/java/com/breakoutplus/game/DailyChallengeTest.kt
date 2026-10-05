package com.breakoutplus.game

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class DailyChallengeTest {
    private fun challenge(type: ChallengeType, target: Int) = DailyChallenge(
        "test", "Test", "Test", type, target, RewardType.THEME_UNLOCK, 1)

    @Test fun comboRequiresActualThreeTimesMultiplier() {
        val challenge = challenge(ChallengeType.COMBO_MULTIPLIER, 3)
        val list = mutableListOf(challenge)
        for (streak in 1..6) {
            DailyChallengeManager.updateChallengeProgress(list, ChallengeType.COMBO_MULTIPLIER,
                BrickCollisionFeedback.comboMultiplier(streak).toInt())
            assertFalse(challenge.completed)
        }
        assertEquals(1, DailyChallengeManager.updateChallengeProgress(list, ChallengeType.COMBO_MULTIPLIER,
            BrickCollisionFeedback.comboMultiplier(7).toInt()).size)
        assertTrue(challenge.completed)
        assertFalse(challenge.rewardGranted)
        assertTrue(DailyChallengeManager.updateChallengeProgress(list, ChallengeType.COMBO_MULTIPLIER, 5).isEmpty())
    }

    @Test fun scoreIsMaximumAcrossUpdatesAndRuns() {
        val challenge = challenge(ChallengeType.SCORE_ACHIEVED, 500)
        val list = mutableListOf(challenge)
        for (score in listOf(100, 200, 100, 150))
            DailyChallengeManager.updateChallengeProgress(list, ChallengeType.SCORE_ACHIEVED, score)
        assertEquals(200, challenge.progress)
        assertFalse(challenge.completed)
        DailyChallengeManager.updateChallengeProgress(list, ChallengeType.SCORE_ACHIEVED, 500)
        assertTrue(challenge.completed)
    }

    @Test fun eventObjectivesAccumulateAndClampWithoutOverflow() {
        for (type in listOf(ChallengeType.BRICKS_DESTROYED, ChallengeType.POWERUPS_COLLECTED,
            ChallengeType.LASER_FIRED, ChallengeType.MULTI_BALL_ACTIVE)) {
            val challenge = challenge(type, 3)
            val list = mutableListOf(challenge)
            DailyChallengeManager.updateChallengeProgress(list, type, 1)
            DailyChallengeManager.updateChallengeProgress(list, type, -1)
            assertEquals(1, challenge.progress)
            DailyChallengeManager.updateChallengeProgress(list, type, Int.MAX_VALUE)
            assertEquals(3, challenge.progress)
            assertTrue(challenge.completed)
        }
    }

    @Test fun conditionsUseLevelDurationAndLifeLossBoundary() {
        val time = challenge(ChallengeType.TIME_UNDER_LIMIT, 30)
        val perfect = challenge(ChallengeType.PERFECT_LEVEL, 1)
        val list = listOf(time, perfect)
        DailyChallengeManager.applyEvent(list, ChallengeEvent.Metric(ChallengeType.PERFECT_LEVEL))
        assertFalse(perfect.completed)
        DailyChallengeManager.applyEvent(list, ChallengeEvent.LevelCompleted(30.01f, true))
        assertFalse(time.completed)
        assertFalse(perfect.completed)
        // The next level resets life-loss tracking; total session duration is irrelevant.
        assertEquals(2, DailyChallengeManager.applyEvent(list, ChallengeEvent.LevelCompleted(29.9f, false)).size)
        assertTrue(DailyChallengeManager.applyEvent(list, ChallengeEvent.LevelCompleted(1f, false)).isEmpty())
    }

    @Test fun invalidDurationsCannotCompleteTimeObjectives() {
        val challenge = challenge(ChallengeType.TIME_UNDER_LIMIT, 30)
        for (duration in listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY))
            DailyChallengeManager.applyEvent(listOf(challenge), ChallengeEvent.LevelCompleted(duration, true))
        assertFalse(challenge.completed)
        DailyChallengeManager.applyEvent(listOf(challenge), ChallengeEvent.LevelCompleted(30f, true))
        assertTrue(challenge.completed)
    }

    @Test fun sameDateAndVersionProduceIdenticalChallengesAcrossDateBoundaries() {
        val dates = listOf("2026-10-05", "2026-12-31", "2027-01-01", "2028-02-29")
        for (text in dates) {
            val date = LocalDate.parse(text)
            val first = DailyChallengeManager.generateDailyChallenges(date, 2)
            assertEquals(first, DailyChallengeManager.generateDailyChallenges(date, 2))
            assertNotEquals(first, DailyChallengeManager.generateDailyChallenges(date.plusDays(1), 2))
            assertNotEquals(first, DailyChallengeManager.generateDailyChallenges(date, 3))
            assertEquals(3, first.map { it.id }.distinct().size)
        }
    }
}
