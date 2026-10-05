package com.breakoutplus.game

import org.json.JSONArray
import org.json.JSONObject

object RewardLedgerCodec {
    const val VERSION = 1
    fun encode(ledger: RewardLedger): String = JSONObject().apply {
        put("version", VERSION)
        put("granted", JSONArray(ledger.granted.sorted()))
        put("themes", JSONArray(ledger.themes.sorted()))
        put("cosmeticTier", ledger.cosmeticTier)
        put("pending", JSONArray().apply {
            ledger.pending.forEach { grant -> put(JSONObject().apply {
                put("id", grant.id); put("type", grant.type.name); put("value", grant.value)
            }) }
        })
        put("reservations", JSONObject().apply {
            ledger.reservations.forEach { (id, bonus) -> put(id, JSONObject().apply {
                put("scorePercent", bonus.scorePercent); put("streakBricks", bonus.streakBricks)
            }) }
        })
    }.toString()

    fun decode(raw: String): RewardLedger {
        val root = JSONObject(raw)
        require(root.getInt("version") == VERSION) { "Unsupported reward ledger version" }
        fun strings(key: String): Set<String> {
            val a = root.optJSONArray(key) ?: JSONArray()
            return (0 until a.length()).map { a.getString(it) }.filter { it.isNotBlank() }.toSet()
        }
        val pending = root.optJSONArray("pending") ?: JSONArray()
        val grants = (0 until pending.length()).map { i ->
            val g = pending.getJSONObject(i)
            RewardGrant(g.getString("id"), RewardType.valueOf(g.getString("type")), g.getInt("value").coerceIn(1, 100))
        }.distinctBy { it.id }
        val reserved = root.optJSONObject("reservations") ?: JSONObject()
        val reservations = reserved.keys().asSequence().associateWith { id ->
            val b = reserved.getJSONObject(id)
            RewardBonuses(b.optInt("scorePercent").coerceIn(0, 1000), b.optInt("streakBricks").coerceIn(0, 10000))
        }
        return RewardLedger(strings("granted"), grants, reservations, strings("themes"), root.optInt("cosmeticTier").coerceIn(0, 3))
    }
}
