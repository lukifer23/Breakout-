package com.breakoutplus

import android.content.Context
import com.breakoutplus.game.DailyChallenge
import com.breakoutplus.game.RewardLedger
import com.breakoutplus.game.RewardLedgerCodec
import com.breakoutplus.game.RewardBonuses
import com.breakoutplus.game.LevelTheme
import com.breakoutplus.game.LevelThemes

object UnlockManager {
    private const val PREFS_NAME = "breakout_plus_unlocks"
    private const val KEY_UNLOCKED_THEMES = "unlocked_themes"
    private const val KEY_COSMETIC_TIER = "cosmetic_tier"
    private const val MAX_COSMETIC_TIER = 3

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(context: Context): com.breakoutplus.game.GameUnlocks {
        val prefs = prefs(context)
        val rawThemes = prefs.getString(KEY_UNLOCKED_THEMES, "") ?: ""
        val unlockedThemes = rawThemes.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        val cosmeticTier = prefs.getInt(KEY_COSMETIC_TIER, 0).coerceIn(0, MAX_COSMETIC_TIER)
        return com.breakoutplus.game.GameUnlocks(unlockedThemes, cosmeticTier)
    }

    fun save(context: Context, state: com.breakoutplus.game.GameUnlocks) {
        val serialized = state.unlockedThemes.joinToString(",")
        prefs(context).edit()
            .putString(KEY_UNLOCKED_THEMES, serialized)
            .putInt(KEY_COSMETIC_TIER, state.cosmeticTier.coerceIn(0, MAX_COSMETIC_TIER))
            .apply()
    }

    fun unlockCosmetic(context: Context): com.breakoutplus.game.GameUnlocks {
        val current = load(context)
        val nextTier = (current.cosmeticTier + 1).coerceAtMost(MAX_COSMETIC_TIER)
        val updated = current.copy(cosmeticTier = nextTier)
        save(context, updated)
        return updated
    }

    fun setCosmeticTier(context: Context, tier: Int): com.breakoutplus.game.GameUnlocks {
        val current = load(context)
        val updated = current.copy(cosmeticTier = tier.coerceIn(0, MAX_COSMETIC_TIER))
        save(context, updated)
        return updated
    }

    fun unlockTheme(context: Context, themeName: String): com.breakoutplus.game.GameUnlocks {
        val current = load(context)
        val updatedThemes = current.unlockedThemes + themeName
        val updated = current.copy(unlockedThemes = updatedThemes)
        save(context, updated)
        return updated
    }

    fun unlockRandomTheme(context: Context, exclude: Set<String> = emptySet()): LevelTheme? {
        val current = load(context)
        val locked = LevelThemes.bonusThemes()
            .filter { it.name !in current.unlockedThemes && it.name !in exclude }
        if (locked.isEmpty()) return null
        val chosen = locked.random()
        unlockTheme(context, chosen.name)
        return chosen
    }

    private const val KEY_REWARD_LEDGER = "reward_ledger_v1"
    private const val KEY_REWARD_BACKUP = "reward_ledger_backup_v1"

    @Synchronized
    fun loadRewardLedger(context: Context): RewardLedger {
        val state = load(context)
        val raw = prefs(context).getString(KEY_REWARD_LEDGER, null)
        val ledger = if (raw == null) RewardLedger() else runCatching { RewardLedgerCodec.decode(raw) }.getOrElse {
            val recovered = prefs(context).getString(KEY_REWARD_BACKUP, null)?.let { backup ->
                runCatching { RewardLedgerCodec.decode(backup) }.getOrNull()
            }
            prefs(context).edit().putString("corrupt_reward_ledger", raw.take(65536)).commit()
            recovered ?: throw IllegalStateException("Reward ledger cannot be recovered safely; retained for diagnosis", it)
        }
        // Legacy XP/manual unlock APIs remain authoritative for these fields.
        return ledger.copy(themes = state.unlockedThemes, cosmeticTier = state.cosmeticTier)
    }

    @Synchronized
    fun grantDailyRewards(context: Context, challenges: List<DailyChallenge>): com.breakoutplus.game.GameUnlocks {
        var ledger = loadRewardLedger(context)
        for (challenge in challenges) ledger = ledger.grant(challenge, LevelThemes.bonusThemes().map { it.name })
        commitLedger(context, ledger)
        return com.breakoutplus.game.GameUnlocks(ledger.themes, ledger.cosmeticTier)
    }

    @Synchronized
    fun reserveRunRewards(context: Context, runId: String): RewardBonuses {
        val ledger = loadRewardLedger(context).reserve(runId)
        commitLedger(context, ledger)
        return ledger.reservations.getValue(runId)
    }

    private fun commitLedger(context: Context, ledger: RewardLedger) {
        check(prefs(context).edit()
            .putString(KEY_REWARD_LEDGER, RewardLedgerCodec.encode(ledger))
            .putString(KEY_REWARD_BACKUP, RewardLedgerCodec.encode(ledger))
            .putString(KEY_UNLOCKED_THEMES, ledger.themes.sorted().joinToString(","))
            .putInt(KEY_COSMETIC_TIER, ledger.cosmeticTier)
            .commit()) { "Reward transaction could not be persisted" }
    }

    fun resolveThemePool(state: com.breakoutplus.game.GameUnlocks): List<LevelTheme> {
        val base = LevelThemes.baseThemes()
        val extras = LevelThemes.bonusThemes().filter { it.name in state.unlockedThemes }
        return base + extras
    }
}
