package com.breakoutplus

import android.content.Context
import com.breakoutplus.game.GameSummary
import kotlin.math.max

object LifetimeStatsManager {
    private const val PREFS_NAME = "breakout_plus_lifetime_stats"
    private const val KEY_TOTAL_BRICKS_BROKEN = "total_bricks_broken"
    private const val KEY_TOTAL_LIVES_LOST = "total_lives_lost"
    private const val KEY_TOTAL_PLAY_SECONDS = "total_play_seconds"
    private const val KEY_LONGEST_RUN_SECONDS = "longest_run_seconds"
    private const val KEY_GAMES_PLAYED = "games_played"
    private const val KEY_HIGHEST_SCORE = "highest_score"
    private const val KEY_TOTAL_SCORE = "total_score"

    data class LifetimeStats(
        val totalBricksBroken: Int,
        val totalLivesLost: Int,
        val totalPlaySeconds: Long,
        val longestRunSeconds: Int,
        val gamesPlayed: Int,
        val highestScore: Int,
        val totalScore: Long
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(context: Context): LifetimeStats {
        val prefs = prefs(context)
        return LifetimeStats(
            totalBricksBroken = prefs.getInt(KEY_TOTAL_BRICKS_BROKEN, 0).coerceAtLeast(0),
            totalLivesLost = prefs.getInt(KEY_TOTAL_LIVES_LOST, 0).coerceAtLeast(0),
            totalPlaySeconds = prefs.getLong(KEY_TOTAL_PLAY_SECONDS, 0L).coerceAtLeast(0L),
            longestRunSeconds = prefs.getInt(KEY_LONGEST_RUN_SECONDS, 0).coerceAtLeast(0),
            gamesPlayed = prefs.getInt(KEY_GAMES_PLAYED, 0).coerceAtLeast(0),
            highestScore = prefs.getInt(KEY_HIGHEST_SCORE, 0).coerceAtLeast(0),
            totalScore = prefs.getLong(KEY_TOTAL_SCORE, 0L).coerceAtLeast(0L)
        )
    }

    fun recordRun(context: Context, summary: GameSummary): LifetimeStats {
        val current = load(context)
        val updated = LifetimeStats(
            totalBricksBroken = (current.totalBricksBroken + summary.bricksBroken).coerceAtLeast(0),
            totalLivesLost = (current.totalLivesLost + summary.livesLost).coerceAtLeast(0),
            totalPlaySeconds = (current.totalPlaySeconds + summary.durationSeconds.toLong()).coerceAtLeast(0L),
            longestRunSeconds = max(current.longestRunSeconds, summary.durationSeconds),
            gamesPlayed = (current.gamesPlayed + 1).coerceAtLeast(1),
            highestScore = max(current.highestScore, summary.score),
            totalScore = (current.totalScore + summary.score.toLong()).coerceAtLeast(0L)
        )
        prefs(context).edit()
            .putInt(KEY_TOTAL_BRICKS_BROKEN, updated.totalBricksBroken)
            .putInt(KEY_TOTAL_LIVES_LOST, updated.totalLivesLost)
            .putLong(KEY_TOTAL_PLAY_SECONDS, updated.totalPlaySeconds)
            .putInt(KEY_LONGEST_RUN_SECONDS, updated.longestRunSeconds)
            .putInt(KEY_GAMES_PLAYED, updated.gamesPlayed)
            .putInt(KEY_HIGHEST_SCORE, updated.highestScore)
            .putLong(KEY_TOTAL_SCORE, updated.totalScore)
            .apply()
        return updated
    }
}
