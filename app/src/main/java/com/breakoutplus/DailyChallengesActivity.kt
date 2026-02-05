package com.breakoutplus

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.breakoutplus.databinding.ActivityDailyChallengesBinding
import com.breakoutplus.databinding.ItemDailyChallengeBinding

class DailyChallengesActivity : FoldAwareActivity() {
    private lateinit var binding: ActivityDailyChallengesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyChallengesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)

        binding.buttonChallengesBack.setOnClickListener { finish() }
        renderChallenges()
        animateEntry()
    }

    private fun renderChallenges() {
        val challenges = DailyChallengeStore.load(this)
        binding.challengesList.removeAllViews()
        val inflater = LayoutInflater.from(this)
        challenges.forEach { challenge ->
            val row = ItemDailyChallengeBinding.inflate(inflater, binding.challengesList, false)
            row.challengeTitle.text = challenge.title
            row.challengeDescription.text = challenge.description
            row.challengeProgressBar.max = challenge.targetValue
            row.challengeProgressBar.progress = challenge.progress.coerceAtMost(challenge.targetValue)
            row.challengeProgress.text = "${challenge.progress}/${challenge.targetValue}"
            row.challengeStatus.text = if (challenge.completed) "Completed" else "In progress"
            val statusColor = if (challenge.completed) R.color.bp_green else R.color.bp_gold
            row.challengeStatus.setTextColor(ContextCompat.getColor(this, statusColor))
            binding.challengesList.addView(row.root)
        }
    }

    private fun animateEntry() {
        val views = listOf(binding.challengesTitle, binding.challengesScroll, binding.challengesFooter)
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
