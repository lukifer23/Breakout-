package com.breakoutplus

import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import android.util.TypedValue
import androidx.appcompat.app.AppCompatDelegate
import android.content.Intent
import com.breakoutplus.databinding.ActivitySettingsBinding
import com.breakoutplus.game.LevelThemes

class SettingsActivity : FoldAwareActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)

        binding.buttonSettingsBack.setOnClickListener {
            finish()
            playCloseTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        registerSlideCloseOnBackPressed()
        val settings = SettingsManager.load(this)
        binding.switchSound.isChecked = settings.soundEnabled
        binding.switchMusic.isChecked = settings.musicEnabled
        binding.switchVibration.isChecked = settings.vibrationEnabled
        binding.switchTips.isChecked = settings.tipsEnabled
        binding.switchLeftHanded.isChecked = settings.leftHanded
        binding.seekSensitivity.progress = (settings.sensitivity * 100).toInt()
        binding.seekMasterVolume.progress = (settings.masterVolume * 100).toInt()
        binding.seekEffectsVolume.progress = (settings.effectsVolume * 100).toInt()
        binding.seekMusicVolume.progress = (settings.musicVolume * 100).toInt()
        binding.switchLogging.isChecked = settings.loggingEnabled
        binding.switchDarkMode.isChecked = settings.darkMode
        binding.switchFpsCounter.isChecked = settings.showFpsCounter
        binding.switchHighRefresh.isChecked = settings.highRefreshRate

        val saveSettings = {
            SettingsManager.save(
                this,
                SettingsManager.Settings(
                    soundEnabled = binding.switchSound.isChecked,
                    musicEnabled = binding.switchMusic.isChecked,
                    vibrationEnabled = binding.switchVibration.isChecked,
                    tipsEnabled = binding.switchTips.isChecked,
                    leftHanded = binding.switchLeftHanded.isChecked,
                    sensitivity = binding.seekSensitivity.progress / 100f,
                    masterVolume = binding.seekMasterVolume.progress / 100f,
                    effectsVolume = binding.seekEffectsVolume.progress / 100f,
                    musicVolume = binding.seekMusicVolume.progress / 100f,
                    loggingEnabled = binding.switchLogging.isChecked,
                    darkMode = binding.switchDarkMode.isChecked,
                    showFpsCounter = binding.switchFpsCounter.isChecked,
                    highRefreshRate = binding.switchHighRefresh.isChecked
                )
            )
        }

        binding.switchSound.setOnCheckedChangeListener { _, _ -> saveSettings() }
        binding.switchMusic.setOnCheckedChangeListener { _, _ -> saveSettings() }
        binding.switchVibration.setOnCheckedChangeListener { _, _ -> saveSettings() }
        binding.switchTips.setOnCheckedChangeListener { _, _ -> saveSettings() }
        binding.switchLeftHanded.setOnCheckedChangeListener { _, _ -> saveSettings() }
        binding.switchLogging.setOnCheckedChangeListener { _, _ -> saveSettings() }
        binding.switchDarkMode.setOnCheckedChangeListener { _, enabled ->
            saveSettings()
            AppCompatDelegate.setDefaultNightMode(
                if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            recreate()
        }
        binding.switchFpsCounter.setOnCheckedChangeListener { _, _ -> saveSettings() }
        binding.switchHighRefresh.setOnCheckedChangeListener { _, _ -> saveSettings() }
        binding.seekSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) saveSettings()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        binding.seekMasterVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) saveSettings()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        binding.seekEffectsVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) saveSettings()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        binding.seekMusicVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) saveSettings()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        binding.buttonResetScores.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.label_reset_scores_title))
                .setMessage(getString(R.string.label_reset_scores_message))
                .setPositiveButton(getString(R.string.label_reset_scores_confirm)) { _, _ ->
                    ScoreboardManager.reset(this)
                }
                .setNegativeButton(getString(R.string.label_back), null)
                .show()
        }

        binding.buttonPrivacyPolicy.setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
            playOpenTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        UiMotion.attachPressScale(binding.buttonSettingsBack)
        UiMotion.attachPressScale(binding.buttonResetScores)
        UiMotion.attachPressScale(binding.buttonPrivacyPolicy)

        renderUnlockGallery()
        animateEntry()
    }

    private fun renderUnlockGallery() {
        val unlockState = UnlockManager.load(this)
        val section = TextView(this).apply {
            setTextAppearance(R.style.Text_BreakoutPlus_Caption)
            text = getString(R.string.label_section_unlocks)
            setPadding(0, dp(18), 0, dp(6))
        }
        binding.settingsList.addView(section)
        LevelThemes.bonusThemes().forEach { theme ->
            val unlocked = theme.name in unlockState.unlockedThemes
            val row = TextView(this).apply {
                text = "${theme.name} — " + getString(
                    if (unlocked) R.string.label_unlock_status_unlocked else R.string.label_unlock_status_locked
                )
                setTextColor(getColor(if (unlocked) R.color.bp_green else R.color.bp_gray))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.bp_body_text_size))
                setPadding(0, dp(4), 0, dp(4))
            }
            binding.settingsList.addView(row)
        }
        val cosmetic = TextView(this).apply {
            text = getString(R.string.label_cosmetic_tier_format, unlockState.cosmeticTier)
            setTextColor(getColor(R.color.bp_white))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.bp_body_text_size))
            setPadding(0, dp(8), 0, dp(4))
        }
        binding.settingsList.addView(cosmetic)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun animateEntry() {
        UiMotion.animateScreenSections(
            listOf(binding.settingsTitle, binding.settingsScroll, binding.settingsFooter)
        )
        binding.settingsList.post {
            UiMotion.animateStaggerChildren(binding.settingsList)
        }
    }
}
