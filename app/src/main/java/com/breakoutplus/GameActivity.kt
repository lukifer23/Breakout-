package com.breakoutplus

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import com.breakoutplus.databinding.ActivityGameBinding
import com.breakoutplus.game.GameConfig
import com.breakoutplus.game.GameEventListener
import com.breakoutplus.game.GameMode
import com.breakoutplus.game.GameSummary
import com.breakoutplus.game.PowerUpType
import com.breakoutplus.game.PowerupStatus

class GameActivity : FoldAwareActivity(), GameEventListener {
    private lateinit var binding: ActivityGameBinding
    private lateinit var config: GameConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)

        val modeName = intent.getStringExtra(EXTRA_MODE)
        val mode = runCatching { GameMode.valueOf(modeName ?: "CLASSIC") }.getOrDefault(GameMode.CLASSIC)
        val settings = SettingsManager.load(this)
        config = GameConfig(mode, settings)

        binding.gameSurface.start(config, this)
        applyHandedness(settings.leftHanded)

        binding.buttonPause.setOnClickListener { showPause(true) }
        binding.buttonResume.setOnClickListener { showPause(false) }
        binding.buttonRestart.setOnClickListener { restartGame() }
        binding.buttonExit.setOnClickListener { exitToMenu() }
        binding.buttonEndSecondary.setOnClickListener { exitToMenu() }
        binding.buttonEndPrimary.setOnClickListener { handleEndPrimary() }
        binding.buttonTooltipDismiss.setOnClickListener { hideTooltip() }

        // Show first-run tooltip if tips enabled
        if (settings.tipsEnabled) {
            showTooltip()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSettings()
        binding.gameSurface.onResume()
        if (binding.pauseOverlay.visibility != View.VISIBLE) {
            binding.gameSurface.resumeGame()
        }
    }

    override fun onPause() {
        binding.gameSurface.pauseGame()
        binding.gameSurface.onPause()
        super.onPause()
    }

    private fun refreshSettings() {
        val settings = SettingsManager.load(this)
        config = GameConfig(config.mode, settings)
        binding.gameSurface.applySettings(settings)
        applyHandedness(settings.leftHanded)
        if (!settings.tipsEnabled) {
            binding.hudTip.visibility = View.GONE
            hideTooltip()
        }
    }

    private fun showPause(show: Boolean) {
        if (show) {
            showOverlay(binding.pauseOverlay)
            binding.gameSurface.pauseGame()
        } else {
            hideOverlay(binding.pauseOverlay)
            binding.gameSurface.resumeGame()
        }
    }

    private fun restartGame() {
        hideOverlay(binding.endOverlay)
        hideOverlay(binding.pauseOverlay)
        binding.gameSurface.restartGame()
    }

    private fun exitToMenu() {
        finish()
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun handleEndPrimary() {
        when (binding.endTitle.text.toString()) {
            getString(R.string.label_level_complete) -> {
                hideOverlay(binding.endOverlay)
                // Add brief celebration delay before next level
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    binding.gameSurface.nextLevel()
                }, 800)
            }
            else -> restartGame()
        }
    }

    private fun applyHandedness(leftHanded: Boolean) {
        val params = binding.buttonPause.layoutParams as ConstraintLayout.LayoutParams
        val isCentered =
            params.startToStart == ConstraintLayout.LayoutParams.PARENT_ID &&
                params.endToEnd == ConstraintLayout.LayoutParams.PARENT_ID
        if (isCentered) return

        if (leftHanded) {
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET
        } else {
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            params.startToStart = ConstraintLayout.LayoutParams.UNSET
        }
        binding.buttonPause.layoutParams = params
    }

    override fun onScoreUpdated(score: Int) {
        runOnUiThread { binding.hudScore.text = "Score $score" }
    }

    override fun onLivesUpdated(lives: Int) {
        runOnUiThread { binding.hudLives.text = "Lives $lives" }
    }

    override fun onTimeUpdated(secondsRemaining: Int) {
        runOnUiThread {
            val minutes = secondsRemaining / 60
            val seconds = secondsRemaining % 60
            val isCountdown = config.mode.timeLimitSeconds > 0
            val label = if (isCountdown) "Time" else "Elapsed"
            binding.hudTime.visibility = android.view.View.VISIBLE
            binding.hudTime.text = "$label ${String.format("%02d:%02d", minutes, seconds)}"
            if (isCountdown && secondsRemaining <= 10) {
                binding.hudTime.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.bp_red))
            } else {
                binding.hudTime.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.bp_white))
            }
        }
    }

    override fun onLevelUpdated(level: Int) {
        runOnUiThread { binding.hudLevel.text = "Level $level" }
    }

    override fun onModeUpdated(mode: GameMode) {
        runOnUiThread { binding.hudMode.text = mode.displayName }
    }

    override fun onPowerupStatus(status: String) {
        runOnUiThread {
            if (binding.hudPowerupChips.visibility != View.VISIBLE) {
                binding.hudPowerupText.text = status
                binding.hudPowerupText.visibility = View.VISIBLE
            }
        }
    }

    override fun onPowerupsUpdated(status: List<PowerupStatus>, combo: Int) {
        runOnUiThread {
            renderPowerupChips(status)
            val showCombo = combo >= 2
            val text = when {
                status.isEmpty() && showCombo -> "Powerups: none • Combo x$combo"
                status.isEmpty() -> "Powerups: none"
                showCombo -> "Combo x$combo"
                else -> ""
            }
            if (text.isNotEmpty()) {
                binding.hudPowerupText.text = text
                binding.hudPowerupText.visibility = View.VISIBLE
            } else {
                binding.hudPowerupText.visibility = View.GONE
            }
        }
    }

    override fun onFpsUpdate(fps: Int) {
        runOnUiThread {
            if (config.settings.showFpsCounter) {
                binding.hudFps.text = "FPS: $fps"
                binding.hudFps.visibility = android.view.View.VISIBLE
            } else {
                binding.hudFps.visibility = android.view.View.GONE
            }
        }
    }

    override fun onTip(message: String) {
        if (!config.settings.tipsEnabled) return
        runOnUiThread {
            binding.hudTip.text = message
            binding.hudTip.visibility = View.VISIBLE
            binding.hudTip.alpha = 0f
            binding.hudTip.animate().alpha(1f).setDuration(300).withEndAction {
                binding.hudTip.animate().alpha(0f).setDuration(600).setStartDelay(2000).withEndAction {
                    binding.hudTip.visibility = View.GONE
                }.start()
            }.start()
        }
    }

    override fun onGameOver(summary: GameSummary) {
        runOnUiThread {
            // Check if this is a high score for the mode
            if (ScoreboardManager.isHighScoreForMode(this, config.mode.displayName, summary.score, summary.durationSeconds)) {
                // Show name input dialog
                showNameInputDialog(summary)
            } else {
                // Not a high score, just show game over screen
                binding.endTitle.text = getString(R.string.label_game_over)
                binding.endStats.text = "Score ${summary.score} • Level ${summary.level}"
                binding.buttonEndPrimary.text = getString(R.string.label_restart)
                showOverlay(binding.endOverlay)
            }
        }
    }

    private fun showNameInputDialog(summary: GameSummary) {
        val input = android.widget.EditText(this)
        input.hint = "Enter your name"
        input.setText("Player") // Default name
        input.setSelection(input.text.length)

        android.app.AlertDialog.Builder(this)
            .setTitle("New High Score!")
            .setMessage("Score: ${summary.score} • Level: ${summary.level}\nMode: ${config.mode.displayName}")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val playerName = input.text.toString().trim().ifEmpty { "Player" }
                ScoreboardManager.addHighScore(
                    this,
                    ScoreboardManager.ScoreEntry(
                        score = summary.score,
                        mode = config.mode.displayName,
                        name = playerName,
                        level = summary.level,
                        durationSeconds = summary.durationSeconds,
                        timestamp = System.currentTimeMillis()
                    )
                )
                // Now show the game over screen
                binding.endTitle.text = getString(R.string.label_game_over)
                binding.endStats.text = "Score ${summary.score} • Level ${summary.level}"
                binding.buttonEndPrimary.text = getString(R.string.label_restart)
                showOverlay(binding.endOverlay)
            }
            .setNegativeButton("Skip") { _, _ ->
                // Don't save, just show game over screen
                binding.endTitle.text = getString(R.string.label_game_over)
                binding.endStats.text = "Score ${summary.score} • Level ${summary.level}"
                binding.buttonEndPrimary.text = getString(R.string.label_restart)
                showOverlay(binding.endOverlay)
            }
            .setCancelable(false)
            .show()
    }

    override fun onLevelComplete(summary: GameSummary) {
        runOnUiThread {
            binding.endTitle.text = getString(R.string.label_level_complete)
            binding.endStats.text = "Score ${summary.score} • Level ${summary.level}"
            binding.buttonEndPrimary.text = getString(R.string.label_next_level)
            showOverlay(binding.endOverlay)
        }
    }

    private fun showTooltip() {
        showOverlay(binding.tooltipOverlay)
    }

    private fun hideTooltip() {
        hideOverlay(binding.tooltipOverlay)
    }

    private fun showOverlay(view: View) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.scaleX = 0.96f
        view.scaleY = 0.96f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    private fun hideOverlay(view: View) {
        if (view.visibility != View.VISIBLE) return
        view.animate()
            .alpha(0f)
            .scaleX(0.98f)
            .scaleY(0.98f)
            .setDuration(180)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                view.visibility = View.GONE
                view.alpha = 1f
                view.scaleX = 1f
                view.scaleY = 1f
            }
            .start()
    }

    private fun renderPowerupChips(status: List<PowerupStatus>) {
        val container = binding.hudPowerupChips
        container.removeAllViews()
        if (status.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        status.forEach { item ->
            container.addView(buildPowerupChip(item))
        }
    }

    private fun buildPowerupChip(status: PowerupStatus): android.widget.TextView {
        val chip = android.widget.TextView(this)
        chip.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
        chip.setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
        chip.setSingleLine(true)
        chip.setPadding(dp(10), dp(6), dp(10), dp(6))

        val label = if (status.type == PowerUpType.SHIELD && status.charges > 0) {
            "${powerupLabel(status.type)} x${status.charges} ${status.remainingSeconds}s"
        } else {
            "${powerupLabel(status.type)} ${status.remainingSeconds}s"
        }
        val text = "● $label"
        val spannable = android.text.SpannableString(text)
        val color = android.graphics.Color.rgb(
            (status.type.color[0] * 255).toInt().coerceIn(0, 255),
            (status.type.color[1] * 255).toInt().coerceIn(0, 255),
            (status.type.color[2] * 255).toInt().coerceIn(0, 255)
        )
        spannable.setSpan(
            android.text.style.ForegroundColorSpan(color),
            0,
            1,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        chip.text = spannable
        chip.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.bp_white))

        val backgroundColor = ColorUtils.setAlphaComponent(color, 46)
        val strokeColor = ColorUtils.setAlphaComponent(color, 120)
        val drawable = android.graphics.drawable.GradientDrawable()
        drawable.cornerRadius = dp(14).toFloat()
        drawable.setColor(backgroundColor)
        drawable.setStroke(dp(1), strokeColor)
        chip.background = drawable

        val params = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.marginEnd = dp(8)
        chip.layoutParams = params
        return chip
    }

    private fun powerupLabel(type: PowerUpType): String {
        return when (type) {
            PowerUpType.MULTI_BALL -> "MB"
            PowerUpType.LASER -> "LZR"
            PowerUpType.GUARDRAIL -> "GRD"
            PowerUpType.LIFE -> "1UP"
            PowerUpType.SHIELD -> "SHD"
            PowerUpType.WIDE_PADDLE -> "WIDE"
            PowerUpType.SLOW -> "SLOW"
            PowerUpType.FIREBALL -> "FIRE"
            PowerUpType.MAGNET -> "MAG"
            PowerUpType.GRAVITY_WELL -> "GRAV"
            PowerUpType.BALL_SPLITTER -> "SPLIT"
            PowerUpType.FREEZE -> "FRZ"
            PowerUpType.PIERCE -> "PRC"
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_MODE = "extra_mode"
    }
}
