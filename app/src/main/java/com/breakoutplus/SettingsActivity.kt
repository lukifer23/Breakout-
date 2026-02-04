package com.breakoutplus

import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatDelegate
import com.breakoutplus.databinding.ActivitySettingsBinding

class SettingsActivity : FoldAwareActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)

        binding.buttonSettingsBack.setOnClickListener { finish() }

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
                    showFpsCounter = binding.switchFpsCounter.isChecked
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
        }
        binding.switchFpsCounter.setOnCheckedChangeListener { _, _ -> saveSettings() }
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
                .setTitle("Reset Scores")
                .setMessage("This will permanently delete all saved scores. Are you sure?")
                .setPositiveButton("Reset") { _, _ ->
                    ScoreboardManager.reset(this)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        animateEntry()
    }

    private fun animateEntry() {
        val views = listOf(binding.settingsTitle, binding.settingsScroll, binding.settingsFooter)
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 18f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(80L * index)
                .setDuration(350L)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }

        binding.settingsList.post {
            animateStagger(binding.settingsList)
        }
    }

    private fun animateStagger(container: android.view.ViewGroup) {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            child.alpha = 0f
            child.translationY = 14f
            child.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(60L * i)
                .setDuration(260L)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }
}
