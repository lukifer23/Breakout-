package com.breakoutplus.game

import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class GameState {
    READY, RUNNING, PAUSED, GAME_OVER
}

data class Ball(
    var x: Float,
    var y: Float,
    var radius: Float,
    var vx: Float,
    var vy: Float,
    var isFireball: Boolean = false,
    var color: FloatArray = floatArrayOf(0.97f, 0.97f, 1f, 1f),
    var stuckToPaddle: Boolean = false,
    var stickOffset: Float = 0f,
    var ricochetBounces: Int? = null
) {
    val defaultColor: FloatArray = floatArrayOf(0.97f, 0.97f, 1f, 1f)
    val trail: ArrayDeque<TrailPoint> = ArrayDeque()
    var trailTimer: Float = 0f
}

data class Paddle(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var targetX: Float = x
)

data class Brick(
    val gridX: Int,
    val gridY: Int,
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var baseX: Float = x,
    var baseY: Float = y,
    var hitPoints: Int,
    val maxHitPoints: Int,
    val type: BrickType,
    var alive: Boolean = true
) {
    companion object {
        internal val ROW_BANDS = arrayOf(
            floatArrayOf(0.08f, -0.02f, -0.05f),
            floatArrayOf(-0.03f, 0.06f, 0.02f),
            floatArrayOf(0.02f, 0.04f, -0.06f),
            floatArrayOf(-0.05f, -0.01f, 0.07f),
            floatArrayOf(0.06f, -0.04f, 0.03f)
        )
        internal val COL_BANDS = arrayOf(
            floatArrayOf(0.04f, 0.01f, -0.03f),
            floatArrayOf(-0.02f, 0.05f, 0.02f),
            floatArrayOf(0.03f, -0.04f, 0.04f),
            floatArrayOf(-0.04f, -0.02f, 0.05f)
        )
        internal val COOL_VARIANTS = arrayOf(
            floatArrayOf(0.32f, 0.84f, 0.98f),
            floatArrayOf(0.45f, 0.75f, 0.99f),
            floatArrayOf(0.46f, 0.88f, 0.76f),
            floatArrayOf(0.62f, 0.64f, 0.98f),
            floatArrayOf(0.86f, 0.62f, 0.95f),
            floatArrayOf(0.95f, 0.7f, 0.45f),
            floatArrayOf(0.56f, 0.94f, 0.5f),
            floatArrayOf(0.94f, 0.58f, 0.78f)
        )
        internal val WARM_VARIANTS = arrayOf(
            floatArrayOf(0.98f, 0.56f, 0.34f),
            floatArrayOf(0.95f, 0.72f, 0.4f),
            floatArrayOf(0.98f, 0.45f, 0.5f),
            floatArrayOf(0.88f, 0.68f, 0.3f),
            floatArrayOf(0.94f, 0.55f, 0.74f),
            floatArrayOf(0.74f, 0.76f, 0.4f),
            floatArrayOf(0.7f, 0.58f, 0.9f),
            floatArrayOf(0.89f, 0.46f, 0.36f)
        )
        internal val BALANCED_VARIANTS = arrayOf(
            floatArrayOf(0.54f, 0.84f, 0.97f),
            floatArrayOf(0.91f, 0.64f, 0.42f),
            floatArrayOf(0.45f, 0.9f, 0.65f),
            floatArrayOf(0.89f, 0.56f, 0.84f),
            floatArrayOf(0.96f, 0.79f, 0.38f),
            floatArrayOf(0.59f, 0.63f, 0.99f),
            floatArrayOf(0.94f, 0.5f, 0.56f),
            floatArrayOf(0.7f, 0.88f, 0.5f)
        )
        internal val BIAS_NORMAL = floatArrayOf(0f, 0f, 0f)
        internal val BIAS_REINFORCED = floatArrayOf(0.04f, -0.02f, 0.03f)
        internal val BIAS_ARMORED = floatArrayOf(-0.02f, 0.04f, -0.01f)
        internal val BIAS_EXPLOSIVE = floatArrayOf(0.12f, -0.08f, -0.07f)
        internal val BIAS_UNBREAKABLE = floatArrayOf(-0.04f, -0.03f, 0.06f)
        internal val BIAS_MOVING = floatArrayOf(-0.01f, 0.08f, 0.03f)
        internal val BIAS_SPAWNING = floatArrayOf(0.03f, 0.01f, 0.08f)
        internal val BIAS_PHASE = floatArrayOf(0.08f, 0.06f, -0.04f)
        internal val BIAS_BOSS = floatArrayOf(0.12f, -0.07f, -0.05f)
        internal val BIAS_INVADER = floatArrayOf(0.03f, 0.08f, 0.1f)
    }

    var hitFlash = 0f
    // Dynamic brick properties
    var vx: Float = 0f  // Horizontal velocity for moving bricks
    var vy: Float = 0f  // Vertical velocity
    var phase: Int = 0  // Current phase for phase bricks
    var maxPhase: Int = 1  // Total phases for phase bricks
    var spawnCount: Int = 0  // Number of spawns left for spawning bricks
    var lastHitTime: Float = 0f  // Timestamp of last hit for special effects
    var fireFlash: Float = 0f  // Invader firing glow
    internal var cachedThemeName: String? = null
    internal var cachedHitPoints: Int = -1
    internal var cachedColor: FloatArray? = null
    val scoreValue: Int = when (type) {
        BrickType.NORMAL -> 50
        BrickType.REINFORCED -> 80
        BrickType.ARMORED -> 120
        BrickType.EXPLOSIVE -> 150
        BrickType.UNBREAKABLE -> 200
        BrickType.MOVING -> 75
        BrickType.SPAWNING -> 100
        BrickType.PHASE -> 180
        BrickType.BOSS -> 300
        BrickType.INVADER -> 120
    }

    val centerX: Float
        get() = x + width / 2f
    val centerY: Float
        get() = y + height / 2f

    fun applyHit(forceBreak: Boolean): Boolean {
        if (type == BrickType.UNBREAKABLE && !forceBreak) {
            hitFlash = 0.2f
            return false
        }

        val damage = if (forceBreak && type == BrickType.UNBREAKABLE) 2 else 1
        hitPoints -= damage
        hitFlash = 0.2f
        lastHitTime = System.nanoTime() / 1_000_000_000f  // Current time in seconds

        // Special brick behaviors
        when (type) {
            BrickType.PHASE -> {
                if (hitPoints <= 0) {
                    phase++
                    if (phase >= maxPhase) {
                        alive = false
                        return true
                    } else {
                        // Reset hitpoints for next phase
                        hitPoints = max(1, maxHitPoints / (phase + 1))
                        hitFlash = 0.5f  // Longer flash for phase change
                        return false
                    }
                }
            }
            BrickType.BOSS -> {
                if (hitPoints <= 0) {
                    phase++
                    if (phase >= maxPhase) {
                        alive = false
                        return true
                    } else {
                        // Boss maintains strength across phases
                        hitPoints = maxHitPoints
                        hitFlash = 0.8f  // Dramatic flash for boss phase
                        // Could add screen shake or special effects here
                        return false
                    }
                }
            }
            BrickType.SPAWNING -> {
                if (hitPoints <= 0) {
                    alive = false
                    // Spawning logic will be handled externally when brick is destroyed
                    return true
                }
            }
            else -> {
                if (hitPoints <= 0) {
                    alive = false
                    return true
                }
            }
        }
        return false
    }

    fun currentColor(theme: LevelTheme): FloatArray {
        if (hitFlash <= 0f && cachedThemeName == theme.name && cachedHitPoints == hitPoints) {
            cachedColor?.let { return it }
        }
        val base = theme.brickPalette[type] ?: theme.accent
        val durability = if (type == BrickType.UNBREAKABLE) {
            1f
        } else {
            (hitPoints.toFloat() / maxHitPoints.toFloat()).coerceIn(0.35f, 1f)
        }
        val variants = variantsForTheme(theme.name)
        val seed = (gridX * 73856093) xor (gridY * 19349663) xor (type.ordinal * 83492791) xor theme.name.hashCode()
        val tint = variants[positiveMod(seed, variants.size)]
        val typeBias = biasForType()
        val tintMix = tintMixForType()
        val diversity = diversityForType()
        val rowBand = ROW_BANDS[positiveMod(gridY, ROW_BANDS.size)]
        val colBand = COL_BANDS[positiveMod(gridX, COL_BANDS.size)]
        val bandScale = when (type) {
            BrickType.NORMAL -> 0.22f
            BrickType.INVADER -> 0.2f
            BrickType.BOSS -> 0.09f
            else -> 0.14f
        } * diversity
        val brightness = 0.84f + durability * 0.24f
        val damageWarmth = (1f - durability) * when (type) {
            BrickType.EXPLOSIVE, BrickType.BOSS -> 0.12f
            BrickType.PHASE -> 0.09f
            else -> 0.06f
        }
        val mixR = mix(base[0], tint[0], tintMix)
        val mixG = mix(base[1], tint[1], tintMix)
        val mixB = mix(base[2], tint[2], tintMix)
        val finalColor = floatArrayOf(
            (mixR * brightness + typeBias[0] + (rowBand[0] + colBand[0]) * bandScale + damageWarmth).coerceIn(0.05f, 0.98f),
            (mixG * brightness + typeBias[1] + (rowBand[1] + colBand[1]) * bandScale).coerceIn(0.05f, 0.98f),
            (mixB * brightness + typeBias[2] + (rowBand[2] + colBand[2]) * bandScale - damageWarmth * 0.45f).coerceIn(0.05f, 0.98f),
            1f
        )

        if (hitFlash <= 0f) {
            cachedThemeName = theme.name
            cachedHitPoints = hitPoints
            cachedColor = finalColor
            return finalColor
        }
        val flashBoost = (0.2f + hitFlash * 0.85f).coerceIn(0.2f, 0.52f)
        return floatArrayOf(
            min(1f, finalColor[0] + flashBoost),
            min(1f, finalColor[1] + flashBoost),
            min(1f, finalColor[2] + flashBoost),
            1f
        )
    }

    internal fun variantsForTheme(themeName: String): Array<FloatArray> {
        return when (themeName) {
            "Sunset", "Lava", "Ember" -> WARM_VARIANTS
            "Neon", "Cobalt", "Circuit", "Invaders", "Vapor" -> COOL_VARIANTS
            else -> BALANCED_VARIANTS
        }
    }



    internal fun biasForType(): FloatArray {
        return when (type) {
            BrickType.NORMAL -> BIAS_NORMAL
            BrickType.REINFORCED -> BIAS_REINFORCED
            BrickType.ARMORED -> BIAS_ARMORED
            BrickType.EXPLOSIVE -> BIAS_EXPLOSIVE
            BrickType.UNBREAKABLE -> BIAS_UNBREAKABLE
            BrickType.MOVING -> BIAS_MOVING
            BrickType.SPAWNING -> BIAS_SPAWNING
            BrickType.PHASE -> BIAS_PHASE
            BrickType.BOSS -> BIAS_BOSS
            BrickType.INVADER -> BIAS_INVADER
        }
    }

    internal fun tintMixForType(): Float {
        return when (type) {
            BrickType.NORMAL -> 0.34f
            BrickType.REINFORCED -> 0.24f
            BrickType.ARMORED -> 0.21f
            BrickType.EXPLOSIVE -> 0.28f
            BrickType.UNBREAKABLE -> 0.12f
            BrickType.MOVING -> 0.29f
            BrickType.SPAWNING -> 0.28f
            BrickType.PHASE -> 0.33f
            BrickType.BOSS -> 0.2f
            BrickType.INVADER -> 0.32f
        }
    }

    internal fun diversityForType(): Float {
        return when (type) {
            BrickType.NORMAL -> 1f
            BrickType.MOVING, BrickType.PHASE, BrickType.SPAWNING, BrickType.INVADER -> 0.9f
            BrickType.BOSS -> 0.65f
            BrickType.UNBREAKABLE -> 0.55f
            else -> 0.75f
        }
    }

    internal fun mix(start: Float, end: Float, t: Float): Float {
        return start + (end - start) * t
    }

    internal fun positiveMod(value: Int, size: Int): Int {
        val mod = value % size
        return if (mod < 0) mod + size else mod
    }

    fun isNeighbor(other: Brick, radius: Int): Boolean {
        return abs(gridX - other.gridX) <= radius && abs(gridY - other.gridY) <= radius
    }
}

enum class BrickType { NORMAL, REINFORCED, ARMORED, EXPLOSIVE, UNBREAKABLE, MOVING, SPAWNING, PHASE, BOSS, INVADER }

data class PowerUp(
    var x: Float,
    var y: Float,
    val type: PowerUpType,
    val speed: Float,
    val size: Float = 3.2f
)

enum class PowerUpType(val displayName: String, val color: FloatArray) {
    MULTI_BALL("Multi-ball", floatArrayOf(0.19f, 0.88f, 0.97f, 1f)),
    LASER("Laser", floatArrayOf(1f, 0.31f, 0.84f, 1f)),
    GUARDRAIL("Guardrail", floatArrayOf(1f, 0.78f, 0.34f, 1f)),
    LIFE("Extra Life", floatArrayOf(0.14f, 0.92f, 0.64f, 1f)),
    SHIELD("Shield", floatArrayOf(0.52f, 0.61f, 1f, 1f)),
    WIDE_PADDLE("Wide Paddle", floatArrayOf(0.98f, 0.62f, 0.2f, 1f)),
    SHRINK("Shrink", floatArrayOf(1f, 0.45f, 0.35f, 1f)),
    SLOW("Slow", floatArrayOf(0.63f, 0.76f, 1f, 1f)),
    OVERDRIVE("Overdrive", floatArrayOf(1f, 0.62f, 0.22f, 1f)),
    FIREBALL("Fireball", floatArrayOf(1f, 0.36f, 0.27f, 1f)),
    MAGNET("Magnet", floatArrayOf(0.8f, 0.4f, 1f, 1f)),
    GRAVITY_WELL("Gravity Well", floatArrayOf(0.4f, 0.6f, 1f, 1f)),
    BALL_SPLITTER("Ball Splitter", floatArrayOf(1f, 0.8f, 0.2f, 1f)),
    FREEZE("Freeze", floatArrayOf(0.3f, 0.8f, 1f, 1f)),
    PIERCE("Pierce", floatArrayOf(0.9f, 0.5f, 0.1f, 1f)),
    RICOCHET("Ricochet", floatArrayOf(0.6f, 0.8f, 1f, 1f)),
    TIME_WARP("Time Warp", floatArrayOf(0.4f, 0.9f, 0.7f, 1f)),
    DOUBLE_SCORE("2x Score", floatArrayOf(1f, 0.8f, 0.3f, 1f))
}

data class Beam(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val speed: Float,
    val color: FloatArray
)

data class EnemyShot(
    var x: Float,
    var y: Float,
    val radius: Float,
    val vx: Float,
    val vy: Float,
    val color: FloatArray,
    val style: Int = 0,
    val wiggle: Float = 0f,
    val wobbleFreq: Float = 0f,
    var age: Float = 0f
)

data class TrailPoint(
    var x: Float,
    var y: Float,
    var radius: Float,
    var life: Float,
    val maxLife: Float
)

data class Particle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    var life: Float,
    val color: FloatArray
)

data class ExplosionWave(
    var x: Float,
    var y: Float,
    var radius: Float,
    val color: FloatArray,
    var life: Float,
    val maxLife: Float,
    val speed: Float,
    val chainCount: Int = 1
)
