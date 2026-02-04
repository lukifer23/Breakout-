package com.breakoutplus

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.breakoutplus.databinding.ActivityScoreboardBinding
import com.breakoutplus.databinding.ItemScoreRowBinding
import com.breakoutplus.game.GameMode

class ScoreboardActivity : FoldAwareActivity() {
    private lateinit var binding: ActivityScoreboardBinding
    private var currentModeIndex = 0
    private val modes = GameMode.values()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScoreboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)

        binding.buttonScoreBack.setOnClickListener { finish() }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonScorePrev)?.setOnClickListener { switchMode(-1) }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonScoreNext)?.setOnClickListener { switchMode(1) }

        renderScores()
        animateEntry()
    }

    private fun switchMode(direction: Int) {
        currentModeIndex = (currentModeIndex + direction + modes.size) % modes.size
        renderScores()
    }

    private fun renderScores() {
        val currentMode = modes[currentModeIndex]
        val scores = ScoreboardManager.getHighScoresForMode(this, currentMode.displayName)
        binding.scoreList.removeAllViews()

        // Update mode title
        binding.scoreTitle.text = "${currentMode.displayName} Leaderboard"

        if (scores.isEmpty()) {
            binding.scoreEmpty.text = "No high scores yet for ${currentMode.displayName}.\nBe the first!"
            binding.scoreEmpty.visibility = android.view.View.VISIBLE
            return
        }
        binding.scoreEmpty.visibility = android.view.View.GONE
        val inflater = LayoutInflater.from(this)
        scores.forEachIndexed { index, entry ->
            val row = ItemScoreRowBinding.inflate(inflater, binding.scoreList, false)
            row.scoreRank.text = "#${index + 1}"
            row.scoreMode.text = entry.name // Show player name instead of mode
            val timeText = if (entry.durationSeconds > 0) formatDuration(entry.durationSeconds) else "--"
            row.scoreMeta.text = "Level ${entry.level} • $timeText"
            row.scoreValue.text = "%d".format(entry.score)
            val rankColor = when (index) {
                0 -> R.color.bp_gold
                1 -> R.color.bp_cyan
                2 -> R.color.bp_magenta
                else -> R.color.bp_gray
            }
            row.scoreRank.setTextColor(ContextCompat.getColor(this, rankColor))

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
                .setDuration(300)
                .setStartDelay(index * 50L)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()

            binding.scoreList.addView(rowView)
        }
    }

    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remaining = seconds % 60
        return String.format("%02d:%02d", minutes, remaining)
    }

    private fun animateEntry() {
        val views = listOf(binding.scoreTitle, binding.scoreScroll, binding.scoreFooter)
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 18f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(80L * index)
                .setDuration(350L)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }
}
