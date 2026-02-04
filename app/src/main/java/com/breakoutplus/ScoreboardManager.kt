package com.breakoutplus

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ScoreboardManager {
    private const val PREFS_NAME = "breakout_plus_scores"
    private const val KEY_SCORES = "scores"
    private const val MAX_SCORES = 20

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

    fun loadScores(context: Context): List<ScoreEntry> {
        val raw = prefs(context).getString(KEY_SCORES, "[]") ?: "[]"
        val json = JSONArray(raw)
        return buildList {
            for (i in 0 until json.length()) {
                val obj = json.optJSONObject(i) ?: continue
                add(
                    ScoreEntry(
                        score = obj.optInt("score"),
                        mode = obj.optString("mode"),
                        name = obj.optString("name", "Player"), // Default to "Player" for legacy scores
                        level = obj.optInt("level"),
                        durationSeconds = obj.optInt("duration"),
                        timestamp = obj.optLong("timestamp")
                    )
                )
            }
        }
    }

    fun getHighScoresForMode(context: Context, mode: String): List<ScoreEntry> {
        return loadScores(context)
            .filter { it.mode == mode }
            .sortedWith(compareByDescending<ScoreEntry> { it.score }.thenBy { it.durationSeconds })
            .take(10) // Top 10 per mode
    }

    fun isHighScoreForMode(context: Context, mode: String, score: Int): Boolean {
        val highScores = getHighScoresForMode(context, mode)
        return highScores.isEmpty() || score > highScores.last().score
    }

    fun addHighScore(context: Context, entry: ScoreEntry): List<ScoreEntry> {
        if (!isHighScoreForMode(context, entry.mode, entry.score)) {
            return getHighScoresForMode(context, entry.mode) // Return existing high scores unchanged
        }

        val scores = loadScores(context).toMutableList()
        scores.add(entry)
        val sorted = scores.sortedWith(compareByDescending<ScoreEntry> { it.score }.thenBy { it.durationSeconds })
        val trimmed = sorted.take(MAX_SCORES)
        saveScores(context, trimmed)
        return getHighScoresForMode(context, entry.mode)
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
}
