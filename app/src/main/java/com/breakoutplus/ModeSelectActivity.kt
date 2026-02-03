package com.breakoutplus

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import com.breakoutplus.databinding.ActivityModeSelectBinding
import com.breakoutplus.databinding.ItemModeCardBinding
import com.breakoutplus.game.GameMode

class ModeSelectActivity : FoldAwareActivity() {
    private lateinit var binding: ActivityModeSelectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModeSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)

        binding.buttonModeBack.setOnClickListener { finish() }

        renderModes()
    }

    private fun renderModes() {
        val inflater = LayoutInflater.from(this)
        val modes = listOf(
            GameMode.CLASSIC,
            GameMode.TIMED,
            GameMode.ENDLESS,
            GameMode.GOD,
            GameMode.RUSH
        )
        modes.forEachIndexed { index, mode ->
            val cardBinding = ItemModeCardBinding.inflate(inflater, binding.modeList, false)
            cardBinding.modeCardTitle.text = mode.displayName
            cardBinding.modeCardDescription.text = mode.description
            cardBinding.modeCardMeta.text = mode.meta
            cardBinding.modeCardStart.setOnClickListener {
                startActivity(Intent(this, GameActivity::class.java).putExtra(GameActivity.EXTRA_MODE, mode.name))
            }

            // Add entrance animation
            val cardView = cardBinding.root
            cardView.alpha = 0f
            cardView.translationY = 30f
            cardView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(100L + index * 80L)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()

            binding.modeList.addView(cardView)
        }
    }
}
