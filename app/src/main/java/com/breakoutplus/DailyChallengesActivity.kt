package com.breakoutplus

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.breakoutplus.databinding.ActivityDailyChallengesBinding
import com.breakoutplus.databinding.ItemDailyChallengeBinding
import com.breakoutplus.game.DailyChallengeManager
import com.breakoutplus.game.GameMode

class DailyChallengesActivity : FoldAwareActivity() {
    private lateinit var binding: ActivityDailyChallengesBinding
    private var clickEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyChallengesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)

        binding.buttonChallengesBack.setOnClickListener {
            finish()
            playCloseTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        registerSlideCloseOnBackPressed()
        UiMotion.attachPressScale(binding.buttonChallengesBack)
        renderChallenges()
        animateEntry()
    }

    override fun onResume() {
        super.onResume()
        clickEnabled = true
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
            row.challengeReward.text = getString(
                R.string.label_challenge_reward,
                DailyChallengeManager.getChallengeRewardDescription(challenge)
            )
            row.challengeProgressBar.max = challenge.targetValue
            row.challengeProgressBar.progress = challenge.progress.coerceAtMost(challenge.targetValue)
            row.challengeProgress.text = getString(
                R.string.label_challenge_progress,
                challenge.progress,
                challenge.targetValue
            )
            val suggestedMode = DailyChallengeManager.suggestedModeForChallenge(challenge.type)
            row.challengePlayHint.text = getString(
                R.string.label_challenge_tap_to_play,
                suggestedMode.displayName
            )
            row.challengeStatus.text = when {
                challenge.completed && challenge.rewardGranted ->
                    getString(R.string.label_challenge_reward_applied)
                challenge.completed -> getString(R.string.label_challenge_completed)
                else -> getString(R.string.label_challenge_in_progress)
            }
            val statusColor = when {
                challenge.completed && challenge.rewardGranted -> R.color.bp_green
                challenge.completed -> R.color.bp_cyan
                else -> R.color.bp_azure
            }
            row.challengeStatus.setTextColor(ContextCompat.getColor(this, statusColor))

            row.root.setOnClickListener {
                if (!clickEnabled) return@setOnClickListener
                clickEnabled = false
                startActivity(
                    Intent(this, GameActivity::class.java)
                        .putExtra(GameActivity.EXTRA_MODE, suggestedMode.name)
                )
                playOpenTransition(R.anim.fade_in, R.anim.fade_out)
            }
            UiMotion.attachPressScale(row.root)

            val rowView = row.root
            UiMotion.animateListItem(
                rowView,
                index,
                offsetY = UiMotion.CARD_ENTRY_OFFSET_Y,
                enterScale = UiMotion.LIST_ENTER_SCALE,
                step = UiMotion.STAGGER_STEP_CHALLENGE_ROW
            )

            binding.challengesList.addView(rowView)
        }
    }

    private fun animateEntry() {
        UiMotion.animateScreenSections(
            listOf(binding.challengesTitle, binding.challengesScroll, binding.challengesFooter)
        )
    }
}
