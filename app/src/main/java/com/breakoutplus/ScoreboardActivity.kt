package com.breakoutplus

import android.os.Bundle
import android.view.LayoutInflater
import com.breakoutplus.databinding.ActivityScoreboardBinding
import com.breakoutplus.databinding.ItemScoreRowBinding

class ScoreboardActivity : FoldAwareActivity() {
    private lateinit var binding: ActivityScoreboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScoreboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)

        binding.buttonScoreBack.setOnClickListener { finish() }
        renderScores()
    }

    private fun renderScores() {
        val scores = ScoreboardManager.loadScores(this)
        binding.scoreList.removeAllViews()
        if (scores.isEmpty()) {
            binding.scoreEmpty.visibility = android.view.View.VISIBLE
            return
        }
        binding.scoreEmpty.visibility = android.view.View.GONE
        val inflater = LayoutInflater.from(this)
        scores.forEachIndexed { index, entry ->
            val row = ItemScoreRowBinding.inflate(inflater, binding.scoreList, false)
            row.scoreRank.text = "#${index + 1}"
            row.scoreMode.text = entry.mode
            val timeText = if (entry.durationSeconds > 0) formatDuration(entry.durationSeconds) else "--"
            row.scoreMeta.text = "Level ${entry.level} • $timeText"
            row.scoreValue.text = "%d".format(entry.score)

            // Add entrance animation
            val rowView = row.root
            rowView.alpha = 0f
            rowView.translationX = -50f
            rowView.animate()
                .alpha(1f)
                .translationX(0f)
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
}
