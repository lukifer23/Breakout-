package com.breakoutplus

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.breakoutplus.databinding.ActivityGameBinding
import com.breakoutplus.game.GameConfig
import com.breakoutplus.game.GameEventListener
import com.breakoutplus.game.GameMode
import com.breakoutplus.game.GameSummary

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
        runOnUiThread { binding.hudPowerupText.text = status }
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
            if (ScoreboardManager.isHighScoreForMode(this, config.mode.displayName, summary.score)) {
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

    companion object {
        const val EXTRA_MODE = "extra_mode"
    }
}
