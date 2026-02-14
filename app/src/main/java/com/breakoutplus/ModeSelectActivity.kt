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
    private var clickEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModeSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)

        binding.buttonModeBack.setOnClickListener {
            finish()
            playCloseTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        renderModes()
        animateEntry()
    }

    override fun onResume() {
        super.onResume()
        clickEnabled = true
    }

    override fun onPause() {
        super.onPause()
        // Cancel all ongoing animations to prevent them from running after activity is paused
        for (i in 0 until binding.modeList.childCount) {
            val child = binding.modeList.getChildAt(i)
            child.animate().cancel()
        }
        binding.modeTitle.animate().cancel()
        binding.modeSubtitle.animate().cancel()
        binding.modeFooter.animate().cancel()
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
            GameMode.INVADERS,
            GameMode.ZEN
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
                GameMode.ZEN -> R.color.bp_gray
            }
            val accentColor = ContextCompat.getColor(this, accentRes)
            (cardBinding.root as? MaterialCardView)?.strokeColor = accentColor
            cardBinding.modeCardTitle.setTextColor(accentColor)
            cardBinding.modeCardAccent.setBackgroundColor(accentColor)
            cardBinding.modeCardStart.backgroundTintList = ColorStateList.valueOf(accentColor)
            cardBinding.modeCardStart.setOnClickListener {
                if (!clickEnabled) return@setOnClickListener
                clickEnabled = false
                startActivity(Intent(this, GameActivity::class.java).putExtra(GameActivity.EXTRA_MODE, mode.name))
                playOpenTransition(R.anim.fade_in, R.anim.fade_out)
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
