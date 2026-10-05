package com.breakoutplus

import android.content.Context
import com.breakoutplus.game.DailyChallenge
import com.breakoutplus.game.DailyChallengeCodec
import com.breakoutplus.game.DailyChallengeManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DailyChallengeStore {
    private const val PREFS_NAME = "breakout_plus_daily_challenges"
    private const val KEY_DATE = "date_key"
    private const val KEY_CHALLENGES = "challenges"
    private const val KEY_OUTBOX = "reward_outbox"
    private val dateFormat = DateTimeFormatter.BASIC_ISO_DATE

    @Synchronized
    fun load(context: Context, date: LocalDate = LocalDate.now()): MutableList<DailyChallenge> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = date.format(dateFormat)
        prefs.getString(KEY_OUTBOX, null)?.let { rawOutbox ->
            val outbox = DailyChallengeCodec.decode(rawOutbox, 1024)
            UnlockManager.grantDailyRewards(context, outbox)
            check(prefs.edit().remove(KEY_OUTBOX).commit()) { "Reward outbox acknowledgement failed" }
        }
        val raw = prefs.getString(KEY_CHALLENGES, null)
        val stored = raw?.let { runCatching { DailyChallengeCodec.decode(it) }.getOrNull() }
        // Replay a committed completion after crash, including one from yesterday.
        if (stored != null && stored.any { it.completed && !it.rewardGranted }) {
            persistProgressAndRewards(context, stored, prefs.getString(KEY_DATE, today) ?: today)
        }
        if (prefs.getString(KEY_DATE, null) == today && stored != null) return stored
        if (raw != null && stored == null) prefs.edit().putString("corrupt_challenges", raw.take(65536)).apply()
        val fresh = DailyChallengeManager.generateDailyChallenges(date).toMutableList()
        save(context, fresh, today)
        return fresh
    }

    @Synchronized
    fun save(context: Context, challenges: List<DailyChallenge>, dateOverride: String? = null) {
        val dateKey = dateOverride ?: LocalDate.now().format(dateFormat)
        check(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_DATE, dateKey)
            .putString(KEY_CHALLENGES, DailyChallengeCodec.encode(challenges)).commit()) {
            "Daily progress could not be persisted"
        }
    }

    @Synchronized
    fun persistProgressAndRewards(context: Context, snapshot: List<DailyChallenge>, dateKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingDate = prefs.getString(KEY_DATE, null)
        // Older active runs must not replace a newly generated day's objectives.
        val ownsDailySet = existingDate == null || existingDate <= dateKey
        if (ownsDailySet) save(context, snapshot, dateKey)
        val pending = snapshot.filter { it.completed && !it.rewardGranted }
        val previous = prefs.getString(KEY_OUTBOX, null)?.let { DailyChallengeCodec.decode(it, 1024) }.orEmpty()
        val outbox = (previous + pending).distinctBy { "${it.dateGenerated}/${it.id}" }
        if (outbox.isEmpty()) return
        check(prefs.edit().putString(KEY_OUTBOX, DailyChallengeCodec.encode(outbox)).commit()) {
            "Pending rewards could not be persisted"
        }
        // Effect and receipt commit atomically. Retry after a crash is idempotent.
        UnlockManager.grantDailyRewards(context, outbox)
        pending.forEach { it.rewardGranted = true }
        if (ownsDailySet) save(context, snapshot, dateKey)
        check(prefs.edit().remove(KEY_OUTBOX).commit()) { "Reward acknowledgement could not be persisted" }
    }
}
