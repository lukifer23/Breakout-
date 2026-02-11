package com.breakoutplus

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
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
        animateEntry()
    }

    private fun renderModes() {
        val inflater = LayoutInflater.from(this)
        val modes = listOf(
            GameMode.CLASSIC,
            GameMode.TIMED,
            GameMode.ENDLESS,
            GameMode.GOD,
            GameMode.RUSH,
            GameMode.VOLLEY,
            GameMode.TUNNEL,
            GameMode.SURVIVAL,
            GameMode.INVADERS
        )
        modes.forEachIndexed { index, mode ->
            val cardBinding = ItemModeCardBinding.inflate(inflater, binding.modeList, false)
            cardBinding.modeCardTitle.text = mode.displayName
            cardBinding.modeCardDescription.text = mode.description
            cardBinding.modeCardMeta.text = mode.meta
            val accentRes = when (mode) {
                GameMode.CLASSIC -> R.color.bp_cyan
                GameMode.TIMED -> R.color.bp_gold
                GameMode.ENDLESS -> R.color.bp_green
                GameMode.GOD -> R.color.bp_magenta
                GameMode.RUSH -> R.color.bp_red
                GameMode.VOLLEY -> R.color.bp_azure
                GameMode.TUNNEL -> R.color.bp_gold
                GameMode.SURVIVAL -> R.color.bp_orange
                GameMode.INVADERS -> R.color.bp_violet
            }
            val accentColor = ContextCompat.getColor(this, accentRes)
            (cardBinding.root as? MaterialCardView)?.strokeColor = accentColor
            cardBinding.modeCardTitle.setTextColor(accentColor)
            cardBinding.modeCardAccent.setBackgroundColor(accentColor)
            cardBinding.modeCardStart.backgroundTintList = ColorStateList.valueOf(accentColor)
            cardBinding.modeCardStart.setOnClickListener {
                startActivity(Intent(this, GameActivity::class.java).putExtra(GameActivity.EXTRA_MODE, mode.name))
            }

            // Add entrance animation
            val cardView = cardBinding.root
            cardView.alpha = 0f
            cardView.translationY = 30f
            cardView.scaleX = 0.98f
            cardView.scaleY = 0.98f
            cardView.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(UiMotion.ENTRY_DURATION)
                .setStartDelay(UiMotion.stagger(index, base = 90L, step = 70L))
                .setInterpolator(UiMotion.EMPHASIS_OUT)
                .start()

            binding.modeList.addView(cardView)
        }
    }

    private fun animateEntry() {
        val views = listOf(binding.modeTitle, binding.modeFooter)
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 16f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(UiMotion.stagger(index, step = 60L))
                .setDuration(UiMotion.ENTRY_DURATION)
                .setInterpolator(UiMotion.EMPHASIS_OUT)
                .start()
        }
    }
}
