package com.breakoutplus

import android.content.Context
import com.breakoutplus.game.ChallengeType
import com.breakoutplus.game.DailyChallenge
import com.breakoutplus.game.DailyChallengeManager
import com.breakoutplus.game.RewardType
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DailyChallengeStore {
    private const val PREFS_NAME = "breakout_plus_daily_challenges"
    private const val KEY_DATE = "date_key"
    private const val KEY_CHALLENGES = "challenges"
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    fun load(context: Context): MutableList<DailyChallenge> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = dateFormat.format(Date())
        val storedDate = prefs.getString(KEY_DATE, null)
        val raw = prefs.getString(KEY_CHALLENGES, null)

        if (storedDate != today || raw.isNullOrEmpty()) {
            val fresh = DailyChallengeManager.generateDailyChallenges().toMutableList()
            save(context, fresh, today)
            return fresh
        }

        return runCatching { parseChallenges(raw) }
            .getOrElse {
                val fresh = DailyChallengeManager.generateDailyChallenges().toMutableList()
                save(context, fresh, today)
                fresh
            }
    }

    fun save(context: Context, challenges: List<DailyChallenge>, dateOverride: String? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dateKey = dateOverride ?: dateFormat.format(Date())
        val array = JSONArray()
        challenges.forEach { challenge ->
            val obj = JSONObject()
            obj.put("id", challenge.id)
            obj.put("title", challenge.title)
            obj.put("description", challenge.description)
            obj.put("type", challenge.type.name)
            obj.put("targetValue", challenge.targetValue)
            obj.put("rewardType", challenge.rewardType.name)
            obj.put("rewardValue", challenge.rewardValue)
            obj.put("progress", challenge.progress)
            obj.put("completed", challenge.completed)
            obj.put("rewardGranted", challenge.rewardGranted)
            obj.put("dateGenerated", challenge.dateGenerated)
            array.put(obj)
        }
        prefs.edit()
            .putString(KEY_DATE, dateKey)
            .putString(KEY_CHALLENGES, array.toString())
            .apply()
    }

    private fun parseChallenges(raw: String): MutableList<DailyChallenge> {
        val array = JSONArray(raw)
        val result = mutableListOf<DailyChallenge>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val challenge = DailyChallenge(
                id = obj.optString("id"),
                title = obj.optString("title"),
                description = obj.optString("description"),
                type = ChallengeType.valueOf(obj.optString("type", ChallengeType.BRICKS_DESTROYED.name)),
                targetValue = obj.optInt("targetValue", 1),
                rewardType = RewardType.valueOf(obj.optString("rewardType", RewardType.SCORE_MULTIPLIER.name)),
                rewardValue = obj.optInt("rewardValue", 0),
                progress = obj.optInt("progress", 0),
                completed = obj.optBoolean("completed", false),
                rewardGranted = obj.optBoolean("rewardGranted", false),
                dateGenerated = obj.optLong("dateGenerated", System.currentTimeMillis())
            )
            result.add(challenge)
        }
        return result
    }
}
