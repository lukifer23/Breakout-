package com.breakoutplus

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Rect
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
    private lateinit var hud: GameHudController
    private var endOverlayState: EndOverlayState = EndOverlayState.NONE
    private var maxInsetTop = 0
    private var maxInsetBottom = 0
    private var baseSurfaceBottomMargin = 0
    private var lastHudAvailWidthPx: Int = -1
    private var lastHudAvailHeightPx: Int = -1
    private var levelAdvanceInProgress = false
    private var debugAutoPlaySession = false
    private var debugProgressionProbeSession = false
    private var debugAutoPlayStopRunnable: Runnable? = null
    private var hudResizeRunnable: Runnable? = null
    private val hudResizeDebounceMs = 120L
    private var hudResizingInProgress = false
    private var levelAdvanceRecoveryRunnable: Runnable? = null
    private var runStatsRecorded = false
    private var runSnapshotCaptureInFlight = false
    private val overlayAnimationTokens = mutableMapOf<Int, Int>()
    private var tipBannerRunnable: Runnable? = null
    private var queuedTipMessage: String? = null
    private var lastTipMessage: String = ""
    private var lastTipTimestampMs: Long = 0L
    private val tipMinGapMs = 900L
    private val tipDuplicateSuppressMs = 2800L
    private val manualLevelAdvanceTimeoutMs = 1800L
    private val autoLevelAdvanceTimeoutMs = 2600L
    private val autoLevelAdvanceRetryTimeoutMs = 1600L
    private val maxAutoLevelAdvanceRetries = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hud = GameHudController(this, binding)
        setFoldAwareRoot(binding.root)
        configureSystemUi()
        observeViewportChanges()
        hud.applyResponsiveHudSizing()
        baseSurfaceBottomMargin =
            (binding.gameSurface.layoutParams as ConstraintLayout.LayoutParams).bottomMargin
        applyWindowInsets()
        applyGameGestureExclusion()

        val modeName = intent.getStringExtra(EXTRA_MODE)
        val mode = runCatching { GameMode.valueOf(modeName ?: "CLASSIC") }.getOrDefault(GameMode.CLASSIC)
        val settings = SettingsManager.load(this)
        val dailyChallenges = DailyChallengeStore.load(this)
        val unlocks = UnlockManager.load(this)
        config = GameConfig(mode, settings, dailyChallenges, unlocks)
        hud.currentMode = mode
        hud.currentXpTotal = ProgressionManager.loadXp(this)
        hud.updateJourneyLabel(1)
        hud.applyModeHud(mode)

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
            debugProgressionProbeSession = intent.getBooleanExtra(EXTRA_DEBUG_PROGRESSION_PROBE, false)
            if (debugAutoPlaySession) {
                val runSeconds = intent.getIntExtra(EXTRA_DEBUG_AUTOPLAY_SECONDS, 0).coerceIn(0, 600)
                binding.gameSurface.setDebugAutoPlay(true)
                if (debugProgressionProbeSession) {
                    binding.gameSurface.setDebugProgressionProbe(true)
                }
                if (runSeconds > 0) {
                    val stopRunnable = Runnable {
                        binding.gameSurface.setDebugAutoPlay(false)
                        if (debugProgressionProbeSession) {
                            binding.gameSurface.setDebugProgressionProbe(false)
                        }
                    }
                    debugAutoPlayStopRunnable = stopRunnable
                    binding.gameSurface.postDelayed(stopRunnable, runSeconds * 1000L)
                }
                Log.i("BreakoutAutoPlay", "event=session_start mode=${mode.name} seconds=${runSeconds}")
                if (debugProgressionProbeSession) {
                    Log.i("BreakoutAutoPlay", "event=progression_probe_enabled mode=${mode.name}")
                }
            } else if (debugProgressionProbeSession) {
                binding.gameSurface.setDebugProgressionProbe(true)
                Log.i("BreakoutAutoPlay", "event=progression_probe_enabled mode=${mode.name}")
            }
        }

        binding.buttonPause.setOnClickListener { showPause(true) }
        binding.buttonResume.setOnClickListener { showPause(false) }
        binding.buttonSkipLevel?.setOnClickListener {
            // Keep engine state paused for GOD-mode force-skip validation.
            binding.gameSurface.nextLevel()
            showPause(false)
            hud.playGameFade()
        }
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

        // Keep debug automation sessions unblocked by tutorial overlays.
        if (settings.tipsEnabled && !debugAutoPlaySession && !debugProgressionProbeSession) {
            showTooltip()
        }

        hud.playGameFade()
    }

    override fun onResume() {
        super.onResume()
        hud.applyResponsiveHudSizing()
        applyGameGestureExclusion()
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
        cancelLevelAdvanceRecovery()
        config.dailyChallenges?.let { DailyChallengeStore.save(this, it) }
        hud.clearLaserCooldown()
        debugAutoPlayStopRunnable?.let { binding.gameSurface.removeCallbacks(it) }
        debugAutoPlayStopRunnable = null
        hudResizeRunnable?.let { binding.root.removeCallbacks(it) }
        hudResizeRunnable = null
        tipBannerRunnable?.let { binding.root.removeCallbacks(it) }
        tipBannerRunnable = null
        queuedTipMessage = null
        if (debugAutoPlaySession) {
            binding.gameSurface.setDebugAutoPlay(false)
        }
        if (debugProgressionProbeSession) {
            binding.gameSurface.setDebugProgressionProbe(false)
        }
        super.onPause()
    }

    override fun onDestroy() {
        tipBannerRunnable?.let { binding.root.removeCallbacks(it) }
        tipBannerRunnable = null
        queuedTipMessage = null
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.root.post {
            hud.applyResponsiveHudSizing()
            applyGameGestureExclusion()
        }
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
            if (config.mode == GameMode.GOD || config.mode == GameMode.ZEN) {
                binding.buttonSkipLevel?.visibility = View.VISIBLE
            } else {
                binding.buttonSkipLevel?.visibility = View.GONE
            }
        } else {
            hideOverlay(binding.pauseOverlay)
            binding.gameSurface.resumeGame()
            if (hud.laserActive) {
                binding.buttonLaser.visibility = View.VISIBLE
            }
        }
    }

    private fun restartGame() {
        recordRunSnapshotIfNeeded {
            levelAdvanceInProgress = false
            cancelLevelAdvanceRecovery()
            hideOverlay(binding.endOverlay)
            hideOverlay(binding.pauseOverlay)
            hideOverlay(binding.tooltipOverlay)
            endOverlayState = EndOverlayState.NONE
            binding.buttonEndPrimary.isEnabled = true
            binding.buttonEndSecondary.isEnabled = true
            binding.gameSurface.resumeGame()
            binding.gameSurface.restartGame()
            runStatsRecorded = false
            hud.playGameFade()
        }
    }

    private fun exitToMenu() {
        recordRunSnapshotIfNeeded {
            levelAdvanceInProgress = false
            cancelLevelAdvanceRecovery()
            playCloseTransition(R.anim.fade_in, R.anim.fade_out)
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
            finish()
        }
    }

    private fun handleEndPrimary() {
        when (endOverlayState) {
            EndOverlayState.LEVEL_COMPLETE -> {
                endOverlayState = EndOverlayState.NONE
                hideOverlay(binding.endOverlay)
                requestLevelAdvance(
                    source = "manual",
                    timeoutMs = manualLevelAdvanceTimeoutMs,
                    restoreOverlayOnTimeout = true,
                    fallbackSummary = null
                )
                endOverlayState = EndOverlayState.NONE
            }
            else -> restartGame()
        }
    }

    private fun applyHandedness(leftHanded: Boolean) {
        val params = binding.buttonPause.layoutParams as ConstraintLayout.LayoutParams
        if (leftHanded) {
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET
        } else {
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            params.startToStart = ConstraintLayout.LayoutParams.UNSET
        }
        binding.buttonPause.layoutParams = params

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
            binding.root.post { applyGameGestureExclusion() }
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
            val runnable = Runnable {
                hud.applyResponsiveHudSizing()
                applyGameGestureExclusion()
            }
            hudResizeRunnable = runnable
            binding.root.postDelayed(runnable, hudResizeDebounceMs)
        }
    }

    private fun applyGameGestureExclusion() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val surface = binding.gameSurface
        val width = surface.width
        val height = surface.height
        if (width <= 0 || height <= 0) {
            surface.post { applyGameGestureExclusion() }
            return
        }
        val edgeWidth = dp(28f).coerceAtLeast(1).coerceAtMost(width / 3)
        val leftEdge = Rect(0, 0, edgeWidth, height)
        val rightEdge = Rect(width - edgeWidth, 0, width, height)
        surface.systemGestureExclusionRects = listOf(leftEdge, rightEdge)
    }

    override fun onScoreUpdated(score: Int) {
        hud.queueScoreUpdate(score)
    }

    override fun onLivesUpdated(lives: Int) {
        hud.queueLivesUpdate(lives)
    }

    override fun onVolleyBallsUpdated(volleyBalls: Int) {
        hud.queueVolleyBallsUpdate(volleyBalls)
    }

    override fun onTimeUpdated(secondsRemaining: Int) {
        runOnUiThread {
            if (hud.currentMode == GameMode.ZEN) {
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
                binding.hudTime.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.bp_hud_text))
            }
        }
    }

    override fun onLevelUpdated(level: Int) {
        runOnUiThread {
            levelAdvanceInProgress = false
            cancelLevelAdvanceRecovery()
            binding.buttonEndPrimary.isEnabled = true
            binding.buttonEndSecondary.isEnabled = true
            binding.hudLevel.text = getString(R.string.label_level_format, level)
            hud.updateJourneyLabel(level)
            hud.updateHudMeta()
            if (debugAutoPlaySession) {
                Log.i("BreakoutAutoPlay", "event=level_start mode=${config.mode.name} level=$level")
            }
        }
    }

    override fun onModeUpdated(mode: GameMode) {
        runOnUiThread {
            hud.currentMode = mode
            hud.currentModeLabel = mode.displayName
            hud.applyModeHud(mode)
            hud.updateHudMeta()
            hud.updateShieldVisibility(mode.invaders)
        }
    }

    override fun onPowerupStatus(status: String) {
        runOnUiThread {
            hud.currentPowerupSummary = status
            hud.updateHudMeta()
        }
    }

    override fun onPowerupsUpdated(status: List<PowerupStatus>, combo: Int) {
        runOnUiThread {
            val previousCount = hud.currentPowerupCount
            val previousCombo = hud.currentCombo
            hud.renderPowerupChips(status)
            hud.currentCombo = combo
            hud.currentPowerupCount = status.size
            val preserveModeSummary =
                hud.currentMode == GameMode.VOLLEY ||
                    hud.currentMode == GameMode.TUNNEL ||
                    hud.currentMode == GameMode.SURVIVAL ||
                    hud.currentMode == GameMode.ZEN
            if (!preserveModeSummary) {
                hud.currentPowerupSummary = if (status.isEmpty()) {
                    getString(R.string.label_powerups_none)
                } else {
                    resources.getQuantityString(R.plurals.label_powerups_active, status.size, status.size)
                }
            }
            hud.updateLaserButton(status)
            hud.updateHudMeta()
            if (status.size > previousCount || combo > previousCombo) {
                hud.pulseHudMeta()
            }
        }
    }

    override fun onLaserFired(cooldownSeconds: Float) {
        runOnUiThread {
            if (!hud.laserActive) return@runOnUiThread
            hud.startLaserCooldown(cooldownSeconds)
        }
    }

    override fun onThemeUnlocked(themeName: String) {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            val updated = UnlockManager.unlockTheme(this, themeName)
            config = config.copy(unlocks = updated)
            binding.gameSurface.applyUnlocks(updated)
            hud.showBanner(getString(R.string.label_theme_unlocked, themeName))
        }
    }

    override fun onCosmeticUnlocked(newTier: Int) {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            val updated = UnlockManager.setCosmeticTier(this, newTier)
            config = config.copy(unlocks = updated)
            binding.gameSurface.applyUnlocks(updated)
            hud.showBanner(getString(R.string.label_cosmetic_unlocked))
        }
    }

    override fun onFpsUpdate(fps: Int) {
        hud.queueFpsUpdate(fps)
    }

    override fun onShieldUpdated(current: Int, max: Int) {
        runOnUiThread {
            hud.onShieldUpdated(current, max)
        }
    }

    override fun onTip(message: String) {
        if (!config.settings.tipsEnabled) return
        val normalized = message.trim()
        if (normalized.isBlank()) return
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            if (binding.pauseOverlay.visibility == View.VISIBLE || binding.endOverlay.visibility == View.VISIBLE) {
                return@runOnUiThread
            }
            val now = System.currentTimeMillis()
            if (normalized == lastTipMessage && now - lastTipTimestampMs < tipDuplicateSuppressMs) {
                return@runOnUiThread
            }
            val sinceLast = now - lastTipTimestampMs
            if (sinceLast >= tipMinGapMs) {
                hud.showBanner(normalized)
                lastTipMessage = normalized
                lastTipTimestampMs = now
                queuedTipMessage = null
                tipBannerRunnable?.let { binding.root.removeCallbacks(it) }
                tipBannerRunnable = null
                return@runOnUiThread
            }
            queuedTipMessage = normalized
            val delayMs = (tipMinGapMs - sinceLast).coerceAtLeast(80L)
            tipBannerRunnable?.let { binding.root.removeCallbacks(it) }
            val runner = Runnable {
                val queued = queuedTipMessage ?: return@Runnable
                if (isFinishing || isDestroyed) return@Runnable
                val emitTime = System.currentTimeMillis()
                if (queued == lastTipMessage && emitTime - lastTipTimestampMs < tipDuplicateSuppressMs) {
                    queuedTipMessage = null
                    tipBannerRunnable = null
                    return@Runnable
                }
                hud.showBanner(queued)
                lastTipMessage = queued
                lastTipTimestampMs = emitTime
                queuedTipMessage = null
                tipBannerRunnable = null
            }
            tipBannerRunnable = runner
            binding.root.postDelayed(runner, delayMs)
        }
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
            runStatsRecorded = true
            if (debugAutoPlaySession) {
                Log.i(
                    "BreakoutAutoPlay",
                    "event=game_over mode=${config.mode.name} score=${summary.score} level=${summary.level} duration=${summary.durationSeconds} bricks=${summary.bricksBroken} lives_lost=${summary.livesLost}"
                )
            }
            val highScoreTimestamp = System.currentTimeMillis()
            // Check if this is a high score for the mode
            if (ScoreboardManager.isHighScoreForMode(
                    this,
                    config.mode.displayName,
                    summary.score,
                    summary.durationSeconds,
                    highScoreTimestamp
                )
            ) {
                // Show name input dialog
                showNameInputDialog(summary, highScoreTimestamp)
            } else {
                // Not a high score, just show game over screen
                binding.endTitle.text = getString(R.string.label_game_over)
                hud.animateEndStats(summary, getString(R.string.label_game_over))
                binding.buttonEndPrimary.text = getString(R.string.label_restart)
                showOverlay(binding.endOverlay)
            }
            config.dailyChallenges?.let { DailyChallengeStore.save(this, it) }
        }
    }

    private fun showNameInputDialog(summary: GameSummary, highScoreTimestamp: Long) {
        val dialog = android.app.Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_high_score, binding.root, false)
        dialog.setContentView(view)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9f).toInt().coerceAtMost(dp(600f)),
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
        }.coerceAtLeast(dp(80f)) // Minimum 80dp
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
            hud.animateEndStats(summary, getString(R.string.label_game_over))
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
                    timestamp = highScoreTimestamp
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
            if (config.mode != GameMode.ZEN) {
                hud.currentXpTotal = ProgressionManager.addXp(this, ProgressionManager.xpForLevel(summary.level))
                hud.updateHudMeta()
            }
            if (debugProgressionProbeSession || config.mode == GameMode.ZEN) {
                endOverlayState = EndOverlayState.NONE
                hideOverlay(binding.endOverlay)
                hud.showLevelBanner(summary.level + 1)
                advanceLevelWithAutoRecovery(summary)
                config.dailyChallenges?.let { DailyChallengeStore.save(this, it) }
                return@runOnUiThread
            }
            endOverlayState = EndOverlayState.LEVEL_COMPLETE
            binding.endTitle.text = getString(R.string.label_level_complete)
            hud.animateEndStats(summary, getString(R.string.label_level_complete))
            binding.buttonEndPrimary.text = getString(R.string.label_next_level)
            showOverlay(binding.endOverlay)
            config.dailyChallenges?.let { DailyChallengeStore.save(this, it) }
        }
    }

    private fun advanceLevelWithAutoRecovery(summary: GameSummary) {
        requestLevelAdvance(
            source = "auto",
            timeoutMs = autoLevelAdvanceTimeoutMs,
            restoreOverlayOnTimeout = true,
            fallbackSummary = summary
        )
    }

    private fun requestLevelAdvance(
        source: String,
        timeoutMs: Long,
        restoreOverlayOnTimeout: Boolean,
        fallbackSummary: GameSummary?
    ) {
        if (levelAdvanceInProgress || isFinishing || isDestroyed) return
        levelAdvanceInProgress = true
        binding.buttonEndPrimary.isEnabled = false
        binding.buttonEndSecondary.isEnabled = false
        cancelLevelAdvanceRecovery()

        if (debugAutoPlaySession) {
            val fromLevel = fallbackSummary?.level ?: binding.hudLevel.text.toString().filter { it.isDigit() }.toIntOrNull()
            val targetLevel = fromLevel?.plus(1)
            Log.i(
                "BreakoutAutoPlay",
                "event=next_level_request mode=${config.mode.name} from_level=${fromLevel ?: -1} target_level=${targetLevel ?: -1} source=$source"
            )
        }
        var retryAttempt = 0

        fun scheduleRecovery(waitMs: Long) {
            val recovery = Runnable {
                if (!levelAdvanceInProgress || isFinishing || isDestroyed) return@Runnable
                if (source == "auto" && retryAttempt < maxAutoLevelAdvanceRetries) {
                    retryAttempt += 1
                    if (debugAutoPlaySession) {
                        val level = fallbackSummary?.level ?: -1
                        Log.i(
                            "BreakoutAutoPlay",
                            "event=next_level_retry mode=${config.mode.name} from_level=$level target_level=${if (level > 0) level + 1 else -1} source=$source attempt=$retryAttempt"
                        )
                    }
                    scheduleRecovery(autoLevelAdvanceRetryTimeoutMs)
                    binding.gameSurface.nextLevel()
                    hud.playGameFade()
                    return@Runnable
                }

                levelAdvanceInProgress = false
                binding.buttonEndPrimary.isEnabled = true
                binding.buttonEndSecondary.isEnabled = true
                if (restoreOverlayOnTimeout) {
                    endOverlayState = EndOverlayState.LEVEL_COMPLETE
                    binding.endTitle.text = getString(R.string.label_level_complete)
                    binding.buttonEndPrimary.text = getString(R.string.label_next_level)
                    fallbackSummary?.let { hud.animateEndStats(it, getString(R.string.label_level_complete)) }
                    showOverlay(binding.endOverlay)
                }
                if (source == "auto") {
                    Log.e("GameActivity", "Auto level advance timed out; restored manual next-level overlay")
                } else {
                    Log.w("GameActivity", "Manual level advance timed out; restored end overlay for retry")
                }
                if (debugAutoPlaySession) {
                    val level = fallbackSummary?.level ?: -1
                    Log.i("BreakoutAutoPlay", "event=next_level_fallback mode=${config.mode.name} from_level=$level target_level=${if (level > 0) level + 1 else -1} source=$source")
                }
            }
            levelAdvanceRecoveryRunnable = recovery
            binding.root.postDelayed(recovery, waitMs)
        }

        scheduleRecovery(timeoutMs)
        binding.gameSurface.nextLevel()
        hud.playGameFade()
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

    private fun overlayTokenKey(view: View): Int {
        return if (view.id != View.NO_ID) view.id else System.identityHashCode(view)
    }

    private fun nextOverlayAnimationToken(view: View): Int {
        val key = overlayTokenKey(view)
        val next = (overlayAnimationTokens[key] ?: 0) + 1
        overlayAnimationTokens[key] = next
        return next
    }

    private fun isOverlayAnimationTokenCurrent(view: View, token: Int): Boolean {
        return overlayAnimationTokens[overlayTokenKey(view)] == token
    }

    private fun showOverlay(view: View) {
        val token = nextOverlayAnimationToken(view)
        UiMotion.animateOverlayIn(
            view = view,
            token = token,
            isCurrent = { isOverlayAnimationTokenCurrent(view, it) },
            onEnd = {
                if (view is android.view.ViewGroup && view.childCount > 1) {
                    UiMotion.staggerOverlayChildren(view, skipFirst = 1)
                }
            }
        )
    }

    private fun hideOverlay(view: View) {
        val token = nextOverlayAnimationToken(view)
        UiMotion.animateOverlayOut(
            view = view,
            token = token,
            isCurrent = { isOverlayAnimationTokenCurrent(view, it) }
        )
    }





    private fun cancelLevelAdvanceRecovery() {
        levelAdvanceRecoveryRunnable?.let { binding.root.removeCallbacks(it) }
        levelAdvanceRecoveryRunnable = null
    }

    private fun recordRunSnapshotIfNeeded(onComplete: () -> Unit) {
        if (runStatsRecorded || endOverlayState == EndOverlayState.GAME_OVER) {
            onComplete()
            return
        }
        if (runSnapshotCaptureInFlight) return
        runSnapshotCaptureInFlight = true
        val timeout = Runnable {
            if (!runSnapshotCaptureInFlight) return@Runnable
            runSnapshotCaptureInFlight = false
            onComplete()
        }
        binding.root.postDelayed(timeout, 220L)
        binding.gameSurface.captureSummary { summary ->
            if (!runSnapshotCaptureInFlight) return@captureSummary
            binding.root.removeCallbacks(timeout)
            if (!runStatsRecorded && summary != null && shouldRecordRunSummary(summary)) {
                LifetimeStatsManager.recordRun(this, summary)
                runStatsRecorded = true
            }
            runSnapshotCaptureInFlight = false
            onComplete()
        }
    }

    private fun shouldRecordRunSummary(summary: GameSummary): Boolean {
        return summary.score > 0 ||
            summary.level > 1 ||
            summary.durationSeconds >= 8 ||
            summary.bricksBroken > 0 ||
            summary.livesLost > 0
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
        const val EXTRA_DEBUG_PROGRESSION_PROBE = "extra_debug_progression_probe"
    }
}
