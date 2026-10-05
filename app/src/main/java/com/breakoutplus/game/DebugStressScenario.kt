package com.breakoutplus.game

/** Controlled real-engine fixtures, reachable only through debuggable Android automation. */
internal fun GameEngine.prepareStressScenario(name: String) {
    require(name in setOf("dense", "multiball", "particles", "invaders", "volley", "tunnel"))
    levelIndex = if (name == "tunnel") 30 else 24
    resetLevel(first = false)
    lives = 999
    when (name) {
        "multiball" -> {
            launchBall()
            val source = balls.first()
            repeat(63) { i -> balls.add(source.copy(x = worldWidth * (0.1f + 0.8f * i / 63f),
                vx = (i % 7 - 3) * 7f, vy = source.vy)) }
        }
        "volley" -> volleyBallCount = 100
        "invaders" -> invaderShotCooldown = 0.08f
    }
    launchBall()
}

internal fun GameEngine.updateStressVisuals(dt: Float) {
    if (config.debugStressScenario != "particles") return
    // Exercise the existing confetti/explosion effect paths, without disk or network I/O.
    val previous = ((elapsedSeconds - dt) * 2).toInt()
    if ((elapsedSeconds * 2).toInt() != previous) {
        spawnLevelCompleteConfetti()
        bricks.firstOrNull { it.alive }?.let { spawnBrickDestructionFx(it, it.x, it.y, 2f) }
    }
}
