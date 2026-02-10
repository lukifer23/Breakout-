package com.breakoutplus

import android.content.Intent
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.children
import com.breakoutplus.databinding.ActivityMainBinding

class MainActivity : FoldAwareActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)

        binding.buttonPlay.setOnClickListener {
            startActivity(Intent(this, ModeSelectActivity::class.java))
        }
        binding.buttonChallenges.setOnClickListener {
            startActivity(Intent(this, DailyChallengesActivity::class.java))
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
            .setDuration(UiMotion.TITLE_DURATION)
            .setInterpolator(UiMotion.EMPHASIS_IN_OUT)
            .start()

        binding.titleSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(UiMotion.stagger(1, base = 90L, step = 90L))
            .setDuration(UiMotion.SUBTITLE_DURATION)
            .setInterpolator(UiMotion.EMPHASIS_IN_OUT)
            .start()

        binding.mainButtonColumn.children.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 20f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(UiMotion.stagger(index, base = 180L, step = 78L))
                .setDuration(UiMotion.ENTRY_DURATION)
                .setInterpolator(UiMotion.EMPHASIS_OUT)
                .start()
        }
    }
}
