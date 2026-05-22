package com.breakoutplus

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.content.res.ColorStateList
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.breakoutplus.databinding.ActivityModeSelectBinding
import com.breakoutplus.databinding.ItemModeCardBinding
import com.breakoutplus.game.GameMode
import com.breakoutplus.game.ModeAccent

class ModeSelectActivity : FoldAwareActivity() {
    private lateinit var binding: ActivityModeSelectBinding
    private var clickEnabled = true
    private var modeRowLayout: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModeSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)

        binding.buttonModeBack.setOnClickListener { finishWithCloseTransition() }
        registerSlideCloseOnBackPressed()

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
            if (child is LinearLayout) {
                for (j in 0 until child.childCount) {
                    child.getChildAt(j).animate().cancel()
                }
            }
        }
        binding.modeTitle.animate().cancel()
        binding.modeSubtitle.animate().cancel()
        binding.modeFooter.animate().cancel()
    }

    private fun isTabletLayout(): Boolean {
        val metrics = resources.displayMetrics
        val widthDp = metrics.widthPixels / metrics.density
        val heightDp = metrics.heightPixels / metrics.density
        return DeviceLayoutPolicy.classifyByDp(widthDp, heightDp).tabletClass
    }

    private fun renderModes() {
        val inflater = LayoutInflater.from(this)
        val tablet = isTabletLayout()
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
            val accentRes = ModeAccent.colorRes(mode)
            val accentColor = ContextCompat.getColor(this, accentRes)
            (cardBinding.root as? MaterialCardView)?.strokeColor = accentColor
            cardBinding.modeCardTitle.setTextColor(accentColor)
            cardBinding.modeCardAccent.setBackgroundColor(accentColor)
            cardBinding.modeCardStart.backgroundTintList = ColorStateList.valueOf(accentColor)
            UiMotion.attachPressScale(cardBinding.modeCardStart)
            cardBinding.modeCardStart.setOnClickListener {
                if (!clickEnabled) return@setOnClickListener
                clickEnabled = false
                startActivity(Intent(this, GameActivity::class.java).putExtra(GameActivity.EXTRA_MODE, mode.name))
                playOpenTransition(R.anim.fade_in, R.anim.fade_out)
            }

            val cardView = cardBinding.root
            UiMotion.animateListItem(
                cardView,
                index,
                offsetY = UiMotion.CARD_ENTRY_OFFSET_Y,
                enterScale = UiMotion.LIST_ENTER_SCALE,
                step = UiMotion.STAGGER_STEP_CARD,
                base = UiMotion.STAGGER_BASE_CARD,
                duration = UiMotion.ENTRY_DURATION
            )

            if (tablet) {
                if (index % 2 == 0) {
                    modeRowLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                    binding.modeList.addView(modeRowLayout)
                }
                val rowParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (index % 2 == 0) marginEnd = (8 * resources.displayMetrics.density).toInt()
                    else marginStart = (8 * resources.displayMetrics.density).toInt()
                    bottomMargin = (16 * resources.displayMetrics.density).toInt()
                }
                cardView.layoutParams = rowParams
                modeRowLayout?.addView(cardView)
            } else {
                binding.modeList.addView(cardView)
            }
        }
    }

    private fun animateEntry() {
        val headers = listOf(binding.modeTitle, binding.modeSubtitle, binding.modeFooter)
        headers.forEachIndexed { index, view ->
            UiMotion.animateFadeUp(
                view = view,
                index = index,
                offsetY = UiMotion.HEADER_ENTRY_OFFSET_Y,
                step = UiMotion.STAGGER_STEP_HEADER
            )
        }
    }

    private fun finishWithCloseTransition() {
        finish()
        playCloseTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
