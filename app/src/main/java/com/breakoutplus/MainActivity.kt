package com.breakoutplus

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.children
import com.breakoutplus.databinding.ActivityMainBinding
import com.breakoutplus.game.GameMode

class MainActivity : FoldAwareActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)

        binding.buttonPlay.setOnClickListener {
            startActivity(Intent(this, ModeSelectActivity::class.java))
        }
        binding.buttonScoreboard.setOnClickListener {
            startActivity(Intent(this, ScoreboardActivity::class.java))
        }
        binding.buttonSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.buttonHowTo.setOnClickListener {
            startActivity(Intent(this, HowToActivity::class.java))
        }

        animateIntro()
    }

    private fun animateIntro() {
        binding.titleText.alpha = 0f
        binding.titleSubtitle.alpha = 0f
        binding.titleText.translationY = -20f
        binding.titleSubtitle.translationY = -20f

        binding.titleText.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        binding.titleSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(120)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        binding.mainButtonColumn.children.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 20f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(200L + index * 90L)
                .setDuration(400)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }
}
