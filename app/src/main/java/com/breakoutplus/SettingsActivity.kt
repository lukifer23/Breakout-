package com.breakoutplus

import android.os.Bundle
import android.widget.SeekBar
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

        val saveSettings = {
            SettingsManager.save(
                this,
                SettingsManager.Settings(
                    soundEnabled = binding.switchSound.isChecked,
                    musicEnabled = binding.switchMusic.isChecked,
                    vibrationEnabled = binding.switchVibration.isChecked,
                    tipsEnabled = binding.switchTips.isChecked,
                    leftHanded = binding.switchLeftHanded.isChecked,
                    sensitivity = binding.seekSensitivity.progress / 100f
                )
            )
        }

        binding.switchSound.setOnCheckedChangeListener { _, _ -> saveSettings() }
        binding.switchMusic.setOnCheckedChangeListener { _, _ -> saveSettings() }
        binding.switchVibration.setOnCheckedChangeListener { _, _ -> saveSettings() }
        binding.switchTips.setOnCheckedChangeListener { _, _ -> saveSettings() }
        binding.switchLeftHanded.setOnCheckedChangeListener { _, _ -> saveSettings() }
        binding.seekSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) saveSettings()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        binding.buttonResetScores.setOnClickListener {
            ScoreboardManager.reset(this)
        }
    }
}
