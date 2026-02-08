package com.breakoutplus.game

import java.util.*

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

enum class ChallengeType {
    BRICKS_DESTROYED,
    SCORE_ACHIEVED,
    COMBO_MULTIPLIER,
    POWERUPS_COLLECTED,
    PERFECT_LEVEL,
    TIME_UNDER_LIMIT,
    MULTI_BALL_ACTIVE,
    LASER_FIRED
}

enum class RewardType {
    COSMETIC_UNLOCK,
    THEME_UNLOCK,
    STREAK_BONUS,
    SCORE_MULTIPLIER
}

object DailyChallengeManager {

    private const val PREFS_CHALLENGES = "daily_challenges"
    private const val KEY_LAST_UPDATE = "last_update"
    private const val KEY_CHALLENGES = "challenges"

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

    fun generateDailyChallenges(): List<DailyChallenge> {
        val random = Random(System.currentTimeMillis())
        val shuffled = challengeTemplates.shuffled(random)
        return shuffled.take(3).map { it() } // Generate 3 random challenges per day
    }

    fun updateChallengeProgress(challenges: MutableList<DailyChallenge>, type: ChallengeType, value: Int = 1): List<DailyChallenge> {
        val completed = mutableListOf<DailyChallenge>()
        challenges.forEach { challenge ->
            if (!challenge.completed && challenge.type == type) {
                challenge.progress += value
                if (challenge.progress >= challenge.targetValue) {
                    challenge.completed = true
                    challenge.rewardGranted = true
                    completed.add(challenge)
                }
            }
        }
        return completed
    }

    fun completeChallenge(challenge: DailyChallenge) {
        if (!challenge.completed) {
            challenge.progress = challenge.targetValue
            challenge.completed = true
            challenge.rewardGranted = true
        }
    }

    fun getChallengeProgressText(challenge: DailyChallenge): String {
        return "${challenge.progress}/${challenge.targetValue}"
    }

    fun getChallengeRewardDescription(challenge: DailyChallenge): String {
        return when (challenge.rewardType) {
            RewardType.COSMETIC_UNLOCK -> "Unlocks a cosmetic item"
            RewardType.THEME_UNLOCK -> "Unlocks a visual theme"
            RewardType.STREAK_BONUS -> "Bonus points for next ${challenge.rewardValue} bricks"
            RewardType.SCORE_MULTIPLIER -> "+${challenge.rewardValue}% score bonus"
        }
    }
}
