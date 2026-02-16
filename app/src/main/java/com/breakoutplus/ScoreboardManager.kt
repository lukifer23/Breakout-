package com.breakoutplus

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ScoreboardManager {
    private const val PREFS_NAME = "breakout_plus_scores"
    private const val KEY_SCORES = "scores"
    private const val MAX_SCORES_PER_MODE = 10

    data class ScoreEntry(
        val score: Int,
        val mode: String,
        val name: String,
        val level: Int,
        val durationSeconds: Int,
        val timestamp: Long
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val scoreComparator = compareByDescending<ScoreEntry> { it.score }
        .thenBy { normalizedDuration(it.durationSeconds) }
        .thenByDescending { it.timestamp }

    fun loadScores(context: Context): List<ScoreEntry> {
        val raw = prefs(context).getString(KEY_SCORES, "[]") ?: "[]"
        val json = JSONArray(raw)
        return buildList {
            for (i in 0 until json.length()) {
                val obj = json.optJSONObject(i) ?: continue
                val rawName = obj.optString("name", "Player").trim()
                val rawMode = obj.optString("mode").trim()
                add(
                    ScoreEntry(
                        score = obj.optInt("score").coerceAtLeast(0),
                        mode = if (rawMode.isNotBlank()) rawMode else "Classic",
                        name = if (rawName.isNotBlank()) rawName else "Player",
                        level = obj.optInt("level", 1).coerceAtLeast(1),
                        durationSeconds = obj.optInt("duration").coerceAtLeast(0),
                        timestamp = obj.optLong("timestamp").coerceAtLeast(0L)
                    )
                )
            }
        }
    }

    fun getHighScoresForMode(context: Context, mode: String): List<ScoreEntry> {
        return loadScores(context)
            .filter { it.mode == mode }
            .sortedWith(scoreComparator)
            .take(MAX_SCORES_PER_MODE)
    }

    fun getHighScoresAllModes(context: Context): List<ScoreEntry> {
        return loadScores(context)
            .sortedWith(scoreComparator)
            .take(10)
    }

    fun isHighScoreForMode(
        context: Context,
        mode: String,
        score: Int,
        durationSeconds: Int,
        timestamp: Long = System.currentTimeMillis()
    ): Boolean {
        val highScores = getHighScoresForMode(context, mode)
        if (highScores.size < 10) return true
        val worst = highScores.last()
        val candidate = ScoreEntry(
            score = score,
            mode = mode,
            name = "",
            level = 1,
            durationSeconds = durationSeconds.coerceAtLeast(0),
            timestamp = timestamp
        )
        return scoreComparator.compare(candidate, worst) < 0
    }

    fun addHighScore(context: Context, entry: ScoreEntry): List<ScoreEntry> {
        val sanitizedEntry = entry.copy(
            score = entry.score.coerceAtLeast(0),
            mode = entry.mode.trim().ifBlank { "Classic" },
            name = entry.name.trim().ifBlank { "Player" },
            level = entry.level.coerceAtLeast(1),
            durationSeconds = entry.durationSeconds.coerceAtLeast(0),
            timestamp = if (entry.timestamp > 0L) entry.timestamp else System.currentTimeMillis()
        )
        if (!isHighScoreForMode(
                context,
                sanitizedEntry.mode,
                sanitizedEntry.score,
                sanitizedEntry.durationSeconds,
                sanitizedEntry.timestamp
            )
        ) {
            return getHighScoresForMode(context, sanitizedEntry.mode)
        }

        val scores = loadScores(context).toMutableList()
        scores.add(sanitizedEntry)
        val trimmed = trimScoresPerMode(scores)
        saveScores(context, trimmed)
        return getHighScoresForMode(context, sanitizedEntry.mode)
    }

    private fun trimScoresPerMode(scores: List<ScoreEntry>): List<ScoreEntry> {
        return scores.groupBy { it.mode }.flatMap { (_, entries) ->
            entries.sortedWith(scoreComparator)
                .take(MAX_SCORES_PER_MODE)
        }
    }

    fun reset(context: Context) {
        prefs(context).edit().putString(KEY_SCORES, "[]").apply()
    }

    private fun saveScores(context: Context, scores: List<ScoreEntry>) {
        val json = JSONArray()
        scores.forEach { entry ->
            val obj = JSONObject()
            obj.put("score", entry.score)
            obj.put("mode", entry.mode)
            obj.put("name", entry.name)
            obj.put("level", entry.level)
            obj.put("duration", entry.durationSeconds)
            obj.put("timestamp", entry.timestamp)
            json.put(obj)
        }
        prefs(context).edit().putString(KEY_SCORES, json.toString()).apply()
    }

    private fun normalizedDuration(durationSeconds: Int): Int =
        if (durationSeconds <= 0) Int.MAX_VALUE else durationSeconds
}
