package com.breakoutplus.game

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

fun GameEngine.render(renderer: Renderer2D) {
        renderer.setWorldSize(worldWidth, worldHeight)
        renderTimeSeconds = System.nanoTime() / 1_000_000_000f
        // Enhanced background with subtle gradient and flash effect
        val flashIntensity = levelClearFlash * 0.8f
        val bgTop = if (flashIntensity > 0f) {
            adjustColor(scratchColor0, theme.background, 1.1f + flashIntensity, 1f)
        } else {
            adjustColor(scratchColor0, theme.background, 1.1f, 1f)
        }
        val bgBottom = if (flashIntensity > 0f) {
            adjustColor(scratchColor1, theme.background, 0.9f + flashIntensity, 1f)
        } else {
            adjustColor(scratchColor1, theme.background, 0.9f, 1f)
        }

        // Draw gradient background (top to bottom)
        val gradientSteps = 20
        val stepHeight = worldHeight / gradientSteps
        for (i in 0 until gradientSteps) {
            val y = i * stepHeight
            val ratio = i.toFloat() / gradientSteps.toFloat()
            renderer.drawRect(
                0f,
                y,
                worldWidth,
                stepHeight,
                fillColor(
                    tempColor,
                    bgTop[0] * (1f - ratio) + bgBottom[0] * ratio,
                    bgTop[1] * (1f - ratio) + bgBottom[1] * ratio,
                    bgTop[2] * (1f - ratio) + bgBottom[2] * ratio,
                    1f
                )
            )
        }

        // Add theme-specific background effects
        val time = renderTimeSeconds
        val backgroundFxDensity = when {
            !settings.highRefreshRate -> 0.74f
            worldHeight > 195f -> 0.86f
            else -> 1f
        } * (0.86f + cosmeticTier * 0.05f)
        fun effectCount(base: Int, minCount: Int, maxCount: Int): Int {
            return (base * backgroundFxDensity).roundToInt().coerceIn(minCount, maxCount)
        }
        when (theme.name) {
            "Neon" -> {
                // Animated grid pattern
                val cols = effectCount(base = 14, minCount = 10, maxCount = 20)
                val rows = effectCount(base = 20, minCount = 12, maxCount = 28)
                val tileWidth = worldWidth / cols.toFloat()
                val tileHeight = worldHeight / rows.toFloat()
                for (x in 0 until cols) {
                    for (y in 0 until rows) {
                        if ((x + y) % 3 != 0) continue
                        val alpha = (kotlin.math.sin(time * 1.9f + x * 0.47f + y * 0.31f) * 0.5f + 0.5f) * 0.085f
                        renderer.drawRect(
                            x * tileWidth,
                            y * tileHeight,
                            tileWidth,
                            tileHeight,
                            fillColor(tempColor, 0.3f, 0.9f, 1f, alpha)
                        )
                    }
                }
            }
            "Sunset" -> {
                // Floating particles
                val particleCount = effectCount(base = 16, minCount = 10, maxCount = 24)
                for (i in 0 until particleCount) {
                    val phase = i * 0.67f
                    val x = (kotlin.math.sin(time * 0.5f + phase) * 0.5f + 0.5f) * worldWidth
                    val y = (kotlin.math.cos(time * 0.3f + phase * 0.7f) * 0.5f + 0.5f) * worldHeight
                    val size = 0.9f + kotlin.math.sin(time * 2f + phase) * 0.45f
                    renderer.drawCircle(x, y, size, fillColor(tempColor, 1f, 0.6f, 0.3f, 0.3f))
                }
            }
            "Aurora" -> {
                // Wave patterns
                val waveCount = effectCount(base = 8, minCount = 5, maxCount = 12)
                for (i in 0 until waveCount) {
                    val waveY = worldHeight * 0.3f + kotlin.math.sin(time + i * 0.8f) * worldHeight * 0.2f
                    val alpha = (kotlin.math.sin(time * 1.5f + i) * 0.5f + 0.5f) * 0.15f
                    renderer.drawRect(
                        0f,
                        waveY,
                        worldWidth,
                        2f,
                        fillColor(tempColor, 0.3f, 0.8f, 0.5f, alpha)
                    )
                }
            }
            "Invaders" -> {
                // Starfield background
                val starCount = effectCount(base = 36, minCount = 22, maxCount = 46)
                for (i in 0 until starCount) {
                    val seed = i * 37 + 13
                    val rx = kotlin.math.sin(time * 0.08f + seed) * 0.5f + 0.5f
                    val ry = kotlin.math.cos(time * 0.07f + seed * 1.7f) * 0.5f + 0.5f
                    val x = rx * worldWidth
                    val y = ry * worldHeight
                    val twinkle = (kotlin.math.sin(time * 2.2f + seed) * 0.5f + 0.5f)
                    val alpha = 0.12f + twinkle * 0.25f
                    val size = 0.3f + twinkle * 0.4f
                    renderer.drawCircle(x, y, size, fillColor(tempColor, 0.6f, 0.8f, 1f, alpha))
                }
            }
        }

        if (config.mode == GameMode.VOLLEY) {
            val turnPulse = (kotlin.math.sin(time * 2.4f) * 0.5f + 0.5f)
            val accentAlpha = 0.08f + turnPulse * 0.08f
            val laneY = paddle.y + paddle.height * 0.5f + 1.8f
            val warningBandHeight = (worldHeight * 0.09f).coerceIn(3f, 7f)
            renderer.drawRect(
                0f,
                0f,
                worldWidth,
                warningBandHeight,
                fillColor(tempColor, 1f, 0.43f, 0.2f, 0.06f + turnPulse * 0.05f)
            )
            renderer.drawRect(
                0f,
                laneY,
                worldWidth,
                0.3f,
                fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], 0.2f + accentAlpha)
            )
            val scanYOffset = laneY + worldHeight * 0.02f
            val scanLineCount = effectCount(base = 7, minCount = 4, maxCount = 8)
            for (i in 0 until scanLineCount) {
                val progress = ((time * (0.12f + i * 0.01f) + i * 0.19f) % 1f + 1f) % 1f
                val width = worldWidth * (0.06f + (i % 3) * 0.01f)
                val x = progress * (worldWidth + width) - width
                val alpha = (0.04f + turnPulse * 0.03f - i * 0.004f).coerceAtLeast(0.015f)
                renderer.drawRect(
                    x,
                    scanYOffset + i * 0.42f,
                    width,
                    0.12f,
                    fillColor(tempColor, 0.95f, 0.58f, 0.22f, alpha)
                )
            }
            val laneBandCount = effectCount(base = 6, minCount = 4, maxCount = 7)
            for (i in 0 until laneBandCount) {
                val y = worldHeight * 0.58f + i * worldHeight * 0.055f
                val alpha = (0.03f + (i % 2) * 0.015f + turnPulse * 0.01f).coerceIn(0.02f, 0.08f)
                renderer.drawRect(
                    0f,
                    y,
                    worldWidth,
                    0.22f,
                    fillColor(tempColor, theme.paddle[0], theme.paddle[1], theme.paddle[2], alpha)
                )
            }
        }
        if (config.mode == GameMode.TUNNEL) {
            val gateZone = tunnelGateZone()
            val boardCols = ((currentLayout?.cols ?: 12) + layoutColBoost).coerceAtLeast(1)
            val colWidth = worldWidth / boardCols.toFloat()
            val centerCol = gateZone?.let {
                ((it.minCol + it.maxCol) * 0.5f).roundToInt().coerceIn(0, boardCols - 1)
            } ?: (boardCols / 2)
            val gateWidthCols = gateZone?.let { (it.maxCol - it.minCol + 1).coerceAtLeast(1) } ?: 3
            val gateWidth = (colWidth * gateWidthCols).coerceIn(8f, 20f)
            val gateX = ((centerCol + 0.5f) * colWidth - gateWidth * 0.5f).coerceIn(0f, worldWidth - gateWidth)
            val gateY = worldHeight * 0.52f
            val gateHeight = worldHeight * 0.36f
            val gatePulse = tunnelGateFlash.coerceIn(0f, 1f)
            val readiness = (tunnelSupplyReadinessPercent / 100f).coerceIn(0f, 1f)
            val integrity = (cachedTunnelGateIntegrityPercent / 100f).coerceIn(0f, 1f)
            val urgency = (1f - integrity).coerceIn(0f, 1f)
            val pulse = (kotlin.math.sin(time * 4.4f) * 0.5f + 0.5f)

            val laneAlpha = (0.04f + readiness * 0.05f + urgency * 0.05f + pulse * 0.02f).coerceIn(0.04f, 0.16f)
            renderer.drawRect(
                gateX,
                paddle.y + paddle.height * 0.8f,
                gateWidth,
                gateY - paddle.y,
                fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], laneAlpha)
            )

            renderer.drawRect(
                gateX,
                gateY,
                gateWidth,
                gateHeight,
                fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], 0.07f + gatePulse * 0.17f + readiness * 0.08f)
            )
            renderer.drawRect(
                gateX + gateWidth * 0.24f,
                gateY,
                gateWidth * 0.52f,
                gateHeight,
                fillColor(tempColor, 1f, 0.95f, 0.85f, 0.04f + gatePulse * 0.1f + readiness * 0.06f)
            )
            renderer.drawRect(
                gateX,
                gateY + gateHeight * 0.92f,
                gateWidth * readiness,
                gateHeight * 0.06f,
                fillColor(tempColor, 0.5f + readiness * 0.5f, 0.9f, 0.55f + readiness * 0.4f, 0.24f + readiness * 0.24f)
            )
        }

        if (gravityWellActive) {
            val centerX = worldWidth * 0.5f
            val centerY = worldHeight * 0.62f
            for (i in 0 until 6) {
                val radius = 2f + i * 2.4f
                val angle = time * (0.8f + i * 0.12f)
                val x = centerX + kotlin.math.cos(angle) * radius
                val y = centerY + kotlin.math.sin(angle) * radius
                val alpha = (0.18f - i * 0.02f).coerceAtLeast(0.05f)
                renderer.drawCircle(x, y, 0.6f + i * 0.12f, fillColor(tempColor, 0.45f, 0.65f, 1f, alpha))
            }
        }

        if (activeEffects.containsKey(PowerUpType.FREEZE) || activeEffects.containsKey(PowerUpType.SLOW)) {
            val chillAlpha = if (activeEffects.containsKey(PowerUpType.FREEZE)) 0.12f else 0.08f
            renderer.drawRect(0f, 0f, worldWidth, worldHeight, fillColor(tempColor, 0.35f, 0.6f, 1f, chillAlpha))
        }

        if (guardrailActive) {
            val pulse = (kotlin.math.sin(time * 3f) * 0.5f + 0.5f)
            renderer.drawRect(
                0f,
                2f,
                worldWidth,
                0.6f,
                fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], 0.5f + pulse * 0.4f)
            )
        }

        if (config.mode.invaders && invaderTelegraphKey != null) {
            val target = invaderBricks.firstOrNull { it.alive && invaderKey(it) == invaderTelegraphKey }
            if (target != null) {
                val alpha = ((invaderTelegraphLead - invaderShotTimer).coerceIn(0f, invaderTelegraphLead) / invaderTelegraphLead)
                val pulse = (kotlin.math.sin(time * 16f) * 0.5f + 0.5f)
                val beamAlpha = (0.15f + alpha * 0.5f + pulse * 0.2f).coerceIn(0f, 0.8f)
                val beamWidth = 0.5f + alpha * 0.8f
                val beamX = target.centerX - beamWidth / 2f
                val beamY = target.y - worldHeight * 0.02f
                val beamHeight = target.y - paddle.y + paddle.height * 0.6f
                renderer.drawRect(
                    beamX,
                    paddle.y + paddle.height * 0.2f,
                    beamWidth,
                    beamHeight,
                    fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], beamAlpha)
                )
            }
        }

        for (brick in bricks) {
            if (!brick.alive) continue
            if (!renderer.isRectVisible(brick.x, brick.y, brick.width, brick.height)) continue
            val color = if (config.mode == GameMode.TUNNEL && brick.type == BrickType.UNBREAKABLE) {
                // Keep tunnel walls visually distinct from regular bricks.
                tunnelWallColor
            } else {
                brick.currentColor(theme)
            }

            if (brick.type == BrickType.INVADER) {
                drawInvaderShip(renderer, brick, color)
                if (brick.maxHitPoints >= 2) {
                    val armor = adjustColor(scratchColor2, color, 0.82f, 0.9f)
                    val count = if (brick.maxHitPoints >= 3) 2 else 1
                    drawStripe(renderer, brick, armor, count)
                if (brick.maxHitPoints >= 3) {
                    val core = adjustColor(scratchColor3, color, 1.4f, 0.9f)
                    renderer.drawCircle(brick.centerX, brick.centerY, brick.height * 0.08f, core)
                }
            }
            continue
        }

            // 3D depth effect: base shadow
            val shadowOffset = brick.width * 0.02f
            val shadowColor = adjustColor(scratchColor4, color, 0.4f, 0.3f)
            renderer.drawRect(brick.x + shadowOffset, brick.y + shadowOffset, brick.width, brick.height, shadowColor)

            // Main brick body
            renderer.drawRect(brick.x, brick.y, brick.width, brick.height, color)

            // 3D highlights and bevels
            val highlight = adjustColor(scratchColor5, color, 1.3f, 1f)
            val midtone = adjustColor(scratchColor6, color, 0.9f, 1f)
            val lowlight = adjustColor(scratchColor7, color, 0.6f, 1f)

            // Top bevel (highlight)
            val topBevelHeight = brick.height * 0.08f
            renderer.drawRect(brick.x, brick.y, brick.width, topBevelHeight, highlight)

            // Left bevel (highlight)
            val leftBevelWidth = brick.width * 0.06f
            renderer.drawRect(brick.x, brick.y, leftBevelWidth, brick.height, midtone)

            // Bottom bevel (lowlight/shadow)
            val bottomBevelHeight = brick.height * 0.1f
            renderer.drawRect(brick.x, brick.y + brick.height - bottomBevelHeight, brick.width, bottomBevelHeight, lowlight)

            // Right bevel (lowlight/shadow)
            val rightBevelWidth = brick.width * 0.08f
            renderer.drawRect(brick.x + brick.width - rightBevelWidth, brick.y, rightBevelWidth, brick.height, lowlight)

            when (brick.type) {
                BrickType.REINFORCED -> drawStripe(renderer, brick, adjustColor(scratchColor8, color, 0.85f, 1f), 1)
                BrickType.ARMORED -> drawStripe(renderer, brick, adjustColor(scratchColor9, color, 0.78f, 1f), 2)
                BrickType.UNBREAKABLE -> {
                    drawStripe(renderer, brick, adjustColor(scratchColor10, color, 0.66f, 1f), 3)
                    if (config.mode == GameMode.TUNNEL) {
                        val lock = adjustColor(scratchColor9, color, 0.48f, 1f)
                        val cap = adjustColor(scratchColor11, color, 1.12f, 0.95f)
                        val barWidth = brick.width * 0.14f
                        val barHeight = brick.height * 0.5f
                        renderer.drawRect(
                            brick.centerX - barWidth * 0.5f,
                            brick.centerY - barHeight * 0.5f,
                            barWidth,
                            barHeight,
                            lock
                        )
                        renderer.drawRect(
                            brick.centerX - brick.width * 0.24f,
                            brick.centerY - brick.height * 0.06f,
                            brick.width * 0.48f,
                            brick.height * 0.12f,
                            cap
                        )
                    }
                }
                BrickType.MOVING -> {
                    // Add movement indicator
                    val indicatorColor = adjustColor(scratchColor8, color, 1.3f, 0.8f)
                    renderer.drawRect(brick.x + brick.width * 0.1f, brick.y + brick.height * 0.1f,
                                    brick.width * 0.8f, brick.height * 0.05f, indicatorColor)
                }
                BrickType.SPAWNING -> {
                    // Add spawn indicator (dots)
                    val dotColor = adjustColor(scratchColor8, color, 1.2f, 0.9f)
                    val dotSize = brick.width * 0.08f
                    for (i in 0 until brick.spawnCount) {
                        val dotX = brick.x + brick.width * 0.2f + i * brick.width * 0.15f
                        val dotY = brick.y + brick.height * 0.85f
                        renderer.drawCircle(dotX, dotY, dotSize, dotColor)
                    }
                }
                BrickType.PHASE -> {
                    // Phase indicator (colored bars)
                    val phaseColor = when (brick.phase) {
                        0 -> fillColor(tempColor, 0f, 1f, 0f, 0.8f) // Green
                        1 -> fillColor(tempColor, 1f, 1f, 0f, 0.8f) // Yellow
                        else -> fillColor(tempColor, 1f, 0f, 0f, 0.8f) // Red
                    }
                    val barHeight = brick.height * 0.1f
                    val barY = brick.y + brick.height - barHeight
                    renderer.drawRect(brick.x, barY, brick.width, barHeight, phaseColor)
                }
                BrickType.BOSS -> {
                    // Boss indicator (pulsing border)
                    val pulse = (kotlin.math.sin(time * 4f) * 0.5f + 0.5f) * 0.3f + 0.7f
                    val bossColor = adjustColor(scratchColor8, color, pulse, 1f)
                    val borderWidth = brick.width * 0.05f
                    // Draw border by drawing slightly larger rect underneath
                    renderer.drawRect(brick.x - borderWidth, brick.y - borderWidth,
                                    brick.width + borderWidth * 2, brick.height + borderWidth * 2, bossColor)
                }
                else -> Unit
            }
        }

        powerups.forEach { power ->
            renderPowerup(renderer, power)

            // Magnet indicator: show attraction line when magnet is active
            if (magnetActive) {
                val dx = paddle.x - power.x
                val dy = paddle.y + paddle.height / 2f - power.y
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                if (distance > 0.1f && distance < 25f) { // Only show for nearby powerups
                    val steps = 8
                    val lineAlpha = (0.15f * (1f - distance / 25f)).coerceIn(0.02f, 0.15f)
                    val lineColor = fillColor(tempColor, 0.8f, 0.4f, 1f, lineAlpha)
                    repeat(steps) { step ->
                        val t = step.toFloat() / (steps - 1)
                        val x = power.x + dx * t
                        val y = power.y + dy * t
                        val dotSize = 0.15f + (1f - t) * 0.1f
                        renderer.drawCircle(x, y, dotSize, lineColor)
                    }
                }
            }
        }

        beams.forEach { beam ->
            renderer.drawBeam(beam.x, beam.y, beam.width, beam.height, beam.color)
        }

        enemyShots.forEach { shot ->
            val speed = kotlin.math.abs(shot.vy)
            val glow = adjustColor(scratchColor2, shot.color, 1.2f + speed * 0.002f, 0.5f)
            renderer.drawCircle(shot.x, shot.y, shot.radius * 1.9f, glow)
            val trailLen = when (shot.style) {
                1 -> 5.0f
                2 -> 4.2f
                else -> if (shot.wiggle > 0f) 3.8f else 3.2f
            }
            val trailColor = when (shot.style) {
                1 -> fillColor(tempColor, 1f, 0.85f, 0.55f, 0.45f)
                2 -> fillColor(tempColor, 0.85f, 0.55f, 1f, 0.4f)
                else -> fillColor(tempColor, shot.color[0], shot.color[1], shot.color[2], 0.35f)
            }
            renderer.drawRect(
                shot.x - shot.radius * 0.28f,
                shot.y + shot.radius * 0.6f,
                shot.radius * 0.56f,
                shot.radius * trailLen,
                trailColor
            )
            when (shot.style) {
                1 -> {
                    renderer.drawRect(
                        shot.x - shot.radius * 0.4f,
                        shot.y - shot.radius * 1.2f,
                        shot.radius * 0.8f,
                        shot.radius * 2.4f,
                        shot.color
                    )
                    renderer.drawCircle(
                        shot.x,
                        shot.y + shot.radius * 1.1f,
                        shot.radius * 0.6f,
                        adjustColor(scratchColor3, shot.color, 1.4f, 0.8f)
                    )
                }
                2 -> {
                    renderer.drawCircle(
                        shot.x,
                        shot.y,
                        shot.radius * 1.35f,
                        adjustColor(scratchColor3, shot.color, 1.4f, 0.45f)
                    )
                    renderer.drawCircle(shot.x, shot.y, shot.radius * 0.65f, shot.color)
                }
                else -> {
                    renderer.drawCircle(shot.x, shot.y, shot.radius, shot.color)
                }
            }
        }

        if (config.mode.invaders && invaderShieldMax > 0f) {
            val ratio = (invaderShield / invaderShieldMax).coerceIn(0f, 1f)
            val pulse = if (invaderShieldCritical) (kotlin.math.sin(time * 6f) * 0.5f + 0.5f) else 0f
            val shieldY = paddle.y + paddle.height * 1.15f
            val thickness = 0.6f + ratio * 0.5f
            val alpha = (0.15f + ratio * 0.35f + shieldHitPulse * 0.25f + pulse * 0.2f).coerceIn(0.1f, 0.75f)
            val baseColor = if (invaderShieldCritical) {
                fillColor(scratchColor11, 1f, 0.45f, 0.45f, alpha)
            } else {
                fillColor(scratchColor11, 0.45f, 0.9f, 1f, alpha)
            }
            val shieldX = worldWidth * 0.06f
            val shieldWidth = worldWidth * 0.88f
            renderer.drawRect(shieldX, shieldY - thickness / 2f, shieldWidth, thickness, baseColor)

            val segments = 10
            val segmentWidth = shieldWidth / segments
            for (i in 0 until segments) {
                val shimmer = (kotlin.math.sin(time * 3.2f + i) * 0.4f + 0.6f).coerceIn(0.3f, 1f)
                val segAlpha = (alpha * shimmer).coerceIn(0f, 0.8f)
                val segX = shieldX + i * segmentWidth + 0.35f
                renderer.drawRect(
                    segX,
                    shieldY - thickness * 0.28f,
                    segmentWidth - 0.7f,
                    thickness * 0.56f,
                    fillColor(tempColor, baseColor[0], baseColor[1], baseColor[2], segAlpha)
                )
            }

            if (shieldHitPulse > 0f) {
                val ringAlpha = (0.4f * shieldHitPulse).coerceIn(0f, 0.6f)
                val ringX = shieldHitX.coerceIn(shieldX, shieldX + shieldWidth)
                renderer.drawCircle(
                    ringX,
                    shieldY + thickness * 0.15f,
                    1.2f + 2.2f * shieldHitPulse,
                    fillColor(tempColor, baseColor[0], baseColor[1], baseColor[2], ringAlpha)
                )
            }

            if (shieldBreakPulse > 0f) {
                val breakAlpha = (shieldBreakPulse * 0.55f).coerceIn(0f, 0.7f)
                renderer.drawRect(
                    shieldX,
                    shieldY - thickness,
                    shieldWidth,
                    thickness * 2.2f,
                    fillColor(tempColor, 1f, 0.4f, 0.4f, breakAlpha)
                )
            }
        }

        waves.forEach { wave ->
            // Render explosion wave with gradient effect
            val lifeRatio = wave.life / wave.maxLife
            val baseAlpha = (lifeRatio * 0.7f).coerceIn(0f, 0.7f)

            // Outer ring (most transparent)
            renderer.drawCircle(
                wave.x, wave.y, wave.radius,
                fillColor(tempColor, wave.color[0], wave.color[1], wave.color[2], baseAlpha * 0.3f)
            )

            // Middle ring
            renderer.drawCircle(
                wave.x, wave.y, wave.radius * 0.75f,
                fillColor(tempColor, wave.color[0], wave.color[1], wave.color[2], baseAlpha * 0.6f)
            )

            // Inner core (most opaque)
            renderer.drawCircle(
                wave.x, wave.y, wave.radius * 0.5f,
                fillColor(tempColor, wave.color[0], wave.color[1], wave.color[2], baseAlpha)
            )
        }
        particles.forEach { particle ->
            if (renderer.isCircleVisible(particle.x, particle.y, particle.radius)) {
                renderer.drawCircleBatch(particle.x, particle.y, particle.radius, particle.color)
            }
        }
        renderer.flushCircleBatch()

        if (state == GameState.READY || hasStuckBall()) {
            renderAimGuide(renderer)
        }

        if (activeEffects.containsKey(PowerUpType.LASER) || shieldCharges > 0) {
            val glowAlpha = if (activeEffects.containsKey(PowerUpType.LASER)) 0.55f else 0.35f
            renderer.drawRect(
                paddle.x - paddle.width / 2f - 1.2f,
                paddle.y - paddle.height / 2f - 0.6f,
                paddle.width + 2.4f,
                paddle.height + 1.2f,
                fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], glowAlpha)
            )
        }
        renderer.drawRect(paddle.x - paddle.width / 2f, paddle.y - paddle.height / 2f, paddle.width, paddle.height, theme.paddle)
        if (cosmeticTier >= 2) {
            renderer.drawRect(
                paddle.x - paddle.width / 2f,
                paddle.y + paddle.height * 0.38f,
                paddle.width,
                paddle.height * 0.08f,
                fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], 0.6f)
            )
        }
        if (shieldHitPulse > 0f) {
            val pulseAlpha = (shieldHitPulse * 0.65f).coerceIn(0f, 0.65f)
            val pulseWidth = paddle.width + 3.2f * shieldHitPulse
            val pulseHeight = paddle.height + 1.4f * shieldHitPulse
            renderer.drawRect(
                paddle.x - pulseWidth / 2f,
                paddle.y - pulseHeight / 2f,
                pulseWidth,
                pulseHeight,
                fillColor(tempColor, shieldHitColor[0], shieldHitColor[1], shieldHitColor[2], pulseAlpha)
            )
            val hitX = shieldHitX.coerceIn(paddle.x - paddle.width / 2f, paddle.x + paddle.width / 2f)
            renderer.drawCircle(
                hitX,
                paddle.y + paddle.height / 2f + 0.6f,
                0.8f + 1.4f * shieldHitPulse,
                fillColor(tempColor, shieldHitColor[0], shieldHitColor[1], shieldHitColor[2], pulseAlpha)
            )
        }

        if (powerupCollectionPulse > 0f) {
            val pulseAlpha = (powerupCollectionPulse * 0.5f).coerceIn(0f, 0.5f)
            val pulseWidth = paddle.width + 2.8f * powerupCollectionPulse
            val pulseHeight = paddle.height + 1.6f * powerupCollectionPulse
            renderer.drawRect(
                paddle.x - pulseWidth / 2f,
                paddle.y - pulseHeight / 2f,
                pulseWidth,
                pulseHeight,
                fillColor(tempColor, 0.9f, 0.95f, 1f, pulseAlpha)
            )
        }

        balls.forEach { ball ->
            val speed = kotlin.math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy)
            val glowStrength = (speed / 70f).coerceIn(0.2f, 0.55f) + cosmeticTier * 0.04f
            val glowBase = when {
                ball.isFireball -> PowerUpType.FIREBALL.color
                pierceActive -> PowerUpType.PIERCE.color
                else -> theme.accent
            }

            ball.trail.forEach { point ->
                val lifeRatio = (point.life / point.maxLife).coerceIn(0f, 1f)
                val alpha = lifeRatio * (0.35f + cosmeticTier * 0.06f)
                renderer.drawCircle(
                    point.x,
                    point.y,
                    point.radius * lifeRatio,
                    fillColor(tempColor, glowBase[0], glowBase[1], glowBase[2], alpha)
                )
                if (cosmeticTier >= 2) {
                    renderer.drawCircle(
                        point.x,
                        point.y,
                        point.radius * lifeRatio * 0.6f,
                        fillColor(tempColor, theme.paddle[0], theme.paddle[1], theme.paddle[2], alpha * 0.6f)
                    )
                }
            }

            renderer.drawCircle(
                ball.x,
                ball.y,
                ball.radius * 1.8f,
                fillColor(tempColor, glowBase[0], glowBase[1], glowBase[2], glowStrength)
            )
            if (cosmeticTier >= 3) {
                renderer.drawCircle(
                    ball.x,
                    ball.y,
                    ball.radius * 2.4f,
                    fillColor(tempColor, glowBase[0], glowBase[1], glowBase[2], 0.18f + cosmeticTier * 0.04f)
                )
            }
            renderer.drawCircle(ball.x, ball.y, ball.radius, ball.color)
        }
    }

internal fun GameEngine.renderPowerup(renderer: Renderer2D, power: PowerUp) {
        val time = renderTimeSeconds
        val isNegative = power.type == PowerUpType.SHRINK || power.type == PowerUpType.OVERDRIVE
        val pulseSpeed = if (isNegative) 4f else 3f // Faster pulse for negative powerups
        val pulse = (kotlin.math.sin(time * pulseSpeed) * 0.5f + 0.5f)
        val wobble = kotlin.math.sin(time * 2.1f) * 0.03f // Gentle wobble rotation effect
        val size = power.size * (0.9f + pulse * 0.12f)

        // Enhanced outer glow effect - red tint for negative powerups
        val glowColor = if (isNegative) {
            fillColor(tempColor, 0.8f + pulse * 0.2f, 0.2f, 0.2f, 0.6f + pulse * 0.2f)
        } else {
            adjustColor(scratchColor0, power.type.color, 0.25f + pulse * 0.2f, 0.6f)
        }
        renderer.drawRect(power.x - size * 0.6f, power.y - size * 0.6f, size * 1.2f, size * 1.2f, glowColor)
        // Rotating ring effect - simple pulsing ring
        val ringPulse = (kotlin.math.sin(time * 2.5f) * 0.5f + 0.5f)
        val ringSize = size * (0.75f + ringPulse * 0.15f)
        val ringAlpha = 0.12f + pulse * 0.08f
        renderer.drawCircle(
            power.x,
            power.y,
            ringSize,
            fillColor(tempColor, power.type.color[0], power.type.color[1], power.type.color[2], ringAlpha)
        )

        val innerRingAlpha = 0.08f + pulse * 0.12f
        renderer.drawCircle(
            power.x,
            power.y,
            size * 0.62f,
            fillColor(tempColor, power.type.color[0], power.type.color[1], power.type.color[2], innerRingAlpha)
        )

        // Main powerup body with gradient
        val outer = adjustColor(scratchColor1, power.type.color, 0.7f, 1f)
        val inner = adjustColor(scratchColor2, power.type.color, 1.1f + pulse * 0.05f, 1f)

        // Draw with rounded appearance using multiple rects
        val cornerInset = size * 0.1f
        val x = power.x - size / 2f + wobble * size * 0.5f
        val y = power.y - size / 2f + wobble * size * 0.3f
        renderer.drawRect(x, y, size, size, outer)
        renderer.drawRect(x + cornerInset, y + cornerInset, size - cornerInset * 2f, size - cornerInset * 2f, inner)
        val highlight = fillColor(tempColor, 1f, 1f, 1f, 0.18f + pulse * 0.12f)
        renderer.drawRect(x + size * 0.12f, y + size * 0.62f, size * 0.76f, size * 0.18f, highlight)

        // Add subtle glow effect for better visibility
        val outerGlowColor = adjustColor(scratchColor3, power.type.color, 1.2f, 0.4f)
        renderer.drawCircle(power.x, power.y, size * 1.4f, outerGlowColor)

        val glyph = adjustColor(scratchColor4, power.type.color, 1.5f, 0.95f)
        val glyphSoft = adjustColor(scratchColor5, power.type.color, 1.15f, 0.78f)
        val outlineColor = adjustColor(scratchColor6, power.type.color, 1.8f, 0.3f)
        renderer.drawCircle(power.x, power.y, size * 0.95f, outlineColor)

        when (power.type) {
            PowerUpType.MULTI_BALL -> {
                renderer.drawCircle(power.x, power.y + size * 0.16f, size * 0.12f, glyph)
                renderer.drawCircle(power.x - size * 0.14f, power.y - size * 0.08f, size * 0.12f, glyph)
                renderer.drawCircle(power.x + size * 0.14f, power.y - size * 0.08f, size * 0.12f, glyph)
                renderer.drawRect(power.x - size * 0.02f, power.y - size * 0.22f, size * 0.04f, size * 0.44f, glyphSoft)
            }
            PowerUpType.LASER -> {
                renderer.drawRect(power.x - size * 0.18f, power.y - size * 0.26f, size * 0.08f, size * 0.52f, glyph)
                renderer.drawRect(power.x + size * 0.10f, power.y - size * 0.26f, size * 0.08f, size * 0.52f, glyph)
                renderer.drawCircle(power.x - size * 0.14f, power.y + size * 0.28f, size * 0.06f, glyphSoft)
                renderer.drawCircle(power.x + size * 0.14f, power.y + size * 0.28f, size * 0.06f, glyphSoft)
                renderer.drawRect(power.x - size * 0.06f, power.y - size * 0.04f, size * 0.12f, size * 0.08f, glyphSoft)
            }
            PowerUpType.GUARDRAIL -> {
                renderer.drawRect(power.x - size * 0.32f, power.y - size * 0.02f, size * 0.64f, size * 0.1f, glyph)
                renderer.drawRect(power.x - size * 0.26f, power.y - size * 0.2f, size * 0.08f, size * 0.18f, glyphSoft)
                renderer.drawRect(power.x + size * 0.18f, power.y - size * 0.2f, size * 0.08f, size * 0.18f, glyphSoft)
                renderer.drawRect(power.x - size * 0.22f, power.y + size * 0.08f, size * 0.44f, size * 0.06f, glyphSoft)
            }
            PowerUpType.SHIELD -> {
                renderer.drawCircle(power.x, power.y + size * 0.1f, size * 0.18f, glyph)
                renderer.drawRect(power.x - size * 0.2f, power.y - size * 0.02f, size * 0.4f, size * 0.22f, glyph)
                renderer.drawRect(power.x - size * 0.12f, power.y - size * 0.2f, size * 0.24f, size * 0.18f, glyph)
                renderer.drawRect(power.x - size * 0.04f, power.y - size * 0.12f, size * 0.08f, size * 0.18f, glyphSoft)
            }
            PowerUpType.WIDE_PADDLE -> {
                renderer.drawRect(power.x - size * 0.32f, power.y - size * 0.06f, size * 0.64f, size * 0.12f, glyph)
                renderer.drawRect(power.x - size * 0.48f, power.y - size * 0.12f, size * 0.06f, size * 0.24f, glyphSoft)
                renderer.drawRect(power.x + size * 0.42f, power.y - size * 0.12f, size * 0.06f, size * 0.24f, glyphSoft)
                renderer.drawRect(power.x - size * 0.42f, power.y - size * 0.02f, size * 0.1f, size * 0.04f, glyphSoft)
                renderer.drawRect(power.x + size * 0.32f, power.y - size * 0.02f, size * 0.1f, size * 0.04f, glyphSoft)
            }
            PowerUpType.SHRINK -> {
                renderer.drawRect(power.x - size * 0.18f, power.y - size * 0.08f, size * 0.12f, size * 0.16f, glyph)
                renderer.drawRect(power.x + size * 0.06f, power.y - size * 0.08f, size * 0.12f, size * 0.16f, glyph)
                renderer.drawRect(power.x - size * 0.05f, power.y - size * 0.06f, size * 0.1f, size * 0.12f, glyphSoft)
            }
            PowerUpType.SLOW -> {
                renderer.drawCircle(power.x, power.y, size * 0.19f, glyph)
                renderer.drawRect(power.x - size * 0.02f, power.y, size * 0.04f, size * 0.16f, glyphSoft)
                renderer.drawRect(power.x - size * 0.02f, power.y - size * 0.08f, size * 0.14f, size * 0.04f, glyphSoft)
                renderer.drawCircle(power.x, power.y, size * 0.04f, glyph)
            }
            PowerUpType.OVERDRIVE -> {
                renderer.drawRect(power.x - size * 0.08f, power.y + size * 0.08f, size * 0.16f, size * 0.18f, glyph)
                renderer.drawRect(power.x - size * 0.14f, power.y - size * 0.02f, size * 0.28f, size * 0.12f, glyphSoft)
                renderer.drawRect(power.x - size * 0.08f, power.y - size * 0.2f, size * 0.16f, size * 0.18f, glyph)
            }
            PowerUpType.FIREBALL -> {
                renderer.drawCircle(power.x, power.y + size * 0.04f, size * 0.18f, glyph)
                renderer.drawCircle(power.x + size * 0.05f, power.y + size * 0.18f, size * 0.09f, glyphSoft)
                renderer.drawRect(power.x - size * 0.06f, power.y - size * 0.22f, size * 0.12f, size * 0.18f, glyph)
                renderer.drawRect(power.x + size * 0.02f, power.y - size * 0.22f, size * 0.08f, size * 0.14f, glyphSoft)
            }
            PowerUpType.LIFE -> {
                renderer.drawCircle(power.x - size * 0.1f, power.y + size * 0.08f, size * 0.1f, glyph)
                renderer.drawCircle(power.x + size * 0.1f, power.y + size * 0.08f, size * 0.1f, glyph)
                renderer.drawRect(power.x - size * 0.2f, power.y - size * 0.04f, size * 0.4f, size * 0.18f, glyph)
                renderer.drawRect(power.x - size * 0.1f, power.y - size * 0.22f, size * 0.2f, size * 0.18f, glyph)
            }
            PowerUpType.MAGNET -> {
                renderer.drawRect(power.x - size * 0.22f, power.y - size * 0.18f, size * 0.1f, size * 0.32f, glyph)
                renderer.drawRect(power.x + size * 0.12f, power.y - size * 0.18f, size * 0.1f, size * 0.32f, glyph)
                renderer.drawRect(power.x - size * 0.22f, power.y - size * 0.22f, size * 0.44f, size * 0.08f, glyph)
                renderer.drawRect(power.x - size * 0.22f, power.y + size * 0.12f, size * 0.44f, size * 0.08f, glyphSoft)
            }
            PowerUpType.GRAVITY_WELL -> {
                val centerX = power.x
                val centerY = power.y
                val step = size * 0.08f
                for (i in 0..4) {
                    val angle = i * 1.1f + pulse
                    val radius = step * (i + 1)
                    val x = centerX + kotlin.math.cos(angle) * radius
                    val y = centerY + kotlin.math.sin(angle) * radius
                    renderer.drawCircle(x, y, size * 0.06f, glyph)
                }
                renderer.drawCircle(centerX, centerY, size * 0.08f, glyphSoft)
            }
            PowerUpType.BALL_SPLITTER -> {
                renderer.drawCircle(power.x, power.y + size * 0.12f, size * 0.1f, glyph)
                renderer.drawCircle(power.x - size * 0.12f, power.y - size * 0.06f, size * 0.1f, glyph)
                renderer.drawCircle(power.x + size * 0.12f, power.y - size * 0.06f, size * 0.1f, glyph)
                renderer.drawRect(power.x - size * 0.02f, power.y - size * 0.2f, size * 0.04f, size * 0.4f, glyphSoft)
            }
            PowerUpType.FREEZE -> {
                renderer.drawRect(power.x - size * 0.02f, power.y - size * 0.2f, size * 0.04f, size * 0.4f, glyph)
                renderer.drawRect(power.x - size * 0.2f, power.y - size * 0.02f, size * 0.4f, size * 0.04f, glyph)
                renderer.drawRect(power.x - size * 0.14f, power.y - size * 0.14f, size * 0.08f, size * 0.08f, glyphSoft)
                renderer.drawRect(power.x + size * 0.06f, power.y - size * 0.14f, size * 0.08f, size * 0.08f, glyphSoft)
                renderer.drawRect(power.x - size * 0.14f, power.y + size * 0.06f, size * 0.08f, size * 0.08f, glyphSoft)
                renderer.drawRect(power.x + size * 0.06f, power.y + size * 0.06f, size * 0.08f, size * 0.08f, glyphSoft)
            }
            PowerUpType.PIERCE -> {
                renderer.drawRect(power.x - size * 0.02f, power.y - size * 0.22f, size * 0.04f, size * 0.44f, glyph)
                renderer.drawRect(power.x - size * 0.12f, power.y + size * 0.12f, size * 0.24f, size * 0.06f, glyph)
                renderer.drawRect(power.x - size * 0.08f, power.y + size * 0.18f, size * 0.16f, size * 0.06f, glyphSoft)
                renderer.drawRect(power.x - size * 0.22f, power.y - size * 0.08f, size * 0.44f, size * 0.16f, glyphSoft)
            }
            PowerUpType.RICOCHET -> {
                // Bouncing arrows
                renderer.drawRect(power.x - size * 0.18f, power.y - size * 0.02f, size * 0.12f, size * 0.04f, glyph)
                renderer.drawRect(power.x - size * 0.06f, power.y - size * 0.14f, size * 0.04f, size * 0.12f, glyph)
                renderer.drawRect(power.x + size * 0.02f, power.y + size * 0.02f, size * 0.12f, size * 0.04f, glyphSoft)
                renderer.drawRect(power.x + size * 0.1f, power.y + size * 0.06f, size * 0.04f, size * 0.12f, glyphSoft)
            }
            PowerUpType.TIME_WARP -> {
                // Clock/spiral
                renderer.drawCircle(power.x, power.y, size * 0.18f, glyph)
                renderer.drawRect(power.x - size * 0.02f, power.y - size * 0.18f, size * 0.04f, size * 0.12f, glyphSoft)
                renderer.drawRect(power.x - size * 0.18f, power.y - size * 0.02f, size * 0.12f, size * 0.04f, glyphSoft)
                renderer.drawRect(power.x + size * 0.06f, power.y + size * 0.06f, size * 0.08f, size * 0.08f, glyph)
            }
            PowerUpType.DOUBLE_SCORE -> {
                // Star/X shape
                renderer.drawRect(power.x - size * 0.02f, power.y - size * 0.18f, size * 0.04f, size * 0.36f, glyph)
                renderer.drawRect(power.x - size * 0.18f, power.y - size * 0.02f, size * 0.36f, size * 0.04f, glyph)
                renderer.drawRect(power.x - size * 0.12f, power.y - size * 0.12f, size * 0.08f, size * 0.08f, glyphSoft)
                renderer.drawRect(power.x + size * 0.04f, power.y - size * 0.12f, size * 0.08f, size * 0.08f, glyphSoft)
                renderer.drawRect(power.x - size * 0.12f, power.y + size * 0.04f, size * 0.08f, size * 0.08f, glyphSoft)
                renderer.drawRect(power.x + size * 0.04f, power.y + size * 0.04f, size * 0.08f, size * 0.08f, glyphSoft)
            }
        }
    }

internal fun GameEngine.renderAimGuide(renderer: Renderer2D) {
        val ball = balls.firstOrNull() ?: return
        val angle = aimAngle.coerceIn(aimMinAngle, Math.PI.toFloat() - aimMinAngle)
        var dx = kotlin.math.cos(angle)
        var dy = kotlin.math.sin(angle)
        val startX = ball.x
        val startY = ball.y
        val radius = ball.radius
        val arrowLength = (worldHeight * 0.16f).coerceIn(12f, 22f)
        val arrowSteps = 7
        for (i in 1..arrowSteps) {
            val t = i.toFloat() / arrowSteps.toFloat()
            val size = 0.42f + t * 0.34f
            val alpha = (0.5f + t * 0.5f).coerceIn(0f, 1f) // Increased base alpha from 0.35f to 0.5f
            renderer.drawCircle(
                startX + dx * arrowLength * t,
                startY + dy * arrowLength * t,
                size,
                fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], alpha)
            )
        }

        var segStartX = startX
        var segStartY = startY
        val maxSegments = 5

        repeat(maxSegments) { segment ->
            val hit = findAimCollision(segStartX, segStartY, dx, dy, radius)
            if (!hit.t.isFinite() || hit.t <= 0f) return
            val segmentLength = hit.t
            val steps = (segmentLength / (worldHeight * 0.06f))
                .toInt()
                .coerceIn(6, 16)
            val segmentAlpha = (0.38f - segment * 0.08f).coerceAtLeast(0.14f)
            val isFirstBounce = segment == 0
            val segmentColor = if (isFirstBounce) {
                // First bounce in contrasting color (dimmer version of accent)
                fillColor(tempColor, theme.accent[0] * 0.6f, theme.accent[1] * 0.6f, theme.accent[2] * 0.6f, 1f)
            } else {
                fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], 1f)
            }
            for (i in 1..steps) {
                val t = i.toFloat() / steps.toFloat()
                val alpha = (segmentAlpha * (1f - t)).coerceIn(0f, segmentAlpha)
                val size = 0.32f + (1f - t) * 0.18f
                val finalColor = fillColor(tempColor, segmentColor[0], segmentColor[1], segmentColor[2], alpha)
                renderer.drawCircle(
                    segStartX + dx * segmentLength * t,
                    segStartY + dy * segmentLength * t,
                    size,
                    finalColor
                )
            }
            val impactX = segStartX + dx * segmentLength
            val impactY = segStartY + dy * segmentLength
            if (hit.hitsBrick) {
                renderer.drawCircle(
                    impactX,
                    impactY,
                    0.7f,
                    fillColor(tempColor, theme.accent[0], theme.accent[1], theme.accent[2], 0.88f)
                )
                return
            }
            if (hit.nx == 0f && hit.ny == 0f) {
                // Fallback: continue in current direction instead of stopping abruptly
                // This prevents the aim guide from cutting off unexpectedly
                segStartX = impactX + dx * 0.02f
                segStartY = impactY + dy * 0.02f
                // Don't return, continue to next segment
            } else {
                if (hit.nx != 0f) dx = -dx
                if (hit.ny != 0f) dy = -dy
                segStartX = impactX + dx * 0.02f
                segStartY = impactY + dy * 0.02f
            }
        }
    }

internal fun GameEngine.drawStripe(renderer: Renderer2D, brick: Brick, color: FloatArray, count: Int) {
        if (count <= 0) return
        val stripeHeight = brick.height * 0.12f
        val gap = brick.height * 0.14f
        repeat(count) { index ->
            val y = brick.y + brick.height * 0.2f + index * gap
            renderer.drawRect(brick.x + brick.width * 0.08f, y, brick.width * 0.84f, stripeHeight, color)
        }
    }

internal fun GameEngine.drawInvaderShip(renderer: Renderer2D, brick: Brick, baseColor: FloatArray) {
        val time = renderTimeSeconds
        val hitPulse = brick.hitFlash.coerceIn(0f, 1f)
        val scale = 1f + hitPulse * 0.08f
        val wobble = if (hitPulse > 0f) {
            kotlin.math.sin(time * 18f + brick.gridX) * hitPulse * brick.width * 0.04f
        } else {
            0f
        }

        val baseW = brick.width
        val baseH = brick.height
        val w = baseW * scale
        val h = baseH * scale
        val x = brick.x + wobble - (w - baseW) * 0.5f
        val y = brick.y - (h - baseH) * 0.5f
        val variant = ((brick.gridX * 3 + brick.gridY * 5) % 4 + 4) % 4
        val tint = when (variant) {
            0 -> 1.0f
            1 -> 0.88f
            2 -> 1.12f
            else -> 0.96f
        }
        val pulseBoost = 1f + hitPulse * 0.25f

        val shadow = adjustColor(scratchColor0, baseColor, 0.35f, 0.35f)
        renderer.drawRect(x + w * 0.04f, y + h * 0.04f, w * 0.92f, h * 0.92f, shadow)

        val bodyHeight = when (variant) {
            0 -> 0.48f
            1 -> 0.42f
            2 -> 0.52f
            else -> 0.46f
        } * h
        val bodyY = when (variant) {
            0 -> 0.26f
            1 -> 0.3f
            2 -> 0.22f
            else -> 0.28f
        } * h
        val body = adjustColor(scratchColor1, baseColor, 0.95f * tint * pulseBoost, 1f)
        renderer.drawRect(x, y + bodyY, w, bodyHeight, body)

        val wingColor = adjustColor(scratchColor2, baseColor, 1.15f * tint * pulseBoost, 1f)
        val wingHeight = h * if (variant == 2) 0.32f else 0.28f
        renderer.drawRect(x + w * 0.06f, y + h * 0.12f, w * 0.2f, wingHeight, wingColor)
        renderer.drawRect(x + w * 0.74f, y + h * 0.12f, w * 0.2f, wingHeight, wingColor)

        val cockpit = adjustColor(scratchColor3, baseColor, 1.35f * pulseBoost, 1f)
        val cockpitRadius = h * when (variant) {
            1 -> 0.15f
            2 -> 0.2f
            else -> 0.18f
        }
        renderer.drawCircle(x + w * 0.5f, y + h * 0.58f, cockpitRadius, cockpit)

        if (variant == 2) {
            val light = adjustColor(scratchColor4, baseColor, 1.6f, 0.9f)
            renderer.drawCircle(x + w * 0.38f, y + h * 0.56f, h * 0.08f, light)
            renderer.drawCircle(x + w * 0.62f, y + h * 0.56f, h * 0.08f, light)
        }

        val engine = adjustColor(scratchColor5, baseColor, 1.5f * tint * pulseBoost, 0.9f)
        renderer.drawRect(x + w * 0.22f, y + h * 0.08f, w * 0.12f, h * 0.12f, engine)
        renderer.drawRect(x + w * 0.66f, y + h * 0.08f, w * 0.12f, h * 0.12f, engine)

        val rim = adjustColor(scratchColor6, baseColor, 0.7f * tint, 1f)
        renderer.drawRect(x + w * 0.08f, y + h * 0.7f, w * 0.84f, h * 0.06f, rim)

        if (variant == 1) {
            val fin = adjustColor(scratchColor7, baseColor, 1.25f, 0.9f)
            renderer.drawRect(x + w * 0.14f, y + h * 0.68f, w * 0.12f, h * 0.08f, fin)
            renderer.drawRect(x + w * 0.74f, y + h * 0.68f, w * 0.12f, h * 0.08f, fin)
        }
        if (variant == 3) {
            val ridge = adjustColor(scratchColor8, baseColor, 1.1f, 0.85f)
            renderer.drawRect(x + w * 0.46f, y + h * 0.34f, w * 0.08f, h * 0.28f, ridge)
        }

        if (brick.fireFlash > 0f) {
            val flashAlpha = (brick.fireFlash * 0.8f).coerceIn(0f, 0.8f)
            val flashColor = adjustColor(scratchColor9, baseColor, 1.5f, flashAlpha)
            val flashRadius = h * (0.16f + brick.fireFlash * 0.18f)
            renderer.drawCircle(x + w * 0.5f, y - h * 0.02f, flashRadius, flashColor)
            renderer.drawRect(
                x + w * 0.46f,
                y - h * 0.16f,
                w * 0.08f,
                h * 0.14f,
                adjustColor(scratchColor10, baseColor, 1.8f, flashAlpha)
            )
        }
    }

internal fun GameEngine.adjustColor(color: FloatArray, factor: Float, alpha: Float): FloatArray {
        return floatArrayOf(
            (color[0] * factor).coerceIn(0f, 1f),
            (color[1] * factor).coerceIn(0f, 1f),
            (color[2] * factor).coerceIn(0f, 1f),
            alpha
        )
    }

internal fun GameEngine.adjustColor(out: FloatArray, color: FloatArray, factor: Float, alpha: Float): FloatArray {
        out[0] = (color[0] * factor).coerceIn(0f, 1f)
        out[1] = (color[1] * factor).coerceIn(0f, 1f)
        out[2] = (color[2] * factor).coerceIn(0f, 1f)
        out[3] = alpha
        return out
    }

internal fun GameEngine.fillColor(out: FloatArray, r: Float, g: Float, b: Float, a: Float): FloatArray {
        out[0] = r
        out[1] = g
        out[2] = b
        out[3] = a
        return out
    }
