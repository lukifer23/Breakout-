package com.breakoutplus

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.breakoutplus.databinding.ActivityScoreboardBinding
import com.breakoutplus.databinding.ItemScoreRowBinding
import com.breakoutplus.game.GameMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.breakoutplus.ProgressionManager

class ScoreboardActivity : FoldAwareActivity() {
    private lateinit var binding: ActivityScoreboardBinding
    private var currentModeIndex = 0
    private val modes = mutableListOf<ModeEntry>()
    private val scoreDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    private data class ModeEntry(val label: String, val mode: GameMode?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScoreboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)

        binding.buttonScoreBack.setOnClickListener {
            finish()
            playCloseTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonScorePrev)?.setOnClickListener { switchMode(-1) }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonScoreNext)?.setOnClickListener { switchMode(1) }

        modes.clear()
        modes.add(ModeEntry(getString(R.string.label_mode_all), null))
        GameMode.values().forEach { mode ->
            modes.add(ModeEntry(mode.displayName, mode))
        }

        updateProgress()
        updateLifetimeStats()
        renderScores()
        animateEntry()
    }

    override fun onResume() {
        super.onResume()
        updateProgress()
        updateLifetimeStats()
        renderScores()
    }

    private fun switchMode(direction: Int) {
        currentModeIndex = (currentModeIndex + direction + modes.size) % modes.size
        renderScores()
    }

    private fun renderScores() {
        val currentMode = modes[currentModeIndex]
        val scores = if (currentMode.mode == null) {
            ScoreboardManager.getHighScoresAllModes(this)
        } else {
            ScoreboardManager.getHighScoresForMode(this, currentMode.mode.displayName)
        }
        binding.scoreList.removeAllViews()

        // Update mode title
        binding.scoreTitle.text = getString(R.string.label_leaderboard_format, currentMode.label)
        binding.buttonScorePrev.isEnabled = modes.size > 1
        binding.buttonScoreNext.isEnabled = modes.size > 1

        if (scores.isEmpty()) {
            binding.scoreEmpty.text = getString(R.string.label_no_scores_mode, currentMode.label)
            binding.scoreEmpty.visibility = android.view.View.VISIBLE
            return
        }
        binding.scoreEmpty.visibility = android.view.View.GONE
        val inflater = LayoutInflater.from(this)
        scores.forEachIndexed { index, entry ->
            val row = ItemScoreRowBinding.inflate(inflater, binding.scoreList, false)
            row.scoreRank.text = getString(R.string.label_rank_format, index + 1)
            row.scorePlayer.text = entry.name.ifBlank { getString(R.string.label_player_default) }
            val timeText = if (entry.durationSeconds > 0) formatDuration(entry.durationSeconds) else "--"
            val modeLabel = entry.mode.ifBlank { getString(R.string.label_mode_classic) }
            val levelLabel = entry.level.coerceAtLeast(1)
            row.scoreModeMeta.text = if (currentMode.mode == null) {
                getString(R.string.label_score_meta_format, modeLabel, levelLabel, timeText)
            } else {
                getString(R.string.label_score_meta_no_mode_format, levelLabel, timeText)
            }
            row.scoreValue.text = String.format(Locale.getDefault(), "%,d", entry.score)
            row.scoreDate.text = if (entry.timestamp > 0L) {
                scoreDateFormat.format(Date(entry.timestamp))
            } else {
                getString(R.string.label_score_date_unknown)
            }
            val rankColor = when (index) {
                0 -> R.color.bp_gold
                1 -> R.color.bp_cyan
                2 -> R.color.bp_magenta
                else -> R.color.bp_gray
            }
            row.scoreRank.setTextColor(ContextCompat.getColor(this, rankColor))
            val card = row.root as? MaterialCardView
            card?.strokeColor = ContextCompat.getColor(this, if (index < 3) rankColor else R.color.bp_line)
            card?.strokeWidth = if (index < 3) dp(1.5f) else dp(1f)
            card?.cardElevation = if (index < 3) dp(4f).toFloat() else dp(2f).toFloat()

            // Add entrance animation
            val rowView = row.root
            rowView.alpha = 0f
            rowView.translationX = -50f
            rowView.scaleX = 0.98f
            rowView.scaleY = 0.98f
            rowView.animate()
                .alpha(1f)
                .translationX(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(UiMotion.LIST_ITEM_DURATION)
                .setStartDelay(UiMotion.stagger(index, step = 52L))
                .setInterpolator(UiMotion.EMPHASIS_OUT)
                .start()

            binding.scoreList.addView(rowView)
        }
    }

    private fun updateProgress() {
        val bestLevel = ProgressionManager.loadBestLevel(this)
        val chapter = ProgressionManager.chapterForLevel(bestLevel)
        val stage = ProgressionManager.stageForLevel(bestLevel)
        val xp = ProgressionManager.loadXp(this)
        val journey = getString(R.string.label_progress_format, chapter, stage, xp)
        val best = getString(R.string.label_best_level_short, bestLevel)
        binding.scoreProgress.text = "$journey • $best"
    }

    private fun updateLifetimeStats() {
        val stats = LifetimeStatsManager.load(this)
        val avgScore = if (stats.gamesPlayed > 0) {
            (stats.totalScore / stats.gamesPlayed.toLong()).toInt()
        } else {
            0
        }
        val playTime = formatPlayTime(stats.totalPlaySeconds)
        val longestRun = formatDuration(stats.longestRunSeconds)
        binding.scoreRunSummary.text = getString(
            R.string.label_lifetime_run_format,
            stats.gamesPlayed,
            stats.highestScore,
            avgScore
        )
        binding.scoreLifetimeStats.text = getString(
            R.string.label_lifetime_stats_format,
            stats.totalBricksBroken,
            stats.totalLivesLost,
            playTime,
            longestRun
        )
    }

    private fun formatDuration(seconds: Int): String {
        val safeSeconds = seconds.coerceAtLeast(0)
        val minutes = safeSeconds / 60
        val remaining = safeSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remaining)
    }

    private fun formatPlayTime(totalSeconds: Long): String {
        if (totalSeconds <= 0L) return "0m"
        val totalMinutes = totalSeconds / 60L
        val days = totalMinutes / (24L * 60L)
        val hours = (totalMinutes / 60L) % 24L
        val minutes = totalMinutes % 60L
        return when {
            days > 0L -> String.format(Locale.getDefault(), "%dd %dh", days, hours)
            hours > 0L -> String.format(Locale.getDefault(), "%dh %02dm", hours, minutes)
            else -> String.format(Locale.getDefault(), "%dm", minutes)
        }
    }

    private fun dp(value: Float): Int {
        if (value <= 0f) return 0
        return (value * resources.displayMetrics.density).toInt().coerceAtLeast(1)
    }

    private fun animateEntry() {
        val views = listOf(binding.scoreTitle, binding.scoreScroll, binding.scoreFooter)
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 18f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(UiMotion.stagger(index, step = 80L))
                .setDuration(UiMotion.ENTRY_DURATION)
                .setInterpolator(UiMotion.EMPHASIS_OUT)
                .start()
        }
    }
}
