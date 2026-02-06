package com.breakoutplus

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import android.os.Build
import android.view.Display
import android.view.WindowManager
import com.breakoutplus.DailyChallengeStore
import com.breakoutplus.databinding.ActivityGameBinding
import com.breakoutplus.game.GameConfig
import com.breakoutplus.game.GameEventListener
import com.breakoutplus.game.GameMode
import com.breakoutplus.game.GameSummary
import com.breakoutplus.game.PowerUpType
import com.breakoutplus.game.PowerupStatus
import java.util.concurrent.atomic.AtomicBoolean

class GameActivity : FoldAwareActivity(), GameEventListener {
    private lateinit var binding: ActivityGameBinding
    private lateinit var config: GameConfig
    private var currentModeLabel: String = "Classic"
    private var currentPowerupSummary: String = "Powerups: none"
    private var currentCombo: Int = 0
    private var laserActive: Boolean = false
    private val hudUpdateQueued = AtomicBoolean(false)
    @Volatile private var pendingScore: Int? = null
    @Volatile private var pendingLives: Int? = null
    @Volatile private var pendingFps: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)

        val modeName = intent.getStringExtra(EXTRA_MODE)
        val mode = runCatching { GameMode.valueOf(modeName ?: "CLASSIC") }.getOrDefault(GameMode.CLASSIC)
        val settings = SettingsManager.load(this)
        val dailyChallenges = DailyChallengeStore.load(this)
        config = GameConfig(mode, settings, dailyChallenges)

        binding.gameSurface.start(config, this)
        applyFrameRatePreference()
        applyHandedness(settings.leftHanded)

        binding.buttonPause.setOnClickListener { showPause(true) }
        binding.buttonResume.setOnClickListener { showPause(false) }
        binding.buttonRestart.setOnClickListener { restartGame() }
        binding.buttonExit.setOnClickListener { exitToMenu() }
        binding.buttonEndSecondary.setOnClickListener { exitToMenu() }
        binding.buttonEndPrimary.setOnClickListener { handleEndPrimary() }
        binding.buttonTooltipDismiss.setOnClickListener { hideTooltip() }
        binding.buttonLaser.setOnClickListener { binding.gameSurface.fireLaser() }

        // Show first-run tooltip if tips enabled
        if (settings.tipsEnabled) {
            showTooltip()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSettings()
        applyFrameRatePreference()
        binding.gameSurface.onResume()
        if (binding.pauseOverlay.visibility != View.VISIBLE) {
            binding.gameSurface.resumeGame()
        }
    }

    override fun onPause() {
        binding.gameSurface.pauseGame()
        binding.gameSurface.onPause()
        config.dailyChallenges?.let { DailyChallengeStore.save(this, it) }
        super.onPause()
    }

    private fun refreshSettings() {
        val settings = SettingsManager.load(this)
        val challenges = config.dailyChallenges ?: DailyChallengeStore.load(this)
        config = GameConfig(config.mode, settings, challenges)
        binding.gameSurface.applySettings(settings)
        applyHandedness(settings.leftHanded)
        if (!settings.tipsEnabled) {
            binding.hudTip.visibility = View.GONE
            hideTooltip()
        }
        if (!settings.showFpsCounter) {
            binding.hudFps.visibility = View.GONE
            if (binding.hudPowerupChips.childCount == 0) {
                binding.hudPowerups.visibility = View.GONE
            }
        }
    }

    private fun applyFrameRatePreference() {
        val display = resolveDisplay() ?: return
        val bestMode = selectBestMode(display, config.settings.highRefreshRate)
        val targetFps = bestMode?.refreshRate ?: display.refreshRate

        val params = window.attributes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && bestMode != null) {
            params.preferredDisplayModeId = bestMode.modeId
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            params.preferredRefreshRate = targetFps
        }
        window.attributes = params
        binding.gameSurface.setTargetFrameRate(targetFps)
    }

    private fun resolveDisplay(): Display? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
        }
    }

    private fun selectBestMode(display: Display, allowHighRefresh: Boolean): Display.Mode? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        val current = display.mode
        val candidates = display.supportedModes.filter {
            it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight
        }
        return if (allowHighRefresh) {
            candidates.maxByOrNull { it.refreshRate }
        } else {
            val sixty = candidates.filter { it.refreshRate <= 61f }
            if (sixty.isNotEmpty()) {
                sixty.maxByOrNull { it.refreshRate }
            } else {
                candidates.minByOrNull { it.refreshRate }
            }
        }
    }

    private fun showPause(show: Boolean) {
        if (show) {
            showOverlay(binding.pauseOverlay)
            binding.gameSurface.pauseGame()
            binding.buttonLaser.visibility = View.GONE
        } else {
            hideOverlay(binding.pauseOverlay)
            binding.gameSurface.resumeGame()
            if (laserActive) {
                binding.buttonLaser.visibility = View.VISIBLE
            }
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
        if (!isCentered) {
            if (leftHanded) {
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = ConstraintLayout.LayoutParams.UNSET
            } else {
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                params.startToStart = ConstraintLayout.LayoutParams.UNSET
            }
            binding.buttonPause.layoutParams = params
        }

        val laserParams = binding.buttonLaser.layoutParams as ConstraintLayout.LayoutParams
        if (leftHanded) {
            laserParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            laserParams.endToEnd = ConstraintLayout.LayoutParams.UNSET
        } else {
            laserParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            laserParams.startToStart = ConstraintLayout.LayoutParams.UNSET
        }
        binding.buttonLaser.layoutParams = laserParams
    }

    override fun onScoreUpdated(score: Int) {
        pendingScore = score
        scheduleHudUpdate()
    }

    override fun onLivesUpdated(lives: Int) {
        pendingLives = lives
        scheduleHudUpdate()
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
        runOnUiThread {
            binding.hudLevel.text = "Level $level"
            showLevelBanner(level)
        }
    }

    override fun onModeUpdated(mode: GameMode) {
        runOnUiThread {
            currentModeLabel = mode.displayName
            updateHudMeta()
            updateShieldVisibility(mode.invaders)
        }
    }

    override fun onPowerupStatus(status: String) {
        runOnUiThread {
            currentPowerupSummary = status.split("•").firstOrNull()?.trim() ?: status
            updateHudMeta()
        }
    }

    override fun onPowerupsUpdated(status: List<PowerupStatus>, combo: Int) {
        runOnUiThread {
            renderPowerupChips(status)
            currentCombo = combo
            currentPowerupSummary = if (status.isEmpty()) {
                "Powerups: none"
            } else {
                "Powerups: ${status.size} active"
            }
            updateLaserButton(status)
            updateHudMeta()
        }
    }

    override fun onFpsUpdate(fps: Int) {
        pendingFps = fps
        scheduleHudUpdate()
    }

    override fun onShieldUpdated(current: Int, max: Int) {
        runOnUiThread {
            if (max <= 0) {
                updateShieldVisibility(false)
                return@runOnUiThread
            }
            updateShieldVisibility(true)
            binding.hudShieldBar.max = max
            binding.hudShieldBar.progress = current.coerceIn(0, max)
            val percent = ((current.toFloat() / max.toFloat()) * 100f).toInt().coerceIn(0, 100)
            binding.hudShieldLabel.text = "Shield $percent%"
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
            binding.buttonLaser.visibility = View.GONE
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
            config.dailyChallenges?.let { DailyChallengeStore.save(this, it) }
        }
    }

    private fun showNameInputDialog(summary: GameSummary) {
        val dialog = android.app.Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_high_score, null)
        dialog.setContentView(view)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val title = view.findViewById<android.widget.TextView>(R.id.highScoreTitle)
        val meta = view.findViewById<android.widget.TextView>(R.id.highScoreMeta)
        val input = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.highScoreNameInput)
        val saveButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.highScoreSave)
        val skipButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.highScoreSkip)

        title.text = "New High Score!"
        meta.text = "Score ${summary.score} • Level ${summary.level} • Mode ${config.mode.displayName}"
        input.setText("Player")
        input.setSelection(input.text?.length ?: 0)
        input.requestFocus()
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        val finishDialog = {
            binding.endTitle.text = getString(R.string.label_game_over)
            binding.endStats.text = "Score ${summary.score} • Level ${summary.level}"
            binding.buttonEndPrimary.text = getString(R.string.label_restart)
            showOverlay(binding.endOverlay)
        }

        saveButton.setOnClickListener {
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
            dialog.dismiss()
            finishDialog()
        }

        skipButton.setOnClickListener {
            dialog.dismiss()
            finishDialog()
        }

        dialog.show()
    }

    override fun onLevelComplete(summary: GameSummary) {
        runOnUiThread {
            binding.buttonLaser.visibility = View.GONE
            binding.endTitle.text = getString(R.string.label_level_complete)
            binding.endStats.text = "Score ${summary.score} • Level ${summary.level}"
            binding.buttonEndPrimary.text = getString(R.string.label_next_level)
            showOverlay(binding.endOverlay)
            config.dailyChallenges?.let { DailyChallengeStore.save(this, it) }
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

    private fun showLevelBanner(level: Int) {
        val banner = binding.hudLevelBanner
        banner.text = "Level $level"
        banner.animate().cancel()
        banner.visibility = View.VISIBLE
        banner.alpha = 0f
        banner.scaleX = 0.92f
        banner.scaleY = 0.92f
        banner.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(180)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                banner.animate()
                    .alpha(0f)
                    .scaleX(1.04f)
                    .scaleY(1.04f)
                    .setStartDelay(800)
                    .setDuration(260)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .withEndAction {
                        banner.visibility = View.GONE
                        banner.alpha = 1f
                        banner.scaleX = 1f
                        banner.scaleY = 1f
                    }
                    .start()
            }
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
            if (binding.hudFps.visibility != View.VISIBLE) {
                binding.hudPowerups.visibility = View.GONE
            }
            return
        }
        binding.hudPowerups.visibility = View.VISIBLE
        container.visibility = View.VISIBLE
        status.forEach { item ->
            container.addView(buildPowerupChip(item))
        }
    }

    private fun buildPowerupChip(status: PowerupStatus): android.widget.TextView {
        val chip = android.widget.TextView(this)
        chip.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.bp_hud_mode_size))
        chip.setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
        chip.setSingleLine(true)
        chip.setPadding(dp(10), dp(6), dp(10), dp(6))
        chip.letterSpacing = 0.02f

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

    private fun updateHudMeta() {
        val parts = mutableListOf<String>()
        if (currentModeLabel.isNotBlank()) parts.add(currentModeLabel)
        if (currentPowerupSummary.isNotBlank()) parts.add(currentPowerupSummary)
        if (currentCombo >= 2) parts.add("Combo x$currentCombo")
        binding.hudMeta.text = parts.joinToString(" • ")
    }

    private fun updateShieldVisibility(show: Boolean) {
        binding.hudShieldRow.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun updateLaserButton(status: List<PowerupStatus>) {
        val hasLaser = status.any { it.type == PowerUpType.LASER }
        laserActive = hasLaser
        binding.buttonLaser.visibility = if (hasLaser) View.VISIBLE else View.GONE
    }

    private fun scheduleHudUpdate() {
        if (!hudUpdateQueued.compareAndSet(false, true)) return
        binding.root.postOnAnimation {
            hudUpdateQueued.set(false)
            pendingScore?.let {
                binding.hudScore.text = "Score $it"
                pendingScore = null
            }
            pendingLives?.let {
                binding.hudLives.text = "Lives $it"
                pendingLives = null
            }
            val fps = pendingFps
            if (fps != null && config.settings.showFpsCounter) {
                binding.hudFps.text = "FPS: $fps"
                binding.hudFps.visibility = View.VISIBLE
                if (binding.hudPowerups.visibility != View.VISIBLE) {
                    binding.hudPowerups.visibility = View.VISIBLE
                }
            } else if (!config.settings.showFpsCounter) {
                binding.hudFps.visibility = View.GONE
                if (binding.hudPowerupChips.visibility != View.VISIBLE) {
                    binding.hudPowerups.visibility = View.GONE
                }
            }
        }
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
