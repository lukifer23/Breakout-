package com.breakoutplus.game

internal fun GameEngine.shouldLogTouch(actionMasked: PointerAction, x: Float, y: Float, eventTimeMs: Long): Boolean {
        if (actionMasked != PointerAction.MOVE) {
            lastTouchLogTimeMs = eventTimeMs
            lastTouchLogX = x
            lastTouchLogY = y
            return true
        }
        val elapsedMs = eventTimeMs - lastTouchLogTimeMs
        val movedDistance = if (lastTouchLogX.isFinite() && lastTouchLogY.isFinite()) {
            kotlin.math.sqrt(
                (x - lastTouchLogX) * (x - lastTouchLogX) +
                    (y - lastTouchLogY) * (y - lastTouchLogY)
            )
        } else {
            Float.POSITIVE_INFINITY
        }
        val shouldLog = elapsedMs >= touchMoveLogMinIntervalMs || movedDistance >= touchMoveLogMinDistance
        if (shouldLog) {
            lastTouchLogTimeMs = eventTimeMs
            lastTouchLogX = x
            lastTouchLogY = y
        }
        return shouldLog
    }

internal fun GameEngine.handlePointerInput(event: PointerInput) {
        if (state == GameState.PAUSED || state == GameState.GAME_OVER) return

        val clampWorldX = { screenX: Float ->
            clampPaddleX(screenX * worldWidth)
        }
        val clampWorldY = { screenY: Float ->
            worldHeight - (screenY * worldHeight)
        }
        val pointerIndexForId = { pointerId: Int ->
            if (pointerId == PointerInput.INVALID_POINTER_ID) -1 else event.findPointerIndex(pointerId)
        }
        val pointerWorldX = { pointerId: Int ->
            val idx = pointerIndexForId(pointerId)
            if (idx in 0 until event.pointerCount) clampWorldX(event.getX(idx)) else null
        }
        val pointerWorldY = { pointerId: Int ->
            val idx = pointerIndexForId(pointerId)
            if (idx in 0 until event.pointerCount) clampWorldY(event.getY(idx)) else null
        }

        if (event.pointerCount <= 0) return

        val actionIndex = event.actionIndex.coerceIn(0, event.pointerCount - 1)
        val actionPointerId = event.getPointerId(actionIndex)
        val trackedPointerId = if (activePointerId != PointerInput.INVALID_POINTER_ID) activePointerId else actionPointerId
        val trackedX = pointerWorldX(trackedPointerId) ?: clampWorldX(event.getX(actionIndex))
        val trackedY = pointerWorldY(trackedPointerId) ?: clampWorldY(event.getY(actionIndex))
        val trackedPointerIndex = pointerIndexForId(trackedPointerId).coerceIn(0, event.pointerCount - 1)
        val trackedPressure = event.getPressure(trackedPointerIndex)

        // Log touch input
        val actionString = when (event.actionMasked) {
            PointerAction.DOWN -> "down"
            PointerAction.MOVE -> "move"
            PointerAction.POINTER_DOWN -> "pointer_down"
            PointerAction.POINTER_UP -> "pointer_up"
            PointerAction.UP -> "up"
            PointerAction.CANCEL -> "cancel"
            else -> "other"
        }
        if (logger != null && shouldLogTouch(event.actionMasked, trackedX, trackedY, event.eventTime)) {
            logger.logTouchInput(actionString, trackedX, trackedY, trackedPressure)
        }

        when (event.actionMasked) {
            PointerAction.POINTER_DOWN -> Unit
            PointerAction.DOWN -> {
                activePointerId = actionPointerId
                val downX = pointerWorldX(activePointerId) ?: trackedX
                val downY = pointerWorldY(activePointerId) ?: trackedY
                touchWorldX = downX
                touchWorldY = downY
                val snapToTouch = shouldSnapTouchToPaddle()
                updatePaddleFromTouch(
                    downX,
                    snapImmediately = snapToTouch
                )
                isDragging = true
                updateAimFromTouch()
                aimNormalized = aimNormalizedTarget
                applyAimFromNormalized(aimNormalized)
            }
            PointerAction.MOVE -> {
                if (activePointerId == PointerInput.INVALID_POINTER_ID) {
                    activePointerId = actionPointerId
                }
                val moveX = pointerWorldX(activePointerId) ?: trackedX
                val moveY = pointerWorldY(activePointerId) ?: trackedY
                touchWorldX = moveX
                touchWorldY = moveY
                val snapToTouch = shouldSnapTouchToPaddle()
                updatePaddleFromTouch(
                    moveX,
                    snapImmediately = snapToTouch
                )
                isDragging = true
                updateAimFromTouch()
                aimNormalized = aimNormalizedTarget
                applyAimFromNormalized(aimNormalized)
            }
            PointerAction.POINTER_UP -> {
                val liftedPointerId = actionPointerId
                if (liftedPointerId == activePointerId) {
                    var replacementIndex = -1
                    for (i in 0 until event.pointerCount) {
                        if (i == actionIndex) continue
                        replacementIndex = i
                        break
                    }
                    if (replacementIndex >= 0) {
                        activePointerId = event.getPointerId(replacementIndex)
                        touchWorldX = clampWorldX(event.getX(replacementIndex))
                        touchWorldY = clampWorldY(event.getY(replacementIndex))
                        val snapToTouch = shouldSnapTouchToPaddle()
                        updatePaddleFromTouch(
                            touchWorldX,
                            snapImmediately = snapToTouch
                        )
                        isDragging = true
                        updateAimFromTouch()
                        aimNormalized = aimNormalizedTarget
                        applyAimFromNormalized(aimNormalized)
                    } else {
                        activePointerId = PointerInput.INVALID_POINTER_ID
                        isDragging = false
                        updateAimFromPaddle()
                    }
                }
            }
            PointerAction.UP -> {
                val upX = pointerWorldX(actionPointerId) ?: trackedX
                val upY = pointerWorldY(actionPointerId) ?: trackedY
                touchWorldX = upX
                touchWorldY = upY
                updatePaddleFromTouch(upX, snapImmediately = true)
                syncAimForLaunch()
                if (state == GameState.READY) {
                    // Launch on tap/release for intuitive starts.
                    launchBall()
                    if (config.mode == GameMode.VOLLEY) {
                        listener.onTip("Volley launched. Bricks will descend when all balls return.")
                    } else {
                        listener.onTip("Tap with two fingers to fire when laser is active")
                    }
                } else if (magnetActive && hasStuckBall()) {
                    releaseStuckBalls()
                }
                isDragging = false
                activePointerId = PointerInput.INVALID_POINTER_ID
            }
            PointerAction.CANCEL -> {
                isDragging = false
                activePointerId = PointerInput.INVALID_POINTER_ID
                updateAimFromPaddle()
            }
        }
        if (event.actionMasked == PointerAction.POINTER_DOWN &&
            activeEffects.containsKey(PowerUpType.LASER)
        ) {
            shootLaser()
        }
    }

