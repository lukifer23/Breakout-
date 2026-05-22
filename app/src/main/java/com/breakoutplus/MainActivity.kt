package com.breakoutplus

import android.content.Intent
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.children
import com.breakoutplus.databinding.ActivityMainBinding

class MainActivity : FoldAwareActivity() {
    private lateinit var binding: ActivityMainBinding
    private var clickEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)

        binding.buttonPlay.setOnClickListener {
            if (!clickEnabled) return@setOnClickListener
            clickEnabled = false
            startActivity(Intent(this, ModeSelectActivity::class.java))
            playOpenTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        binding.buttonChallenges.setOnClickListener {
            if (!clickEnabled) return@setOnClickListener
            clickEnabled = false
            startActivity(Intent(this, DailyChallengesActivity::class.java))
            playOpenTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        binding.buttonScoreboard.setOnClickListener {
            if (!clickEnabled) return@setOnClickListener
            clickEnabled = false
            startActivity(Intent(this, ScoreboardActivity::class.java))
            playOpenTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        binding.buttonSettings.setOnClickListener {
            if (!clickEnabled) return@setOnClickListener
            clickEnabled = false
            startActivity(Intent(this, SettingsActivity::class.java))
            playOpenTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        binding.buttonHowTo.setOnClickListener {
            if (!clickEnabled) return@setOnClickListener
            clickEnabled = false
            startActivity(Intent(this, HowToActivity::class.java))
            playOpenTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        animateIntro()
        binding.mainButtonColumn.children.forEach { UiMotion.attachPressScale(it) }
    }

    override fun onResume() {
        super.onResume()
        clickEnabled = true
    }

    private fun animateIntro() {
        binding.titleText.translationY = UiMotion.TITLE_ENTRY_OFFSET_Y
        binding.titleSubtitle.translationY = UiMotion.TITLE_ENTRY_OFFSET_Y
        UiMotion.animateFadeUp(
            binding.titleText,
            duration = UiMotion.TITLE_DURATION,
            interpolator = UiMotion.EMPHASIS_IN_OUT
        )
        UiMotion.animateFadeUp(
            binding.titleSubtitle,
            index = 1,
            offsetY = UiMotion.TITLE_ENTRY_OFFSET_Y,
            base = UiMotion.MAIN_TITLE_STAGGER_BASE,
            step = UiMotion.MAIN_TITLE_STAGGER_BASE,
            duration = UiMotion.SUBTITLE_DURATION,
            interpolator = UiMotion.EMPHASIS_IN_OUT
        )

        binding.mainButtonColumn.children.forEachIndexed { index, view ->
            UiMotion.animateFadeUp(
                view,
                index = index,
                offsetY = UiMotion.BUTTON_ENTRY_OFFSET_Y,
                base = UiMotion.MAIN_BUTTON_STAGGER_BASE,
                step = UiMotion.MAIN_BUTTON_STAGGER_STEP
            )
        }
    }
}
