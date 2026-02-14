package com.breakoutplus

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import android.os.Build
import android.view.Display
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import com.breakoutplus.DailyChallengeStore
import com.breakoutplus.ProgressionManager
import com.breakoutplus.databinding.ActivityGameBinding
import com.breakoutplus.game.GameConfig
import com.breakoutplus.game.GameEventListener
import com.breakoutplus.game.GameMode
import com.breakoutplus.game.GameSummary
import com.breakoutplus.game.PowerUpType
import com.breakoutplus.game.PowerupStatus
import com.breakoutplus.UnlockManager
import androidx.activity.OnBackPressedCallback
import java.util.concurrent.atomic.AtomicBoolean

class GameActivity : FoldAwareActivity(), GameEventListener {
    private enum class EndOverlayState { NONE, LEVEL_COMPLETE, GAME_OVER }

    private lateinit var binding: ActivityGameBinding
    private lateinit var config: GameConfig
    private var currentMode: GameMode = GameMode.CLASSIC
    private var currentModeLabel: String = "Classic"
    private var currentPowerupSummary: String = "Powerups: none"
    private var currentCombo: Int = 0
    private var currentPowerupCount: Int = 0
    private var currentJourneyLabel: String = ""
    private var currentXpTotal: Int = 0
    private var laserActive: Boolean = false
    private var laserCooldownEndMs: Long = 0L
    private var laserCooldownRunnable: Runnable? = null
    private var lastShieldValue: Int = 0
    private var endStatsAnimator: android.animation.ValueAnimator? = null
    private val hudUpdateQueued = AtomicBoolean(false)
    private var endOverlayState: EndOverlayState = EndOverlayState.NONE
    private var maxInsetTop = 0
    private var maxInsetBottom = 0
    private var baseSurfaceBottomMargin = 0
    @Volatile private var pendingScore: Int? = null
    @Volatile private var pendingLives: Int? = null
    @Volatile private var pendingFps: Int? = null
    @Volatile private var pendingVolleyBalls: Int? = null
    private var hudScale: Float = 1f
    private var hudChipTextPx: Float = 0f
    private var lastHudAvailWidthPx: Int = -1
    private var lastHudAvailHeightPx: Int = -1
    private var levelAdvanceInProgress = false
    private var debugAutoPlaySession = false
    private var debugAutoPlayStopRunnable: Runnable? = null
    private var hudResizeRunnable: Runnable? = null
    private val hudResizeDebounceMs = 120L
    private var hudResizingInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFoldAwareRoot(binding.root)
        configureSystemUi()
        observeViewportChanges()
        applyResponsiveHudSizing()
        baseSurfaceBottomMargin =
            (binding.gameSurface.layoutParams as ConstraintLayout.LayoutParams).bottomMargin
        applyWindowInsets()

        val modeName = intent.getStringExtra(EXTRA_MODE)
        val mode = runCatching { GameMode.valueOf(modeName ?: "CLASSIC") }.getOrDefault(GameMode.CLASSIC)
        val settings = SettingsManager.load(this)
        val dailyChallenges = DailyChallengeStore.load(this)
        val unlocks = UnlockManager.load(this)
        config = GameConfig(mode, settings, dailyChallenges, unlocks)
        currentMode = mode
        currentXpTotal = ProgressionManager.loadXp(this)
        updateJourneyLabel(1)
        applyModeHud(mode)

        binding.gameSurface.start(config, this)
        applyFrameRatePreference()
        applyHandedness(settings.leftHanded)

        val isDebugBuild = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebugBuild) {
            intent.getStringExtra(EXTRA_DEBUG_POWERUP)?.let { powerupName ->
                runCatching { PowerUpType.valueOf(powerupName) }.onSuccess { type ->
                    binding.gameSurface.postDelayed({
                        binding.gameSurface.debugSpawnPowerup(type)
                    }, 600)
                }
            }
            debugAutoPlaySession = intent.getBooleanExtra(EXTRA_DEBUG_AUTOPLAY, false)
            if (debugAutoPlaySession) {
                val runSeconds = intent.getIntExtra(EXTRA_DEBUG_AUTOPLAY_SECONDS, 0).coerceIn(0, 600)
                binding.gameSurface.setDebugAutoPlay(true)
                if (runSeconds > 0) {
                    val stopRunnable = Runnable { binding.gameSurface.setDebugAutoPlay(false) }
                    debugAutoPlayStopRunnable = stopRunnable
                    binding.gameSurface.postDelayed(stopRunnable, runSeconds * 1000L)
                }
                Log.i("BreakoutAutoPlay", "event=session_start mode=${mode.name} seconds=${runSeconds}")
            }
        }

        binding.buttonPause.setOnClickListener { showPause(true) }
        binding.buttonResume.setOnClickListener { showPause(false) }
        binding.buttonRestart.setOnClickListener { restartGame() }
        binding.buttonExit.setOnClickListener { exitToMenu() }
        binding.buttonEndSecondary.setOnClickListener { exitToMenu() }
        binding.buttonEndPrimary.setOnClickListener { handleEndPrimary() }
        binding.buttonTooltipDismiss.setOnClickListener { hideTooltip() }
        binding.buttonLaser.setOnClickListener { binding.gameSurface.fireLaser() }
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.pauseOverlay.visibility == View.VISIBLE) {
                        showPause(false)
                    } else if (binding.endOverlay.visibility != View.VISIBLE) {
                        showPause(true)
                    }
                }
            }
        )

        // Show first-run tooltip if tips enabled
        if (settings.tipsEnabled) {
            showTooltip()
        }

        playGameFade()
    }

    override fun onResume() {
        super.onResume()
        applyResponsiveHudSizing()
        refreshSettings()
        applyFrameRatePreference()
        binding.gameSurface.onResume()
        if (shouldResumeGameplay()) {
            binding.gameSurface.resumeGame()
        }
    }

    override fun onPause() {
        binding.gameSurface.pauseGame()
        binding.gameSurface.onPause()
        levelAdvanceInProgress = false
        config.dailyChallenges?.let { DailyChallengeStore.save(this, it) }
        laserCooldownRunnable?.let { binding.buttonLaser.removeCallbacks(it) }
        debugAutoPlayStopRunnable?.let { binding.gameSurface.removeCallbacks(it) }
        debugAutoPlayStopRunnable = null
        hudResizeRunnable?.let { binding.root.removeCallbacks(it) }
        hudResizeRunnable = null
        if (debugAutoPlaySession) {
            binding.gameSurface.setDebugAutoPlay(false)
        }
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.root.post { applyResponsiveHudSizing() }
    }

    private fun refreshSettings() {
        val settings = SettingsManager.load(this)
        val challenges = config.dailyChallenges ?: DailyChallengeStore.load(this)
        val unlocks = UnlockManager.load(this)
        config = GameConfig(config.mode, settings, challenges, unlocks)
        binding.gameSurface.applySettings(settings)
        binding.gameSurface.applyUnlocks(unlocks)
        applyHandedness(settings.leftHanded)
        if (!settings.tipsEnabled) {
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
        if (bestMode != null) {
            params.preferredDisplayModeId = bestMode.modeId
        } else {
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
        levelAdvanceInProgress = false
        hideOverlay(binding.endOverlay)
        hideOverlay(binding.pauseOverlay)
        hideOverlay(binding.tooltipOverlay)
        endOverlayState = EndOverlayState.NONE
        binding.buttonEndPrimary.isEnabled = true
        binding.buttonEndSecondary.isEnabled = true
        binding.gameSurface.resumeGame()
        binding.gameSurface.restartGame()
        playGameFade()
    }

    private fun exitToMenu() {
        levelAdvanceInProgress = false
        finish()
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun handleEndPrimary() {
        when (endOverlayState) {
            EndOverlayState.LEVEL_COMPLETE -> {
                if (levelAdvanceInProgress) return
                levelAdvanceInProgress = true
                binding.buttonEndPrimary.isEnabled = false
                binding.buttonEndSecondary.isEnabled = false
                hideOverlay(binding.endOverlay)
                if (!isFinishing && !isDestroyed) {
                    Log.d("GameActivity", "Advancing to next level (activity state: finishing=$isFinishing, destroyed=$isDestroyed)")
                    binding.gameSurface.nextLevel()
                    playGameFade()
                } else {
                    Log.w("GameActivity", "Cannot advance level - activity finishing or destroyed")
                }
                endOverlayState = EndOverlayState.NONE
                levelAdvanceInProgress = false
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

    private fun applyWindowInsets() {
        val baseTop = binding.hudContainer.paddingTop
        val baseBottom = binding.hudContainer.paddingBottom
        val baseSurfaceParams = binding.gameSurface.layoutParams as ConstraintLayout.LayoutParams

        // Apply initial insets synchronously if available.
        ViewCompat.getRootWindowInsets(binding.root)?.let { initialInsets ->
            val bars = stableSystemInsets(initialInsets)
            maxInsetTop = bars.top
            maxInsetBottom = bars.bottom
            val topPadding = baseTop + maxInsetTop
            binding.hudContainer.setPadding(
                binding.hudContainer.paddingLeft,
                topPadding,
                binding.hudContainer.paddingRight,
                baseBottom
            )
            val desiredBottomMargin = baseSurfaceBottomMargin + maxInsetBottom
            baseSurfaceParams.bottomMargin = desiredBottomMargin
            binding.gameSurface.layoutParams = baseSurfaceParams
        }

        // Keep gameplay bounds stable across transient system bar animations.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = stableSystemInsets(insets)
            val insetsChanged = bars.top != maxInsetTop || bars.bottom != maxInsetBottom
            if (!insetsChanged) return@setOnApplyWindowInsetsListener insets

            maxInsetTop = bars.top
            maxInsetBottom = bars.bottom
            val topPadding = baseTop + maxInsetTop
            binding.hudContainer.setPadding(
                binding.hudContainer.paddingLeft,
                topPadding,
                binding.hudContainer.paddingRight,
                baseBottom
            )
            val params = binding.gameSurface.layoutParams as ConstraintLayout.LayoutParams
            val desiredBottomMargin = baseSurfaceBottomMargin + maxInsetBottom
            params.bottomMargin = desiredBottomMargin
            binding.gameSurface.layoutParams = params
            insets
        }
    }

    private fun stableSystemInsets(insets: WindowInsetsCompat): androidx.core.graphics.Insets {
        val stableTypes = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insets.getInsetsIgnoringVisibility(stableTypes)
        } else {
            @Suppress("DEPRECATION")
            insets.getInsets(stableTypes)
        }
    }

    private fun configureSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun observeViewportChanges() {
        binding.root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val availWidth = (binding.root.width - binding.root.paddingLeft - binding.root.paddingRight)
                .coerceAtLeast(0)
            val availHeight = (binding.root.height - binding.root.paddingTop - binding.root.paddingBottom)
                .coerceAtLeast(0)
            if (availWidth <= 0 || availHeight <= 0) return@addOnLayoutChangeListener
            if (availWidth == lastHudAvailWidthPx && availHeight == lastHudAvailHeightPx) return@addOnLayoutChangeListener
            lastHudAvailWidthPx = availWidth
            lastHudAvailHeightPx = availHeight

            // Debounce HUD resize to prevent rapid layout changes
            hudResizeRunnable?.let { binding.root.removeCallbacks(it) }
            val runnable = Runnable { applyResponsiveHudSizing() }
            hudResizeRunnable = runnable
            binding.root.postDelayed(runnable, hudResizeDebounceMs)
        }
    }

    override fun onScoreUpdated(score: Int) {
        pendingScore = score
        scheduleHudUpdate()
    }

    override fun onLivesUpdated(lives: Int) {
        pendingLives = lives
        scheduleHudUpdate()
    }

    override fun onVolleyBallsUpdated(volleyBalls: Int) {
        pendingVolleyBalls = volleyBalls
        scheduleHudUpdate()
    }

    override fun onTimeUpdated(secondsRemaining: Int) {
        runOnUiThread {
            if (currentMode == GameMode.ZEN) {
                binding.hudTime.visibility = View.GONE
                return@runOnUiThread
            }
            val minutes = secondsRemaining / 60
            val seconds = secondsRemaining % 60
            val isCountdown = config.mode.timeLimitSeconds > 0
            binding.hudTime.visibility = android.view.View.VISIBLE

            val timeText = if (isCountdown) {
                getString(R.string.label_time_format, minutes, seconds)
            } else {
                getString(R.string.label_elapsed_format, minutes, seconds)
            }

            binding.hudTime.text = timeText
            if (isCountdown && secondsRemaining <= 10 && config.mode != GameMode.SURVIVAL) {
                binding.hudTime.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.bp_red))
            } else {
                binding.hudTime.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.bp_white))
            }
        }
    }

    override fun onLevelUpdated(level: Int) {
        runOnUiThread {
            binding.hudLevel.text = getString(R.string.label_level_format, level)
            updateJourneyLabel(level)
            updateHudMeta()
        }
    }

    override fun onModeUpdated(mode: GameMode) {
        runOnUiThread {
            currentMode = mode
            currentModeLabel = mode.displayName
            applyModeHud(mode)
            updateHudMeta()
            updateShieldVisibility(mode.invaders)
        }
    }

    override fun onPowerupStatus(status: String) {
        runOnUiThread {
            currentPowerupSummary = status
            updateHudMeta()
        }
    }

    override fun onPowerupsUpdated(status: List<PowerupStatus>, combo: Int) {
        runOnUiThread {
            val previousCount = currentPowerupCount
            val previousCombo = currentCombo
            renderPowerupChips(status)
            currentCombo = combo
            currentPowerupCount = status.size
            val preserveModeSummary =
                currentMode == GameMode.VOLLEY ||
                    currentMode == GameMode.TUNNEL ||
                    currentMode == GameMode.SURVIVAL ||
                    currentMode == GameMode.ZEN
            if (!preserveModeSummary) {
                currentPowerupSummary = if (status.isEmpty()) {
                    getString(R.string.label_powerups_none)
                } else {
                    resources.getQuantityString(R.plurals.label_powerups_active, status.size, status.size)
                }
            }
            updateLaserButton(status)
            updateHudMeta()
            if (status.size > previousCount || combo > previousCombo) {
                pulseHudMeta()
            }
        }
    }

    override fun onLaserFired(cooldownSeconds: Float) {
        runOnUiThread {
            if (!laserActive) return@runOnUiThread
            startLaserCooldown(cooldownSeconds)
        }
    }

    override fun onThemeUnlocked(themeName: String) {
        val updated = UnlockManager.unlockTheme(this, themeName)
        config = config.copy(unlocks = updated)
        binding.gameSurface.applyUnlocks(updated)
        showBanner(getString(R.string.label_theme_unlocked, themeName))
    }

    override fun onCosmeticUnlocked(newTier: Int) {
        val updated = UnlockManager.setCosmeticTier(this, newTier)
        config = config.copy(unlocks = updated)
        binding.gameSurface.applyUnlocks(updated)
        showBanner(getString(R.string.label_cosmetic_unlocked))
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
            binding.hudShieldLabel.text = getString(R.string.label_shield_percent, percent)
            if (current < lastShieldValue) {
                binding.hudShieldBar.animate().cancel()
                binding.hudShieldBar.scaleX = 1f
                binding.hudShieldBar.scaleY = 1f
                binding.hudShieldBar.animate()
                    .scaleX(1.08f)
                    .scaleY(1.08f)
                    .setDuration(UiMotion.PULSE_IN_DURATION)
                    .withEndAction {
                        binding.hudShieldBar.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(UiMotion.PULSE_OUT_DURATION)
                            .start()
                    }
                    .start()
                binding.hudShieldLabel.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.bp_red))
                binding.hudShieldLabel.postDelayed({
                    binding.hudShieldLabel.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.bp_white))
                }, 260L)
            }
            lastShieldValue = current
        }
    }

    override fun onTip(message: String) {
        // Intentionally no-op: bottom tip popups obstruct gameplay and are redundant with HUD.
    }

    override fun onGameOver(summary: GameSummary) {
        runOnUiThread {
            if (config.mode == GameMode.ZEN) {
                restartGame()
                return@runOnUiThread
            }
            levelAdvanceInProgress = false
            binding.buttonEndPrimary.isEnabled = true
            binding.buttonEndSecondary.isEnabled = true
            binding.buttonLaser.visibility = View.GONE
            endOverlayState = EndOverlayState.GAME_OVER
            LifetimeStatsManager.recordRun(this, summary)
            if (debugAutoPlaySession) {
                Log.i(
                    "BreakoutAutoPlay",
                    "event=game_over mode=${config.mode.name} score=${summary.score} level=${summary.level} duration=${summary.durationSeconds} bricks=${summary.bricksBroken} lives_lost=${summary.livesLost}"
                )
            }
            // Check if this is a high score for the mode
            if (ScoreboardManager.isHighScoreForMode(this, config.mode.displayName, summary.score, summary.durationSeconds)) {
                // Show name input dialog
                showNameInputDialog(summary)
            } else {
                // Not a high score, just show game over screen
                binding.endTitle.text = getString(R.string.label_game_over)
                animateEndStats(summary, getString(R.string.label_game_over))
                binding.buttonEndPrimary.text = getString(R.string.label_restart)
                showOverlay(binding.endOverlay)
            }
            config.dailyChallenges?.let { DailyChallengeStore.save(this, it) }
        }
    }

    private fun showNameInputDialog(summary: GameSummary) {
        val dialog = android.app.Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_high_score, binding.root, false)
        dialog.setContentView(view)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9f).toInt().coerceAtMost(600),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        // Position in upper portion to avoid paddle area, more conservative on small screens
        val window = dialog.window
        val params = window?.attributes
        params?.gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
        val screenHeight = resources.displayMetrics.heightPixels
        val safeTopMargin = when {
            screenHeight < 1200 -> (screenHeight * 0.08f).toInt() // 8% for small screens
            screenHeight < 1800 -> (screenHeight * 0.12f).toInt() // 12% for medium screens
            else -> (screenHeight * 0.15f).toInt() // 15% for large screens
        }.coerceAtLeast(80) // Minimum 80dp
        params?.y = safeTopMargin
        window?.attributes = params

        val title = view.findViewById<android.widget.TextView>(R.id.highScoreTitle)
        val meta = view.findViewById<android.widget.TextView>(R.id.highScoreMeta)
        val input = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.highScoreNameInput)
        val saveButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.highScoreSave)
        val skipButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.highScoreSkip)

        title.text = getString(R.string.label_high_score_title)
        meta.text = getString(
            R.string.label_score_level_mode_format,
            summary.score,
            summary.level,
            config.mode.displayName
        )
        input.setText(getString(R.string.label_player_default))
        input.setSelection(input.text?.length ?: 0)
        input.requestFocus()
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        val finishDialog = {
            endOverlayState = EndOverlayState.GAME_OVER
            binding.endTitle.text = getString(R.string.label_game_over)
            animateEndStats(summary, getString(R.string.label_game_over))
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
            levelAdvanceInProgress = false
            binding.buttonEndPrimary.isEnabled = true
            binding.buttonEndSecondary.isEnabled = true
            binding.buttonLaser.visibility = View.GONE
            if (debugAutoPlaySession) {
                Log.i(
                    "BreakoutAutoPlay",
                    "event=level_complete mode=${config.mode.name} score=${summary.score} level=${summary.level} duration=${summary.durationSeconds} bricks=${summary.bricksBroken} lives_lost=${summary.livesLost}"
                )
            }
            ProgressionManager.updateBestLevel(this, summary.level)
            currentXpTotal = ProgressionManager.addXp(this, ProgressionManager.xpForLevel(summary.level))
            updateHudMeta()
            if (config.mode == GameMode.ZEN || config.mode == GameMode.GOD) {
                endOverlayState = EndOverlayState.NONE
                hideOverlay(binding.endOverlay)
                showLevelBanner(summary.level + 1)
                if (!isFinishing && !isDestroyed) {
                    binding.gameSurface.nextLevel()
                    playGameFade()
                }
                config.dailyChallenges?.let { DailyChallengeStore.save(this, it) }
                return@runOnUiThread
            }
            endOverlayState = EndOverlayState.LEVEL_COMPLETE
            binding.endTitle.text = getString(R.string.label_level_complete)
            animateEndStats(summary, getString(R.string.label_level_complete))
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

    private fun shouldResumeGameplay(): Boolean {
        return binding.pauseOverlay.visibility != View.VISIBLE &&
            binding.endOverlay.visibility != View.VISIBLE &&
            binding.tooltipOverlay.visibility != View.VISIBLE
    }

    private fun showOverlay(view: View) {
        // Cancel any ongoing hide animation to prevent flashing
        view.animate().cancel()
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.scaleX = 0.96f
        view.scaleY = 0.96f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(UiMotion.OVERLAY_IN_DURATION)
            .setInterpolator(UiMotion.EMPHASIS_OUT)
            .start()
    }

    private fun showLevelBanner(level: Int) {
        showBanner(getString(R.string.label_level_format, level))
    }

    private fun showBanner(message: String) {
        val banner = binding.hudLevelBanner
        banner.text = message
        banner.animate().cancel()
        banner.visibility = View.VISIBLE
        banner.alpha = 0f
        banner.scaleX = 0.92f
        banner.scaleY = 0.92f
        banner.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(UiMotion.BANNER_IN_DURATION)
            .setInterpolator(UiMotion.EMPHASIS_OUT)
            .withEndAction {
                banner.animate()
                    .alpha(0f)
                    .scaleX(1.04f)
                    .scaleY(1.04f)
                    .setStartDelay(UiMotion.BANNER_HOLD_DURATION)
                    .setDuration(UiMotion.BANNER_OUT_DURATION)
                    .setInterpolator(UiMotion.EMPHASIS_OUT)
                    .withEndAction {
                        banner.visibility = View.INVISIBLE
                        banner.alpha = 1f
                        banner.scaleX = 1f
                        banner.scaleY = 1f
                    }
                    .start()
            }
            .start()
    }

    private fun pulseHudMeta() {
        val meta = binding.hudMeta
        meta.animate().cancel()
        meta.scaleX = 1f
        meta.scaleY = 1f
        meta.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(UiMotion.PULSE_IN_DURATION)
            .setInterpolator(UiMotion.EMPHASIS_OUT)
            .withEndAction {
                meta.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(UiMotion.PULSE_OUT_DURATION)
                    .setInterpolator(UiMotion.EMPHASIS_OUT)
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
            .setDuration(UiMotion.OVERLAY_OUT_DURATION)
            .setInterpolator(UiMotion.EMPHASIS_OUT)
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
        val chipScale = hudScale.coerceIn(0.82f, 1.24f)
        val chipTextSize = if (hudChipTextPx > 0f) hudChipTextPx else resources.getDimension(R.dimen.bp_hud_mode_size)
        chip.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, chipTextSize)
        chip.setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
        chip.setSingleLine(true)
        chip.setPadding(
            dp(10f * chipScale),
            dp(6f * chipScale),
            dp(10f * chipScale),
            dp(6f * chipScale)
        )
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
        drawable.cornerRadius = dp(14f * chipScale).toFloat()
        drawable.setColor(backgroundColor)
        drawable.setStroke(dp(1f.coerceAtLeast(0.9f * chipScale)), strokeColor)
        chip.background = drawable

        val params = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.marginEnd = dp(8f * chipScale)
        chip.layoutParams = params
        return chip
    }

    private fun applyResponsiveHudSizing() {
        // Skip HUD resizing during active gameplay to prevent jitters
        if (binding.gameSurface.isGameRunning()) {
            return
        }

        // Prevent re-entry during layout changes
        if (hudResizingInProgress) {
            return
        }
        hudResizingInProgress = true

        try {
            val metrics = resources.displayMetrics
            if (metrics.density <= 0f) return
        val widthPx = (binding.root.width - binding.root.paddingLeft - binding.root.paddingRight)
            .takeIf { it > 0 } ?: metrics.widthPixels
        val heightPx = (binding.root.height - binding.root.paddingTop - binding.root.paddingBottom)
            .takeIf { it > 0 } ?: metrics.heightPixels
        val widthDp = widthPx / metrics.density
        val heightDp = heightPx / metrics.density
        val shortDp = minOf(widthDp, heightDp)
        val longDp = maxOf(widthDp, heightDp)
        val aspect = (longDp / shortDp).coerceAtLeast(1f)

        val baseScale = when {
            shortDp >= 840f -> 1.14f
            shortDp >= 720f -> 1.1f
            shortDp >= 600f -> 1.05f
            shortDp <= 340f -> 0.82f
            shortDp <= 380f -> 0.86f
            shortDp <= 420f -> 0.92f
            else -> 1f
        }
        val tallFoldCompaction = when {
            aspect >= 2.3f -> 0.88f
            aspect >= 2.05f -> 0.92f
            else -> 1f
        }
        hudScale = (baseScale * tallFoldCompaction).coerceIn(0.82f, 1.24f)
        hudChipTextPx = resources.getDimension(R.dimen.bp_hud_mode_size) * hudScale

        val reservedRatio = when {
            shortDp >= 840f -> 0.2f
            shortDp >= 720f -> 0.195f
            shortDp >= 600f -> 0.19f
            aspect >= 2.3f -> 0.155f
            aspect >= 2.0f -> 0.172f
            else -> 0.21f
        }
        val reservedHeightDp = (heightDp * reservedRatio)
            .coerceIn(108f, if (shortDp >= 720f) 210f else 184f)
        val hudParams = binding.hudContainer.layoutParams as ConstraintLayout.LayoutParams
        val targetHeightPx = dp(reservedHeightDp)
        if (hudParams.height != targetHeightPx) {
            hudParams.height = targetHeightPx
            binding.hudContainer.layoutParams = hudParams
        }

        val scoreSize = resources.getDimension(R.dimen.bp_hud_score_size) * hudScale
        val statSize = resources.getDimension(R.dimen.bp_hud_stat_size) * hudScale
        val modeSize = resources.getDimension(R.dimen.bp_hud_mode_size) * hudScale
        val bannerSize = resources.getDimension(R.dimen.bp_hud_banner_size) * hudScale
        binding.hudScore.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, scoreSize)
        binding.hudLives.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, statSize)
        binding.hudTime.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, statSize)
        binding.hudLevel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, statSize)
        binding.hudMeta.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, modeSize)
        binding.hudShieldLabel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, modeSize)
        binding.hudFps.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, modeSize)
        binding.hudLevelBanner.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, bannerSize)

        val actionMin = (44f * hudScale).coerceIn(38f, 60f)
        binding.buttonPause.minimumWidth = dp(actionMin)
        binding.buttonPause.minimumHeight = dp(actionMin)
        binding.buttonLaser.minimumWidth = dp((76f * hudScale).coerceIn(64f, 104f))
        binding.buttonLaser.minimumHeight = dp((42f * hudScale).coerceIn(36f, 56f))

        val shieldWidthDp = (shortDp * 0.24f).coerceIn(116f, 186f)
        val shieldParams = binding.hudShieldBar.layoutParams as android.widget.LinearLayout.LayoutParams
        shieldParams.width = dp(shieldWidthDp)
        binding.hudShieldBar.layoutParams = shieldParams
        } finally {
            hudResizingInProgress = false
        }
    }

    private fun updateHudMeta() {
        val parts = mutableListOf<String>()
        if (currentModeLabel.isNotBlank()) parts.add(currentModeLabel)
        if (currentJourneyLabel.isNotBlank()) parts.add(currentJourneyLabel)
        if (currentMode == GameMode.ZEN) {
            parts.add(getString(R.string.label_zen_flow))
        } else {
            parts.add(getString(R.string.label_xp_format, currentXpTotal))
        }
        val modeSummary = statusSummaryForHud()
        if (!modeSummary.isNullOrBlank()) {
            parts.add(modeSummary)
        }
        val comboLabel = getString(R.string.label_combo_format, currentCombo)
        if (currentCombo >= 2 && (modeSummary == null || !modeSummary.contains(comboLabel))) {
            parts.add(comboLabel)
        }
        binding.hudMeta.text = parts.joinToString(" • ")
    }

    private fun statusSummaryForHud(): String? {
        val summary = currentPowerupSummary.trim()
        if (summary.isBlank() || summary == getString(R.string.label_powerups_none)) return null
        return when (currentMode) {
            GameMode.VOLLEY,
            GameMode.TUNNEL,
            GameMode.SURVIVAL -> summary
            else -> null
        }
    }

    private fun updateJourneyLabel(level: Int) {
        val chapter = ProgressionManager.chapterForLevel(level)
        val stage = ProgressionManager.stageForLevel(level)
        currentJourneyLabel = getString(R.string.label_journey_format, chapter, stage)
    }

    private fun updateShieldVisibility(show: Boolean) {
        binding.hudShieldRow.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun applyModeHud(mode: GameMode) {
        val zen = mode == GameMode.ZEN
        binding.hudScore.visibility = if (zen) View.GONE else View.VISIBLE
        binding.hudLives.visibility = if (zen) View.GONE else View.VISIBLE
        binding.hudTime.visibility = if (zen) View.GONE else View.VISIBLE
        if (zen) {
            currentPowerupSummary = getString(R.string.label_zen_flow)
        }
    }

    private fun updateLaserButton(status: List<PowerupStatus>) {
        val hasLaser = status.any { it.type == PowerUpType.LASER }
        laserActive = hasLaser
        if (!hasLaser) {
            laserCooldownEndMs = 0L
            laserCooldownRunnable?.let { binding.buttonLaser.removeCallbacks(it) }
            binding.buttonLaser.text = getString(R.string.label_fire)
            binding.buttonLaser.isEnabled = true
            binding.buttonLaser.alpha = 1f
        }
        binding.buttonLaser.visibility = if (hasLaser) View.VISIBLE else View.GONE
    }

    private fun playGameFade() {
        val overlay = binding.gameFadeOverlay
        overlay.alpha = 1f
        overlay.visibility = View.VISIBLE
        overlay.animate()
            .alpha(0f)
            .setDuration(UiMotion.OVERLAY_IN_DURATION)
            .setInterpolator(UiMotion.EMPHASIS_OUT)
            .withEndAction { overlay.visibility = View.GONE }
            .start()
    }

    private fun animateEndStats(summary: GameSummary, title: String) {
        binding.endTitle.text = title
        endStatsAnimator?.cancel()
        val animator = android.animation.ValueAnimator.ofInt(0, summary.score)
        endStatsAnimator = animator
        animator.duration = 700
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            binding.endStats.text = getString(R.string.label_score_level_format, value, summary.level)
        }
        animator.start()
    }

    private fun startLaserCooldown(seconds: Float) {
        val durationMs = (seconds * 1000f).toLong().coerceAtLeast(100L)
        laserCooldownEndMs = System.currentTimeMillis() + durationMs
        binding.buttonLaser.isEnabled = false
        binding.buttonLaser.alpha = 0.6f
        laserCooldownRunnable?.let { binding.buttonLaser.removeCallbacks(it) }
        val runner = Runnable { updateLaserCooldown() }
        laserCooldownRunnable = runner
        binding.buttonLaser.post(runner)
    }

    private fun updateLaserCooldown() {
        val remainingMs = laserCooldownEndMs - System.currentTimeMillis()
        if (!laserActive || remainingMs <= 0L) {
            binding.buttonLaser.text = getString(R.string.label_fire)
            binding.buttonLaser.isEnabled = true
            binding.buttonLaser.alpha = 1f
            return
        }
        val remaining = remainingMs / 1000f
        binding.buttonLaser.text = getString(R.string.label_laser_cooldown, remaining)
        val runner = laserCooldownRunnable
        if (runner != null) {
            binding.buttonLaser.postDelayed(runner, 60L)
        }
    }

    private fun scheduleHudUpdate() {
        if (!hudUpdateQueued.compareAndSet(false, true)) return
        binding.root.postOnAnimation {
            hudUpdateQueued.set(false)
            pendingScore?.let {
                binding.hudScore.text = getString(R.string.label_score_format, it)
                pendingScore = null
            }
            pendingLives?.let {
                if (config.mode == GameMode.VOLLEY) {
                    binding.hudLives.text = getString(R.string.label_volley_balls_format, it)
                } else {
                    binding.hudLives.text = getString(R.string.label_lives_format, it)
                }
                pendingLives = null
            }
            pendingVolleyBalls?.let {
                if (config.mode == GameMode.VOLLEY) {
                    binding.hudLives.text = getString(R.string.label_volley_balls_format, it)
                }
                pendingVolleyBalls = null
            }
            val fps = pendingFps
            if (fps != null && config.settings.showFpsCounter) {
                binding.hudFps.text = getString(R.string.label_fps_format, fps)
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
            PowerUpType.SHRINK -> "SHRK"
            PowerUpType.SLOW -> "SLOW"
            PowerUpType.OVERDRIVE -> "FAST"
            PowerUpType.FIREBALL -> "FIRE"
            PowerUpType.MAGNET -> "MAG"
            PowerUpType.GRAVITY_WELL -> "GRAV"
            PowerUpType.BALL_SPLITTER -> "SPLIT"
            PowerUpType.FREEZE -> "FRZ"
            PowerUpType.PIERCE -> "PRC"
            PowerUpType.RICOCHET -> "RICO"
            PowerUpType.TIME_WARP -> "WARP"
            PowerUpType.DOUBLE_SCORE -> "2X"
        }
    }

    private fun dp(value: Int): Int = dp(value.toFloat())

    private fun dp(value: Float): Int {
        if (value <= 0f) return 0
        return (value * resources.displayMetrics.density).toInt().coerceAtLeast(1)
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_DEBUG_POWERUP = "extra_debug_powerup"
        const val EXTRA_DEBUG_AUTOPLAY = "extra_debug_autoplay"
        const val EXTRA_DEBUG_AUTOPLAY_SECONDS = "extra_debug_autoplay_seconds"
    }
}
