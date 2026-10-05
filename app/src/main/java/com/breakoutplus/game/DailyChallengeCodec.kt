package com.breakoutplus.game

import org.json.JSONArray
import org.json.JSONObject

object DailyChallengeCodec {
    const val VERSION = 2
    fun encode(challenges: List<DailyChallenge>): String = JSONObject().apply {
        put("version", VERSION)
        put("challenges", JSONArray().apply {
            for (c in challenges) put(JSONObject().apply {
                put("id", c.id); put("title", c.title); put("description", c.description)
                put("type", c.type.name); put("targetValue", c.targetValue)
                put("rewardType", c.rewardType.name); put("rewardValue", c.rewardValue)
                put("progress", c.progress); put("completed", c.completed)
                put("rewardGranted", c.rewardGranted); put("dateGenerated", c.dateGenerated)
            })
        })
    }.toString()

    fun decode(raw: String, maxCount: Int = 20): MutableList<DailyChallenge> {
        val legacy = raw.trimStart().startsWith("[")
        val array = if (legacy) JSONArray(raw) else JSONObject(raw).let {
            require(it.getInt("version") == VERSION) { "Unsupported daily schema" }
            it.getJSONArray("challenges")
        }
        require(array.length() in 1..maxCount) { "Invalid daily challenge count" }
        val result = mutableListOf<DailyChallenge>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val type = ChallengeType.valueOf(o.getString("type"))
            val target = o.optInt("targetValue", 1).coerceIn(1, 1000000)
            val complete = o.optBoolean("completed")
            var progress = o.optInt("progress").coerceIn(0, target)
            // Old incomplete combos were accumulated incorrectly; no evidence of a
            // maximum can be recovered. Preserve completed legacy objectives/unlocks.
            if (legacy && type == ChallengeType.COMBO_MULTIPLIER && !complete) progress = 0
            val id = o.getString("id")
            require(id.isNotBlank() && result.none { it.id == id }) { "Invalid daily ID" }
            result.add(DailyChallenge(id, o.optString("title"), o.optString("description"), type, target,
                RewardType.valueOf(o.getString("rewardType")), o.optInt("rewardValue").coerceIn(0, 100),
                if (complete) target else progress, complete || progress >= target,
                o.optBoolean("rewardGranted") && complete, o.optLong("dateGenerated").coerceAtLeast(0)))
        }
        return result
    }
}
