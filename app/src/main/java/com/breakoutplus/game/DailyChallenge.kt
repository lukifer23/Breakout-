package com.breakoutplus.game

import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Random

/**
 * Daily Challenge system for added replayability and goals
 */
data class DailyChallenge(
    val id: String,
    val title: String,
    val description: String,
    val type: ChallengeType,
    val targetValue: Int,
    val rewardType: RewardType,
    val rewardValue: Int,
    var progress: Int = 0,
    var completed: Boolean = false,
    var rewardGranted: Boolean = false,
    val dateGenerated: Long = System.currentTimeMillis()
)

enum class ChallengeProgress { ACCUMULATE, MAXIMUM, LEVEL_CONDITION }

enum class ChallengeType(val semantics: ChallengeProgress) {
    BRICKS_DESTROYED(ChallengeProgress.ACCUMULATE),
    SCORE_ACHIEVED(ChallengeProgress.MAXIMUM),
    COMBO_MULTIPLIER(ChallengeProgress.MAXIMUM),
    POWERUPS_COLLECTED(ChallengeProgress.ACCUMULATE),
    PERFECT_LEVEL(ChallengeProgress.LEVEL_CONDITION),
    TIME_UNDER_LIMIT(ChallengeProgress.LEVEL_CONDITION),
    // Counts collected MULTI_BALL activations, not balls or elapsed active ticks.
    MULTI_BALL_ACTIVE(ChallengeProgress.ACCUMULATE),
    LASER_FIRED(ChallengeProgress.ACCUMULATE)
}

sealed interface ChallengeEvent {
    data class Metric(val type: ChallengeType, val value: Int = 1) : ChallengeEvent
    data class LevelCompleted(val durationSeconds: Float, val lostLife: Boolean) : ChallengeEvent
}

enum class RewardType {
    COSMETIC_UNLOCK,
    THEME_UNLOCK,
    STREAK_BONUS,
    SCORE_MULTIPLIER
}

object DailyChallengeManager {

    private val challengeTemplates = listOf(
        // Destruction challenges
        { DailyChallenge("bricks_25", "Brick Buster", "Destroy 25 bricks", ChallengeType.BRICKS_DESTROYED, 25, RewardType.SCORE_MULTIPLIER, 10) },
        { DailyChallenge("bricks_50", "Brick Demolisher", "Destroy 50 bricks", ChallengeType.BRICKS_DESTROYED, 50, RewardType.STREAK_BONUS, 5) },
        { DailyChallenge("bricks_100", "Brick Annihilator", "Destroy 100 bricks", ChallengeType.BRICKS_DESTROYED, 100, RewardType.COSMETIC_UNLOCK, 1) },

        // Score challenges
        { DailyChallenge("score_500", "Score Hunter", "Achieve 500 points", ChallengeType.SCORE_ACHIEVED, 500, RewardType.STREAK_BONUS, 3) },
        { DailyChallenge("score_1000", "Score Master", "Achieve 1000 points", ChallengeType.SCORE_ACHIEVED, 1000, RewardType.SCORE_MULTIPLIER, 15) },

        // Combo challenges
        { DailyChallenge("combo_3x", "Combo Starter", "Achieve 3x combo multiplier", ChallengeType.COMBO_MULTIPLIER, 3, RewardType.STREAK_BONUS, 2) },
        { DailyChallenge("combo_5x", "Combo Expert", "Achieve 5x combo multiplier", ChallengeType.COMBO_MULTIPLIER, 5, RewardType.SCORE_MULTIPLIER, 5) },

        // Special challenges
        { DailyChallenge("powerups_5", "Power Collector", "Collect 5 powerups", ChallengeType.POWERUPS_COLLECTED, 5, RewardType.COSMETIC_UNLOCK, 1) },
        { DailyChallenge("perfect_level", "Perfectionist", "Complete a level without losing a life", ChallengeType.PERFECT_LEVEL, 1, RewardType.THEME_UNLOCK, 1) },
        { DailyChallenge("laser_master", "Laser Commander", "Fire laser 10 times", ChallengeType.LASER_FIRED, 10, RewardType.STREAK_BONUS, 4) },
        { DailyChallenge("speed_run_30", "Speed Runner", "Clear a level under 30 seconds", ChallengeType.TIME_UNDER_LIMIT, 30, RewardType.STREAK_BONUS, 4) },
        { DailyChallenge("multiball_3", "Ball Party", "Activate multi-ball 3 times", ChallengeType.MULTI_BALL_ACTIVE, 3, RewardType.SCORE_MULTIPLIER, 8) }
    )

    const val SCHEMA_VERSION = 2

    fun generateDailyChallenges(
        date: LocalDate = LocalDate.now(),
        schemaVersion: Int = SCHEMA_VERSION
    ): List<DailyChallenge> {
        require(schemaVersion > 0)
        val random = Random(date.toEpochDay() xor (schemaVersion.toLong() shl 32))
        val timestamp = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        return challengeTemplates.shuffled(random).take(3).map {
            val challenge = it()
            challenge.copy(id = "$date/v$schemaVersion/${challenge.id}", dateGenerated = timestamp)
        }
    }

    fun updateChallengeProgress(challenges: MutableList<DailyChallenge>, type: ChallengeType, value: Int = 1): List<DailyChallenge> =
        applyEvent(challenges, ChallengeEvent.Metric(type, value))

    fun applyEvent(challenges: List<DailyChallenge>, event: ChallengeEvent): List<DailyChallenge> {
        val newlyCompleted = mutableListOf<DailyChallenge>()
        for (challenge in challenges) {
            if (challenge.completed) continue
            val target = challenge.targetValue.coerceAtLeast(1)
            when (event) {
                is ChallengeEvent.Metric -> {
                    if (challenge.type != event.type || event.value < 0) continue
                    challenge.progress = when (challenge.type.semantics) {
                        ChallengeProgress.ACCUMULATE -> (challenge.progress.toLong().coerceAtLeast(0) + event.value)
                            .coerceAtMost(target.toLong()).toInt()
                        ChallengeProgress.MAXIMUM -> maxOf(challenge.progress, event.value).coerceIn(0, target)
                        ChallengeProgress.LEVEL_CONDITION -> continue
                    }
                }
                is ChallengeEvent.LevelCompleted -> {
                    when (challenge.type) {
                        ChallengeType.PERFECT_LEVEL -> {
                            if (!event.lostLife) challenge.progress = (challenge.progress.toLong() + 1)
                                .coerceIn(0, target.toLong()).toInt()
                        }
                        ChallengeType.TIME_UNDER_LIMIT -> {
                            if (event.durationSeconds.isFinite() && event.durationSeconds >= 0f &&
                                event.durationSeconds <= target.toFloat()) challenge.progress = target
                        }
                        else -> continue
                    }
                }
            }
            if (challenge.progress >= target) {
                challenge.completed = true
                // The persistence transaction acknowledges rewards separately.
                newlyCompleted.add(challenge)
            }
        }
        return newlyCompleted
    }

    fun getChallengeProgressText(challenge: DailyChallenge): String =
        if (challenge.type == ChallengeType.TIME_UNDER_LIMIT) {
            if (challenge.completed) "Completed" else "Clear in ${challenge.targetValue}s or less"
        } else "${challenge.progress}/${challenge.targetValue}"

    fun getChallengeRewardDescription(challenge: DailyChallenge): String {
        return when (challenge.rewardType) {
            RewardType.COSMETIC_UNLOCK -> "Unlocks a cosmetic item"
            RewardType.THEME_UNLOCK -> "Unlocks a visual theme"
            RewardType.STREAK_BONUS -> "Queued: +20 points on ${challenge.rewardValue} bricks in your next scored run"
            RewardType.SCORE_MULTIPLIER -> "Queued: +${challenge.rewardValue}% score in your next scored run"
        }
    }

    fun suggestedModeForChallenge(type: ChallengeType): GameMode {
        return when (type) {
            ChallengeType.TIME_UNDER_LIMIT -> GameMode.RUSH
            ChallengeType.LASER_FIRED -> GameMode.INVADERS
            ChallengeType.SCORE_ACHIEVED -> GameMode.ENDLESS
            ChallengeType.COMBO_MULTIPLIER -> GameMode.CLASSIC
            ChallengeType.POWERUPS_COLLECTED -> GameMode.CLASSIC
            ChallengeType.PERFECT_LEVEL -> GameMode.CLASSIC
            ChallengeType.MULTI_BALL_ACTIVE -> GameMode.CLASSIC
            ChallengeType.BRICKS_DESTROYED -> GameMode.CLASSIC
        }
    }
}
