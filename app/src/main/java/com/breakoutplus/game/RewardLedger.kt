package com.breakoutplus.game

/** Durable reward effects and run reservations are committed in one storage transaction. */
data class RewardBonuses(val scorePercent: Int = 0, val streakBricks: Int = 0)
data class RewardGrant(val id: String, val type: RewardType, val value: Int)
data class RewardLedger(
    val granted: Set<String> = emptySet(),
    val pending: List<RewardGrant> = emptyList(),
    val reservations: Map<String, RewardBonuses> = emptyMap(),
    val themes: Set<String> = emptySet(),
    val cosmeticTier: Int = 0
) {
    fun grant(challenge: DailyChallenge, availableThemes: List<String>): RewardLedger {
        if (!challenge.completed || challenge.rewardGranted) return this
        val id = "${challenge.dateGenerated}/${challenge.id}"
        if (id in granted) return this
        val next = copy(granted = granted + id)
        return when (challenge.rewardType) {
            RewardType.COSMETIC_UNLOCK -> if (cosmeticTier < 3) next.copy(cosmeticTier = cosmeticTier + 1)
                else next.queue(RewardGrant(id, RewardType.SCORE_MULTIPLIER, 5))
            RewardType.THEME_UNLOCK -> {
                val theme = availableThemes.firstOrNull { it !in themes }
                if (theme != null) next.copy(themes = themes + theme)
                else next.queue(RewardGrant(id, RewardType.SCORE_MULTIPLIER, 5))
            }
            else -> next.queue(RewardGrant(id, challenge.rewardType, challenge.rewardValue.coerceIn(1, 100)))
        }
    }

    private fun queue(grant: RewardGrant) = copy(pending = pending + grant)

    /** Retry returns the same reservation; queued effects cannot be assigned to two runs. */
    fun reserve(runId: String): RewardLedger {
        require(runId.isNotBlank())
        if (runId in reservations) return this
        val score = pending.filter { it.type == RewardType.SCORE_MULTIPLIER }.sumOf { it.value.toLong() }
        val streak = pending.filter { it.type == RewardType.STREAK_BONUS }.sumOf { it.value.toLong() }
        return copy(pending = emptyList(), reservations = reservations +
            (runId to RewardBonuses(score.coerceAtMost(1000).toInt(), streak.coerceAtMost(10000).toInt())))
    }
}
