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

    override fun onResume() {
        super.onResume()
        // Refresh challenges in case the date changed while the app was in background
        renderChallenges()
    }

    private fun renderChallenges() {
        val challenges = DailyChallengeStore.load(this)
        binding.challengesList.removeAllViews()
        val inflater = LayoutInflater.from(this)
        challenges.forEachIndexed { index, challenge ->
            val row = ItemDailyChallengeBinding.inflate(inflater, binding.challengesList, false)
            row.challengeTitle.text = challenge.title
            row.challengeDescription.text = challenge.description
            row.challengeProgressBar.max = challenge.targetValue
            row.challengeProgressBar.progress = challenge.progress.coerceAtMost(challenge.targetValue)
            row.challengeProgress.text = getString(
                R.string.label_challenge_progress,
                challenge.progress,
                challenge.targetValue
            )
            row.challengeStatus.text = if (challenge.completed) {
                getString(R.string.label_challenge_completed)
            } else {
                getString(R.string.label_challenge_in_progress)
            }
            val statusColor = if (challenge.completed) R.color.bp_green else R.color.bp_gold
            row.challengeStatus.setTextColor(ContextCompat.getColor(this, statusColor))

            // Add entrance animation
            val rowView = row.root
            rowView.alpha = 0f
            rowView.translationY = 30f
            rowView.scaleX = 0.98f
            rowView.scaleY = 0.98f
            rowView.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(UiMotion.LIST_ITEM_DURATION)
                .setStartDelay(UiMotion.stagger(index, step = 52L))
                .setInterpolator(UiMotion.EMPHASIS_OUT)
                .start()

            binding.challengesList.addView(rowView)
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
                .setStartDelay(UiMotion.stagger(index, step = 80L))
                .setDuration(UiMotion.ENTRY_DURATION)
                .setInterpolator(UiMotion.EMPHASIS_OUT)
                .start()
        }
    }
}
