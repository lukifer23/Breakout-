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
    private var debugProgressionProbeSession = false
    private var debugAutoPlayStopRunnable: Runnable? = null
    private var hudResizeRunnable: Runnable? = null
    private val hudResizeDebounceMs = 120L
    private var hudResizingInProgress = false
    private var levelAdvanceRecoveryRunnable: Runnable? = null
    private var runStatsRecorded = false
    private var runSnapshotCaptureInFlight = false
    private val overlayAnimationTokens = mutableMapOf<Int, Int>()
    private var bannerAnimationToken = 0
    private var hudMetaPulseToken = 0
    private var shieldPulseToken = 0
    private var shieldLabelFlashToken = 0
    private var fadeAnimationToken = 0
    private var tipBannerRunnable: Runnable? = null
    private var queuedTipMessage: String? = null
    private var lastTipMessage: String = ""
    private var lastTipTimestampMs: Long = 0L
    private val tipMinGapMs = 900L
    private val tipDuplicateSuppressMs = 2800L

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
        applyGameGestureExclusion()

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
            playGameFade()
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

        playGameFade()
    }

    override fun onResume() {
        super.onResume()
        applyResponsiveHudSizing()
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
        laserCooldownRunnable?.let { binding.buttonLaser.removeCallbacks(it) }
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
            applyResponsiveHudSizing()
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
            if (config.mode == GameMode.GOD) {
                binding.buttonSkipLevel?.visibility = View.VISIBLE
            } else {
                binding.buttonSkipLevel?.visibility = View.GONE
            }
        } else {
            hideOverlay(binding.pauseOverlay)
            binding.gameSurface.resumeGame()
            if (laserActive) {
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
            playGameFade()
        }
    }

    private fun exitToMenu() {
        recordRunSnapshotIfNeeded {
            levelAdvanceInProgress = false
            cancelLevelAdvanceRecovery()
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
                if (levelAdvanceInProgress) return
                levelAdvanceInProgress = true
                binding.buttonEndPrimary.isEnabled = false
                binding.buttonEndSecondary.isEnabled = false
                hideOverlay(binding.endOverlay)
                cancelLevelAdvanceRecovery()
                val recovery = Runnable {
                    if (!levelAdvanceInProgress || isFinishing || isDestroyed) return@Runnable
                    levelAdvanceInProgress = false
                    binding.buttonEndPrimary.isEnabled = true
                    binding.buttonEndSecondary.isEnabled = true
                    endOverlayState = EndOverlayState.LEVEL_COMPLETE
                    showOverlay(binding.endOverlay)
                    Log.w("GameActivity", "Level advance timed out; restored end overlay for retry")
                }
                levelAdvanceRecoveryRunnable = recovery
                binding.root.postDelayed(recovery, 1400L)
                if (!isFinishing && !isDestroyed) {
                    Log.d("GameActivity", "Advancing to next level (activity state: finishing=$isFinishing, destroyed=$isDestroyed)")
                    binding.gameSurface.nextLevel()
                    playGameFade()
                } else {
                    Log.w("GameActivity", "Cannot advance level - activity finishing or destroyed")
                    levelAdvanceInProgress = false
                    binding.buttonEndPrimary.isEnabled = true
                    binding.buttonEndSecondary.isEnabled = true
                    cancelLevelAdvanceRecovery()
                }
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
                applyResponsiveHudSizing()
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
            levelAdvanceInProgress = false
            cancelLevelAdvanceRecovery()
            binding.buttonEndPrimary.isEnabled = true
            binding.buttonEndSecondary.isEnabled = true
            binding.hudLevel.text = getString(R.string.label_level_format, level)
            updateJourneyLabel(level)
            updateHudMeta()
            if (debugAutoPlaySession) {
                Log.i("BreakoutAutoPlay", "event=level_start mode=${config.mode.name} level=$level")
            }
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
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            val updated = UnlockManager.unlockTheme(this, themeName)
            config = config.copy(unlocks = updated)
            binding.gameSurface.applyUnlocks(updated)
            showBanner(getString(R.string.label_theme_unlocked, themeName))
        }
    }

    override fun onCosmeticUnlocked(newTier: Int) {
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            val updated = UnlockManager.setCosmeticTier(this, newTier)
            config = config.copy(unlocks = updated)
            binding.gameSurface.applyUnlocks(updated)
            showBanner(getString(R.string.label_cosmetic_unlocked))
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
            binding.hudShieldLabel.text = getString(R.string.label_shield_percent, percent)
            if (current < lastShieldValue) {
                shieldPulseToken += 1
                val pulseToken = shieldPulseToken
                binding.hudShieldBar.animate().cancel()
                binding.hudShieldBar.scaleX = 1f
                binding.hudShieldBar.scaleY = 1f
                binding.hudShieldBar.animate()
                    .scaleX(UiMotion.SHIELD_PULSE_SCALE)
                    .scaleY(UiMotion.SHIELD_PULSE_SCALE)
                    .setDuration(UiMotion.PULSE_IN_DURATION)
                    .setInterpolator(UiMotion.EMPHASIS_OUT)
                    .withEndAction {
                        if (pulseToken != shieldPulseToken) return@withEndAction
                        binding.hudShieldBar.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(UiMotion.PULSE_OUT_DURATION)
                            .setInterpolator(UiMotion.EMPHASIS_OUT)
                            .withEndAction {
                                if (pulseToken != shieldPulseToken) return@withEndAction
                                binding.hudShieldBar.scaleX = 1f
                                binding.hudShieldBar.scaleY = 1f
                            }
                            .start()
                    }
                    .start()
                binding.hudShieldLabel.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.bp_red))
                shieldLabelFlashToken += 1
                val labelFlashToken = shieldLabelFlashToken
                binding.hudShieldLabel.postDelayed({
                    if (labelFlashToken != shieldLabelFlashToken) return@postDelayed
                    binding.hudShieldLabel.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.bp_white))
                }, 260L)
            }
            lastShieldValue = current
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
                showBanner(normalized)
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
                showBanner(queued)
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
                animateEndStats(summary, getString(R.string.label_game_over))
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
            currentXpTotal = ProgressionManager.addXp(this, ProgressionManager.xpForLevel(summary.level))
            updateHudMeta()
            if (debugProgressionProbeSession || config.mode == GameMode.ZEN || config.mode == GameMode.GOD) {
                endOverlayState = EndOverlayState.NONE
                hideOverlay(binding.endOverlay)
                showLevelBanner(summary.level + 1)
                advanceLevelWithAutoRecovery(summary)
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

    private fun advanceLevelWithAutoRecovery(summary: GameSummary) {
        if (isFinishing || isDestroyed) return
        levelAdvanceInProgress = true
        binding.buttonEndPrimary.isEnabled = false
        binding.buttonEndSecondary.isEnabled = false
        cancelLevelAdvanceRecovery()
        if (debugAutoPlaySession) {
            Log.i("BreakoutAutoPlay", "event=next_level_request mode=${config.mode.name} from_level=${summary.level} target_level=${summary.level + 1} source=auto")
        }

        val fallback = Runnable {
            if (!levelAdvanceInProgress || isFinishing || isDestroyed) return@Runnable
            Log.e("GameActivity", "Auto level advance failed; restoring manual next-level overlay")
            if (debugAutoPlaySession) {
                Log.i("BreakoutAutoPlay", "event=next_level_fallback mode=${config.mode.name} from_level=${summary.level} target_level=${summary.level + 1}")
            }
            levelAdvanceInProgress = false
            binding.buttonEndPrimary.isEnabled = true
            binding.buttonEndSecondary.isEnabled = true
            endOverlayState = EndOverlayState.LEVEL_COMPLETE
            binding.endTitle.text = getString(R.string.label_level_complete)
            animateEndStats(summary, getString(R.string.label_level_complete))
            binding.buttonEndPrimary.text = getString(R.string.label_next_level)
            showOverlay(binding.endOverlay)
        }
        levelAdvanceRecoveryRunnable = fallback
        binding.root.postDelayed(fallback, 1400L)
        binding.gameSurface.nextLevel()
        playGameFade()
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
        view.animate().cancel()
        view.visibility = View.VISIBLE
        view.alpha = 0f
        view.scaleX = UiMotion.OVERLAY_ENTER_SCALE
        view.scaleY = UiMotion.OVERLAY_ENTER_SCALE
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(UiMotion.OVERLAY_IN_DURATION)
            .setInterpolator(UiMotion.EMPHASIS_OUT)
            .withEndAction {
                if (!isOverlayAnimationTokenCurrent(view, token)) return@withEndAction
                view.alpha = 1f
                view.scaleX = 1f
                view.scaleY = 1f
            }
            .start()
    }

    private fun showLevelBanner(level: Int) {
        showBanner(getString(R.string.label_level_format, level))
    }

    private fun showBanner(message: String) {
        val banner = binding.hudLevelBanner
        bannerAnimationToken += 1
        val token = bannerAnimationToken
        banner.text = message
        banner.animate().cancel()
        banner.visibility = View.VISIBLE
        banner.alpha = 0f
        banner.scaleX = UiMotion.BANNER_ENTER_SCALE
        banner.scaleY = UiMotion.BANNER_ENTER_SCALE
        banner.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(UiMotion.BANNER_IN_DURATION)
            .setInterpolator(UiMotion.EMPHASIS_OUT)
            .withEndAction {
                if (token != bannerAnimationToken) return@withEndAction
                banner.animate()
                    .alpha(0f)
                    .scaleX(UiMotion.BANNER_EXIT_SCALE)
                    .scaleY(UiMotion.BANNER_EXIT_SCALE)
                    .setStartDelay(UiMotion.BANNER_HOLD_DURATION)
                    .setDuration(UiMotion.BANNER_OUT_DURATION)
                    .setInterpolator(UiMotion.EMPHASIS_OUT)
                    .withEndAction {
                        if (token != bannerAnimationToken) return@withEndAction
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
        hudMetaPulseToken += 1
        val token = hudMetaPulseToken
        meta.animate().cancel()
        meta.scaleX = 1f
        meta.scaleY = 1f
        meta.animate()
            .scaleX(UiMotion.HUD_PULSE_SCALE)
            .scaleY(UiMotion.HUD_PULSE_SCALE)
            .setDuration(UiMotion.PULSE_IN_DURATION)
            .setInterpolator(UiMotion.EMPHASIS_OUT)
            .withEndAction {
                if (token != hudMetaPulseToken) return@withEndAction
                meta.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(UiMotion.PULSE_OUT_DURATION)
                    .setInterpolator(UiMotion.EMPHASIS_OUT)
                    .withEndAction {
                        if (token != hudMetaPulseToken) return@withEndAction
                        meta.scaleX = 1f
                        meta.scaleY = 1f
                    }
                    .start()
            }
            .start()
    }

    private fun hideOverlay(view: View) {
        if (view.visibility != View.VISIBLE) return
        val token = nextOverlayAnimationToken(view)
        view.animate().cancel()
        view.animate()
            .alpha(0f)
            .scaleX(UiMotion.OVERLAY_EXIT_SCALE)
            .scaleY(UiMotion.OVERLAY_EXIT_SCALE)
            .setDuration(UiMotion.OVERLAY_OUT_DURATION)
            .setInterpolator(UiMotion.EMPHASIS_OUT)
            .withEndAction {
                if (!isOverlayAnimationTokenCurrent(view, token)) return@withEndAction
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
        val metrics = resources.displayMetrics
        val density = metrics.density.coerceAtLeast(1f)
        val availableWidthPx = when {
            binding.hudContainer.width > 0 -> binding.hudContainer.width
            binding.root.width > 0 -> binding.root.width
            else -> metrics.widthPixels
        }
        val availableWidthDp = (availableWidthPx / density).coerceAtLeast(0f)
        val estimatedChipWidthDp = (72f * hudScale).coerceAtLeast(54f)
        val widthBoundLimit = (availableWidthDp / estimatedChipWidthDp).toInt().coerceIn(1, 6)
        val baseLimit = when {
            availableWidthDp < 340f -> 2
            availableWidthDp < 430f -> 3
            availableWidthDp < 620f -> 4
            else -> 5
        }
        val maxVisible = minOf(baseLimit, widthBoundLimit).coerceAtLeast(1)
        val visibleItems = status.take(maxVisible)
        visibleItems.forEach { item ->
            container.addView(buildPowerupChip(item))
        }
        val overflow = status.size - visibleItems.size
        if (overflow > 0) {
            container.addView(buildOverflowChip(overflow))
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

    private fun buildOverflowChip(overflowCount: Int): android.widget.TextView {
        val chip = android.widget.TextView(this)
        val chipScale = hudScale.coerceIn(0.82f, 1.24f)
        val chipTextSize = if (hudChipTextPx > 0f) hudChipTextPx else resources.getDimension(R.dimen.bp_hud_mode_size)
        chip.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, chipTextSize)
        chip.setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
        chip.setSingleLine(true)
        chip.text = getString(R.string.label_powerup_overflow_format, overflowCount)
        chip.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.bp_white))
        chip.setPadding(
            dp(10f * chipScale),
            dp(6f * chipScale),
            dp(10f * chipScale),
            dp(6f * chipScale)
        )
        chip.letterSpacing = 0.02f

        val stroke = androidx.core.content.ContextCompat.getColor(this, R.color.bp_line)
        val drawable = android.graphics.drawable.GradientDrawable()
        drawable.cornerRadius = dp(14f * chipScale).toFloat()
        drawable.setColor(ColorUtils.setAlphaComponent(stroke, 38))
        drawable.setStroke(dp(1f.coerceAtLeast(0.9f * chipScale)), ColorUtils.setAlphaComponent(stroke, 180))
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
        // Prevent re-entry during layout changes
        if (hudResizingInProgress) return
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
            val tabletClass = shortDp >= 600f
            val wideSlate = tabletClass && aspect <= 1.85f
            val largeSlate = wideSlate && shortDp >= 840f

            val baseScale = when {
                shortDp >= 840f -> 1.25f // Increased for large tablets
                shortDp >= 720f -> 1.15f
                shortDp >= 600f -> 1.1f
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
            // Relaxed compaction for slates to utilize the screen real estate
            val slateCompaction = when {
                largeSlate -> 1.05f 
                wideSlate && shortDp >= 720f -> 1.0f
                wideSlate -> 0.98f
                else -> 1f
            }
            hudScale = (baseScale * tallFoldCompaction * slateCompaction).coerceIn(0.82f, 1.35f)
            hudChipTextPx = resources.getDimension(R.dimen.bp_hud_mode_size) * hudScale

            // Increased reserved ratios for Slates to prevent cramping
            val reservedRatio = when {
                largeSlate -> 0.16f // Was 0.132f
                wideSlate && shortDp >= 720f -> 0.165f // Was 0.138f
                wideSlate -> 0.17f // Was 0.144f
                shortDp >= 840f && aspect < 1.45f -> 0.17f
                shortDp >= 840f -> 0.172f
                shortDp >= 720f && aspect < 1.45f -> 0.175f
                shortDp >= 720f -> 0.168f
                shortDp >= 600f && aspect < 1.5f -> 0.175f
                shortDp >= 600f -> 0.165f
                aspect >= 2.3f -> 0.155f
                aspect >= 2.0f -> 0.172f
                else -> 0.21f
            }
            val reservedMaxDp = when {
                largeSlate -> 192f // Was 168f
                wideSlate -> 192f // Was 176f
                shortDp >= 720f -> 194f
                else -> 180f
            }
            val reservedMinDp = when {
                aspect >= 2.3f -> 84f
                aspect >= 2.0f -> 88f
                shortDp <= 380f -> 88f
                shortDp <= 430f -> 92f
                wideSlate -> 108f // Was 94f
                else -> 98f
            }
            val reservedHeightDp = (heightDp * reservedRatio)
                .coerceIn(reservedMinDp, reservedMaxDp)
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

            val rowPadding = dp((10f * hudScale).coerceIn(8f, if (wideSlate) 14f else 16f))
            binding.hudRow.setPadding(rowPadding, rowPadding, rowPadding, rowPadding)

            val scoreParams = binding.hudScore.layoutParams as ConstraintLayout.LayoutParams
            scoreParams.marginEnd = dp((10f * hudScale).coerceIn(8f, if (wideSlate) 15f else 18f))
            binding.hudScore.layoutParams = scoreParams

            val statGap = dp((14f * hudScale).coerceIn(10f, if (wideSlate) 20f else 24f))
            val timeParams = binding.hudTime.layoutParams as android.widget.LinearLayout.LayoutParams
            timeParams.marginStart = statGap
            binding.hudTime.layoutParams = timeParams
            val levelParams = binding.hudLevel.layoutParams as android.widget.LinearLayout.LayoutParams
            levelParams.marginStart = statGap
            binding.hudLevel.layoutParams = levelParams

            val statusTopMargin = dp(
                (6f * hudScale).coerceIn(
                    if (wideSlate) 3.5f else 4f,
                    if (wideSlate) 8f else 10f
                )
            )
            val statusParams = binding.hudStatusRow.layoutParams as android.widget.LinearLayout.LayoutParams
            statusParams.topMargin = statusTopMargin
            binding.hudStatusRow.layoutParams = statusParams
            val powerupsParams = binding.hudPowerups.layoutParams as android.widget.LinearLayout.LayoutParams
            powerupsParams.topMargin = statusTopMargin
            binding.hudPowerups.layoutParams = powerupsParams
            val chipsParams = binding.hudPowerupChips.layoutParams as android.widget.LinearLayout.LayoutParams
            chipsParams.bottomMargin = dp((4f * hudScale).coerceIn(2f, if (wideSlate) 6f else 8f))
            binding.hudPowerupChips.layoutParams = chipsParams
            val bannerParams = binding.hudLevelBanner.layoutParams as android.widget.LinearLayout.LayoutParams
            bannerParams.topMargin = dp(
                (10f * hudScale).coerceIn(
                    if (wideSlate) 5f else 6f,
                    if (wideSlate) 12f else 14f
                )
            )
            binding.hudLevelBanner.layoutParams = bannerParams

            val actionMin = if (wideSlate) {
                (42f * hudScale).coerceIn(38f, 56f)
            } else {
                (44f * hudScale).coerceIn(38f, 60f)
            }
            binding.buttonPause.minimumWidth = dp(actionMin)
            binding.buttonPause.minimumHeight = dp(actionMin)
            binding.buttonPause.iconSize = dp((22f * hudScale).coerceIn(18f, if (wideSlate) 28f else 30f))
            val laserWidthBase = if (wideSlate) 72f else 76f
            binding.buttonLaser.minimumWidth = dp(
                (laserWidthBase * hudScale).coerceIn(62f, if (wideSlate) 98f else 104f)
            )
            binding.buttonLaser.minimumHeight = dp(
                (42f * hudScale).coerceIn(34f, if (wideSlate) 52f else 56f)
            )
            val laserMargin = dp((16f * hudScale).coerceIn(10f, if (wideSlate) 20f else 24f))
            val laserParams = binding.buttonLaser.layoutParams as ConstraintLayout.LayoutParams
            laserParams.marginStart = laserMargin
            laserParams.marginEnd = laserMargin
            val topAnchored =
                laserParams.topToBottom != ConstraintLayout.LayoutParams.UNSET &&
                    laserParams.bottomToTop == ConstraintLayout.LayoutParams.UNSET &&
                    laserParams.bottomToBottom == ConstraintLayout.LayoutParams.UNSET
            if (topAnchored) {
                laserParams.topMargin = laserMargin
                laserParams.bottomMargin = 0
            } else {
                laserParams.bottomMargin = laserMargin
            }
            binding.buttonLaser.layoutParams = laserParams

            val shieldRatio = when {
                largeSlate -> 0.2f
                wideSlate -> 0.215f
                aspect < 1.45f -> 0.22f
                else -> 0.24f
            }
            val shieldWidthDp = (shortDp * shieldRatio).coerceIn(112f, if (wideSlate) 176f else 186f)
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
        fadeAnimationToken += 1
        val token = fadeAnimationToken
        overlay.animate().cancel()
        overlay.alpha = 1f
        overlay.visibility = View.VISIBLE
        overlay.animate()
            .alpha(0f)
            .setDuration(UiMotion.OVERLAY_IN_DURATION)
            .setInterpolator(UiMotion.EMPHASIS_OUT)
            .withEndAction {
                if (token != fadeAnimationToken) return@withEndAction
                overlay.visibility = View.GONE
            }
            .start()
    }

    private fun animateEndStats(summary: GameSummary, title: String) {
        binding.endTitle.text = title
        endStatsAnimator?.cancel()
        val timeText = formatDuration(summary.durationSeconds)
        binding.endStats.text = getString(
            R.string.label_end_stats_format,
            0,
            summary.level,
            timeText,
            summary.bricksBroken,
            summary.livesLost
        )
        val animator = android.animation.ValueAnimator.ofInt(0, summary.score)
        endStatsAnimator = animator
        animator.duration = UiMotion.scoreCountDuration(summary.score)
        animator.interpolator = UiMotion.LINEAR
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            binding.endStats.text = getString(
                R.string.label_end_stats_format,
                value,
                summary.level,
                timeText,
                summary.bricksBroken,
                summary.livesLost
            )
        }
        animator.start()
    }

    private fun formatDuration(seconds: Int): String {
        val clamped = seconds.coerceAtLeast(0)
        val minutes = clamped / 60
        val remainingSeconds = clamped % 60
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
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
            pendingFps = null
            if (fps != null && config.settings.showFpsCounter) {
                binding.hudFps.text = getString(R.string.label_fps_format, fps)
                binding.hudFps.visibility = View.VISIBLE
                if (binding.hudPowerups.visibility != View.VISIBLE) {
                    binding.hudPowerups.visibility = View.VISIBLE
                }
            } else if (!config.settings.showFpsCounter) {
                binding.hudFps.visibility = View.GONE
                if (binding.hudPowerupChips.childCount == 0) {
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
