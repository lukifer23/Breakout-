package com.breakoutplus

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.core.content.ContextCompat
import com.breakoutplus.databinding.ActivityGameBinding
import com.breakoutplus.game.GameConfig
import com.breakoutplus.game.GameMode
import com.breakoutplus.game.GameSummary
import com.breakoutplus.game.PowerUpType
import com.breakoutplus.game.PowerupStatus
import java.util.concurrent.atomic.AtomicBoolean

class GameHudController(
    private val activity: GameActivity,
    private val binding: ActivityGameBinding
) {
    var config: GameConfig? = null
    var currentMode: GameMode = GameMode.CLASSIC
    var currentModeLabel: String = "Classic"
    var currentPowerupSummary: String = "Powerups: none"
    var currentCombo: Int = 0
    var currentPowerupCount: Int = 0
    var currentJourneyLabel: String = ""
    var currentXpTotal: Int = 0
    var laserActive: Boolean = false
    
    private var laserCooldownEndMs: Long = 0L
    private var laserCooldownRunnable: Runnable? = null
    private var lastShieldValue: Int = 0
    private var endStatsAnimator: android.animation.ValueAnimator? = null
    
    private val hudUpdateQueued = AtomicBoolean(false)
    @Volatile private var pendingScore: Int? = null
    @Volatile private var pendingLives: Int? = null
    @Volatile private var pendingFps: Int? = null
    @Volatile private var pendingVolleyBalls: Int? = null
    
    var hudScale: Float = 1f
    var hudChipTextPx: Float = 0f
    
    private var hudMetaPulseToken = 0
    private var shieldPulseToken = 0
    private var shieldLabelFlashToken = 0
    private var bannerAnimationToken = 0
    private var fadeAnimationToken = 0

    fun dp(value: Float): Int {
        if (value <= 0f) return 0
        return (value * activity.resources.displayMetrics.density).toInt().coerceAtLeast(1)
    }

    fun applyResponsiveHudSizing() {
        val metrics = activity.resources.displayMetrics
        if (metrics.density <= 0f) return
        val widthPx = (binding.root.width - binding.root.paddingLeft - binding.root.paddingRight)
            .takeIf { it > 0 } ?: metrics.widthPixels
        val heightPx = (binding.root.height - binding.root.paddingTop - binding.root.paddingBottom)
            .takeIf { it > 0 } ?: metrics.heightPixels
        val widthDp = widthPx / metrics.density
        val heightDp = heightPx / metrics.density
        val layoutClass = DeviceLayoutPolicy.classifyByDp(widthDp, heightDp)
        val shortDp = layoutClass.shortDp
        val aspect = layoutClass.aspectRatio
        val tabletClass = layoutClass.tabletClass
        val wideSlate = layoutClass.wideSlate
        val largeSlate = layoutClass.largeSlate

        val baseScale = when {
            shortDp >= 840f -> 1.16f
            shortDp >= 720f -> 1.1f
            shortDp >= 600f -> 1.04f
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
        val slateCompaction = when {
            largeSlate -> 0.92f
            wideSlate && shortDp >= 720f -> 0.94f
            wideSlate -> 0.92f
            else -> 1f
        }
        hudScale = (baseScale * tallFoldCompaction * slateCompaction).coerceIn(0.82f, 1.28f)
        hudChipTextPx = activity.resources.getDimension(R.dimen.bp_hud_mode_size) * hudScale

        val reservedRatio = when {
            largeSlate -> 0.155f
            wideSlate && shortDp >= 720f -> 0.158f
            wideSlate -> 0.160f
            shortDp >= 840f && aspect < 1.45f -> 0.164f
            shortDp >= 840f -> 0.168f
            shortDp >= 720f && aspect < 1.45f -> 0.172f
            shortDp >= 720f -> 0.166f
            shortDp >= 600f && aspect < 1.5f -> 0.174f
            shortDp >= 600f -> 0.168f
            aspect >= 2.3f -> 0.155f
            aspect >= 2.0f -> 0.172f
            else -> 0.21f
        }
        val reservedMaxDp = when {
            largeSlate -> 200f
            wideSlate -> 190f
            shortDp >= 720f -> 186f
            else -> 180f
        }
        val reservedMinDp = when {
            aspect >= 2.3f -> 84f
            aspect >= 2.0f -> 88f
            shortDp <= 380f -> 88f
            shortDp <= 430f -> 92f
            largeSlate -> 140f
            wideSlate -> 120f
            else -> 98f
        }
        val reservedHeightDp = (heightDp * reservedRatio).coerceIn(reservedMinDp, reservedMaxDp)
        val hudParams = binding.hudContainer.layoutParams as ConstraintLayout.LayoutParams
        val targetHeightPx = dp(reservedHeightDp)
        if (hudParams.height != targetHeightPx) {
            hudParams.height = targetHeightPx
            binding.hudContainer.layoutParams = hudParams
        }

        val scoreSize = activity.resources.getDimension(R.dimen.bp_hud_score_size) * hudScale
        val statSize = activity.resources.getDimension(R.dimen.bp_hud_stat_size) * hudScale
        val modeSize = activity.resources.getDimension(R.dimen.bp_hud_mode_size) * hudScale
        val bannerSize = activity.resources.getDimension(R.dimen.bp_hud_banner_size) * hudScale
        binding.hudScore.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, scoreSize)
        binding.hudLives.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, statSize)
        binding.hudTime.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, statSize)
        binding.hudLevel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, statSize)
        binding.hudMeta.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, modeSize)
        binding.hudShieldLabel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, modeSize)
        binding.hudFps.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, modeSize)
        binding.hudLevelBanner.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, bannerSize)

        val rowPadding = dp((10f * hudScale).coerceIn(8f, if (tabletClass) 13f else 16f))
        binding.hudRow.setPadding(rowPadding, rowPadding, rowPadding, rowPadding)

        val scoreParams = binding.hudScore.layoutParams as ConstraintLayout.LayoutParams
        scoreParams.marginEnd = dp((10f * hudScale).coerceIn(8f, if (tabletClass) 14f else 18f))
        binding.hudScore.layoutParams = scoreParams

        val statGap = dp((14f * hudScale).coerceIn(10f, if (tabletClass) 18f else 24f))
        val timeParams = binding.hudTime.layoutParams as android.widget.LinearLayout.LayoutParams
        timeParams.marginStart = statGap
        binding.hudTime.layoutParams = timeParams
        val levelParams = binding.hudLevel.layoutParams as android.widget.LinearLayout.LayoutParams
        levelParams.marginStart = statGap
        binding.hudLevel.layoutParams = levelParams

        val statusTopMargin = dp((6f * hudScale).coerceIn(if (wideSlate) 3.5f else 4f, if (wideSlate) 8f else 10f))
        val statusParams = binding.hudStatusRow.layoutParams as android.widget.LinearLayout.LayoutParams
        statusParams.topMargin = statusTopMargin
        binding.hudStatusRow.layoutParams = statusParams
        val powerupsParams = binding.hudPowerups.layoutParams as android.widget.LinearLayout.LayoutParams
        powerupsParams.topMargin = statusTopMargin
        binding.hudPowerups.layoutParams = powerupsParams
        val chipsParams = binding.hudPowerupChips.layoutParams as android.widget.LinearLayout.LayoutParams
        chipsParams.bottomMargin = dp((4f * hudScale).coerceIn(2f, if (tabletClass) 6f else 8f))
        binding.hudPowerupChips.layoutParams = chipsParams
        val bannerParams = binding.hudLevelBanner.layoutParams as android.widget.LinearLayout.LayoutParams
        bannerParams.topMargin = dp((10f * hudScale).coerceIn(if (wideSlate) 5f else 6f, if (wideSlate) 12f else 14f))
        binding.hudLevelBanner.layoutParams = bannerParams

        val actionMin = if (tabletClass) (42f * hudScale).coerceIn(38f, 56f) else (44f * hudScale).coerceIn(38f, 60f)
        binding.buttonPause.minimumWidth = dp(actionMin)
        binding.buttonPause.minimumHeight = dp(actionMin)
        binding.buttonPause.iconSize = dp((22f * hudScale).coerceIn(18f, if (tabletClass) 26f else 30f))
        val laserWidthBase = if (tabletClass) 70f else 76f
        binding.buttonLaser.minimumWidth = dp((laserWidthBase * hudScale).coerceIn(62f, if (tabletClass) 94f else 104f))
        binding.buttonLaser.minimumHeight = dp((42f * hudScale).coerceIn(34f, if (tabletClass) 50f else 56f))
        val laserMargin = dp((16f * hudScale).coerceIn(10f, if (tabletClass) 18f else 24f))
        val laserParams = binding.buttonLaser.layoutParams as ConstraintLayout.LayoutParams
        laserParams.marginStart = laserMargin
        laserParams.marginEnd = laserMargin
        val topAnchored = laserParams.topToBottom != ConstraintLayout.LayoutParams.UNSET &&
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
    }

    fun updateHudMeta() {
        val parts = mutableListOf<String>()
        if (currentModeLabel.isNotBlank()) parts.add(currentModeLabel)
        if (currentJourneyLabel.isNotBlank()) parts.add(currentJourneyLabel)
        if (currentMode == GameMode.ZEN) {
            parts.add(activity.getString(R.string.label_zen_flow))
        } else {
            parts.add(activity.getString(R.string.label_xp_format, currentXpTotal))
        }
        val modeSummary = statusSummaryForHud()
        if (!modeSummary.isNullOrBlank()) {
            parts.add(modeSummary)
        }
        val comboLabel = activity.getString(R.string.label_combo_format, currentCombo)
        if (currentCombo >= 2 && (modeSummary == null || !modeSummary.contains(comboLabel))) {
            parts.add(comboLabel)
        }
        binding.hudMeta.text = parts.joinToString(" • ")
    }

    private fun statusSummaryForHud(): String? {
        val summary = currentPowerupSummary.trim()
        if (summary.isBlank() || summary == activity.getString(R.string.label_powerups_none)) return null
        return when (currentMode) {
            GameMode.VOLLEY,
            GameMode.TUNNEL,
            GameMode.SURVIVAL -> summary
            else -> null
        }
    }

    fun updateJourneyLabel(level: Int) {
        val chapter = ProgressionManager.chapterForLevel(level)
        val stage = ProgressionManager.stageForLevel(level)
        currentJourneyLabel = activity.getString(R.string.label_journey_format, chapter, stage)
    }

    fun updateShieldVisibility(show: Boolean) {
        binding.hudShieldRow.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun applyModeHud(mode: GameMode) {
        val zen = mode == GameMode.ZEN
        binding.hudScore.visibility = if (zen) View.GONE else View.VISIBLE
        binding.hudLives.visibility = if (zen) View.GONE else View.VISIBLE
        binding.hudTime.visibility = if (zen) View.GONE else View.VISIBLE
        if (zen) {
            currentPowerupSummary = activity.getString(R.string.label_zen_flow)
        }
    }

    fun updateLaserButton(status: List<PowerupStatus>) {
        val hasLaser = status.any { it.type == PowerUpType.LASER }
        laserActive = hasLaser
        if (!hasLaser) {
            laserCooldownEndMs = 0L
            laserCooldownRunnable?.let { binding.buttonLaser.removeCallbacks(it) }
            binding.buttonLaser.text = activity.getString(R.string.label_fire)
            binding.buttonLaser.isEnabled = true
            binding.buttonLaser.alpha = 1f
        }
        binding.buttonLaser.visibility = if (hasLaser) View.VISIBLE else View.GONE
    }

    fun pulseHudMeta() {
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

    fun queueScoreUpdate(score: Int) {
        pendingScore = score
        scheduleHudUpdate()
    }

    fun queueLivesUpdate(lives: Int) {
        pendingLives = lives
        scheduleHudUpdate()
    }

    fun queueVolleyBallsUpdate(volleyBalls: Int) {
        pendingVolleyBalls = volleyBalls
        scheduleHudUpdate()
    }

    fun queueFpsUpdate(fps: Int) {
        pendingFps = fps
        scheduleHudUpdate()
    }

    private fun scheduleHudUpdate() {
        if (!hudUpdateQueued.compareAndSet(false, true)) return
        binding.root.postOnAnimation {
            hudUpdateQueued.set(false)
            pendingScore?.let {
                binding.hudScore.text = activity.getString(R.string.label_score_format, it)
                pendingScore = null
            }
            pendingLives?.let {
                if (currentMode == GameMode.VOLLEY) {
                    binding.hudLives.text = activity.getString(R.string.label_volley_balls_format, it)
                } else {
                    binding.hudLives.text = activity.getString(R.string.label_lives_format, it)
                }
                pendingLives = null
            }
            pendingVolleyBalls?.let {
                if (currentMode == GameMode.VOLLEY) {
                    binding.hudLives.text = activity.getString(R.string.label_volley_balls_format, it)
                }
                pendingVolleyBalls = null
            }
            val fps = pendingFps
            pendingFps = null
            if (fps != null && config?.settings?.showFpsCounter == true) {
                binding.hudFps.text = activity.getString(R.string.label_fps_format, fps)
                binding.hudFps.visibility = View.VISIBLE
                if (binding.hudPowerups.visibility != View.VISIBLE) {
                    binding.hudPowerups.visibility = View.VISIBLE
                }
            } else if (config?.settings?.showFpsCounter == false) {
                binding.hudFps.visibility = View.GONE
                if (binding.hudPowerupChips.childCount == 0) {
                    binding.hudPowerups.visibility = View.GONE
                }
            }
        }
    }

    fun onShieldUpdated(current: Int, max: Int) {
        if (max <= 0) {
            updateShieldVisibility(false)
            return
        }
        updateShieldVisibility(true)
        binding.hudShieldBar.max = max
        binding.hudShieldBar.progress = current.coerceIn(0, max)
        val percent = ((current.toFloat() / max.toFloat()) * 100f).toInt().coerceIn(0, 100)
        binding.hudShieldLabel.text = activity.getString(R.string.label_shield_percent, percent)
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
            binding.hudShieldLabel.setTextColor(ContextCompat.getColor(activity, R.color.bp_red))
            shieldLabelFlashToken += 1
            val labelFlashToken = shieldLabelFlashToken
            binding.hudShieldLabel.postDelayed({
                if (labelFlashToken != shieldLabelFlashToken) return@postDelayed
                binding.hudShieldLabel.setTextColor(ContextCompat.getColor(activity, R.color.bp_white))
            }, 260L)
        }
        lastShieldValue = current
    }

    fun startLaserCooldown(seconds: Float) {
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
            binding.buttonLaser.text = activity.getString(R.string.label_fire)
            binding.buttonLaser.isEnabled = true
            binding.buttonLaser.alpha = 1f
            return
        }
        val remaining = remainingMs / 1000f
        binding.buttonLaser.text = activity.getString(R.string.label_laser_cooldown, remaining)
        val runner = laserCooldownRunnable
        if (runner != null) {
            binding.buttonLaser.postDelayed(runner, 60L)
        }
    }

    fun clearLaserCooldown() {
        laserCooldownRunnable?.let { binding.buttonLaser.removeCallbacks(it) }
    }

    fun animateEndStats(summary: GameSummary, title: String) {
        binding.endTitle.text = title
        endStatsAnimator?.cancel()
        val timeText = formatDuration(summary.durationSeconds)
        binding.endStats.text = activity.getString(
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
            binding.endStats.text = activity.getString(
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

    fun playGameFade() {
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

    fun showBanner(message: String) {
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

    fun showLevelBanner(level: Int) {
        showBanner(activity.getString(R.string.label_level_format, level))
    }

    fun renderPowerupChips(status: List<PowerupStatus>) {
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
        val metrics = activity.resources.displayMetrics
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
        val chip = android.widget.TextView(activity)
        val chipScale = hudScale.coerceIn(0.82f, 1.24f)
        val chipTextSize = if (hudChipTextPx > 0f) hudChipTextPx else activity.resources.getDimension(R.dimen.bp_hud_mode_size)
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
        chip.setTextColor(ContextCompat.getColor(activity, R.color.bp_white))

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
        val chip = android.widget.TextView(activity)
        val chipScale = hudScale.coerceIn(0.82f, 1.24f)
        val chipTextSize = if (hudChipTextPx > 0f) hudChipTextPx else activity.resources.getDimension(R.dimen.bp_hud_mode_size)
        chip.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, chipTextSize)
        chip.setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
        chip.setSingleLine(true)
        chip.text = activity.getString(R.string.label_powerup_overflow_format, overflowCount)
        chip.setTextColor(ContextCompat.getColor(activity, R.color.bp_white))
        chip.setPadding(
            dp(10f * chipScale),
            dp(6f * chipScale),
            dp(10f * chipScale),
            dp(6f * chipScale)
        )
        chip.letterSpacing = 0.02f

        val stroke = ContextCompat.getColor(activity, R.color.bp_line)
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
}
