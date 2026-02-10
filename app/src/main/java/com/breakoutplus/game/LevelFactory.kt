package com.breakoutplus.game

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Factory for generating game levels with brick layouts, themes, and procedural variation.
 * Provides both fixed patterns and endless mode generation with escalating difficulty.
 */
object LevelFactory {
    data class BrickSpec(val col: Int, val row: Int, val type: BrickType, val hitPoints: Int)
    data class LevelLayout(
        val rows: Int,
        val cols: Int,
        val bricks: List<BrickSpec>,
        val theme: LevelTheme,
        val tip: String
    )

    private val levelPatterns = listOf(
        LevelLayout(
            rows = 6,
            cols = 10,
            bricks = parse(
                listOf(
                    "NNNNNNNNNN",
                    "NRRNNRRNNR",
                    "NNENNENNNN",
                    "NRRNNRRNNR",
                    "NNNNNNNNNN",
                    "..U....U.."
                )
            ),
            theme = LevelThemes.NEON,
            tip = "Aim for the explosive bricks to clear clusters."
        ),
        LevelLayout(
            rows = 7,
            cols = 10,
            bricks = parse(
                listOf(
                    "..NNNNNN..",
                    ".NRRAARRN.",
                    ".NNEEEENN.",
                    "NRRNNNNRRN",
                    "NNA....ANN",
                    ".NRR..RRN.",
                    "..NNNNNN.."
                )
            ),
            theme = LevelThemes.SUNSET,
            tip = "Laser powerups let you cut through armor."
        ),
        LevelLayout(
            rows = 7,
            cols = 11,
            bricks = parse(
                listOf(
                    "NNNNNNNNNNN",
                    "NRRRNNNRRRN",
                    "NNNEEENNENN",
                    "NNAANNNNAAN",
                    "NNNNNNNNNNN",
                    "..U.....U..",
                    "..U.....U.."
                )
            ),
            theme = LevelThemes.COBALT,
            tip = "Stack guardrail with shield for safe rallies."
        ),
        LevelLayout(
            rows = 8,
            cols = 12,
            bricks = parse(
                listOf(
                    "NNNNNNNNNNNN",
                    "NRRNNRRNNRRN",
                    "NNAAANNNAAAN",
                    "NNEEENNEEENN",
                    "NNAAANNNAAAN",
                    "NRRNNRRNNRRN",
                    "NNNNNNNNNNNN",
                    "..U..U..U..."
                )
            ),
            theme = LevelThemes.AURORA,
            tip = "Explosive bricks chain through reinforced rows."
        ),
        LevelLayout(
            rows = 8,
            cols = 12,
            bricks = parse(
                listOf(
                    "NNNNNNNNNNNN",
                    "NRAAEEAARRAN",
                    "NNNNNNNNNNNN",
                    "NRRNNRRNNRRN",
                    "NNEEENNNEEEN",
                    "NNNNNNNNNNNN",
                    "..U..U..U...",
                    "..U..U..U..."
                )
            ),
            theme = LevelThemes.FOREST,
            tip = "Wide paddle helps control the faster ball speeds."
        ),
        LevelLayout(
            rows = 9,
            cols = 12,
            bricks = parse(
                listOf(
                    "NNNNNNNNNNNN",
                    "NRRRNNNRRRNN",
                    "NNAANNAANNAA",
                    "NNEEENNEEENN",
                    "NRRRNNNRRRNN",
                    "NNNNNNNNNNNN",
                    "..U..U..U...",
                    "..U..U..U...",
                    "..U..U..U..."
                )
            ),
            theme = LevelThemes.LAVA,
            tip = "Fireball lets you punch through unbreakable bricks."
        ),
        LevelLayout(
            rows = 8,
            cols = 12,
            bricks = parse(
                listOf(
                    "NNNNNNNNNNNN",
                    "NRRMMRRMMRRN",
                    "NNS..SS..SSN",
                    "NNAAANNNAAAN",
                    "NNEEENNNEEEN",
                    "NNNNNNNNNNNN",
                    "..U..U..U...",
                    "..M....M...."
                )
            ),
            theme = LevelThemes.COBALT,
            tip = "Moving and spawning bricks shift the tempo."
        ),
        LevelLayout(
            rows = 9,
            cols = 12,
            bricks = parse(
                listOf(
                    "NNPPNNPPNNPP",
                    "NRRAAARRAARR",
                    "NNSNNSSNNSSN",
                    "NNNNNNNNNNNN",
                    "NNEEENNNEEEN",
                    "..U..U..U...",
                    "..B....B....",
                    "..P..P..P...",
                    "NNNNNNNNNNNN"
                )
            ),
            theme = LevelThemes.AURORA,
            tip = "Phase and boss bricks demand sustained pressure."
        )
    )

    private val patternFillStartLevel = 0

    fun buildLevel(
        index: Int,
        difficulty: Float,
        endless: Boolean = false,
        themePool: List<LevelTheme> = LevelThemes.baseThemes(),
        forcedTheme: LevelTheme? = null
    ): LevelLayout {
        val pool = if (themePool.isNotEmpty()) themePool else LevelThemes.baseThemes()
        val resolvedTheme = forcedTheme ?: pool[index % pool.size]
        return if (endless && index >= levelPatterns.size) {
            // Procedural generation for endless mode beyond initial patterns
            generateProceduralLevel(index, difficulty, pool, resolvedTheme)
        } else {
            val base = levelPatterns[index % levelPatterns.size]
            val scaled = scaleLayout(base, difficulty)
            val themedLayout = scaled.copy(theme = resolvedTheme)
            if (index >= patternFillStartLevel) {
                fillPatternGaps(themedLayout, difficulty, seed = index * 31 + 7)
            } else {
                themedLayout
            }
        }
    }

    private fun scaleLayout(layout: LevelLayout, difficulty: Float): LevelLayout {
        val bricks = layout.bricks.map { spec ->
            val baseHp = when (spec.type) {
                BrickType.NORMAL -> 1
                BrickType.REINFORCED -> 2
                BrickType.ARMORED -> 3
                BrickType.EXPLOSIVE -> 1
                BrickType.UNBREAKABLE -> 999
                BrickType.MOVING -> 2
                BrickType.SPAWNING -> 2
                BrickType.PHASE -> 3
                BrickType.BOSS -> 6
                BrickType.INVADER -> 1
            }
            val hp = if (spec.type == BrickType.UNBREAKABLE) baseHp else max(1, (baseHp * difficulty).roundToInt())
            spec.copy(hitPoints = hp)
        }
        return layout.copy(bricks = bricks)
    }

    private fun parse(pattern: List<String>): List<BrickSpec> {
        val bricks = mutableListOf<BrickSpec>()
        val rows = pattern.size
        val cols = pattern.maxOf { it.length }
        pattern.forEachIndexed { row, line ->
            line.padEnd(cols, '.').forEachIndexed { col, ch ->
                val type = when (ch) {
                    'N' -> BrickType.NORMAL
                    'R' -> BrickType.REINFORCED
                    'A' -> BrickType.ARMORED
                    'E' -> BrickType.EXPLOSIVE
                    'U' -> BrickType.UNBREAKABLE
                    'M' -> BrickType.MOVING
                    'S' -> BrickType.SPAWNING
                    'P' -> BrickType.PHASE
                    'B' -> BrickType.BOSS
                    'I' -> BrickType.INVADER
                    else -> null
                }
                if (type != null) {
                    val baseHp = when (type) {
                        BrickType.NORMAL -> 1
                        BrickType.REINFORCED -> 2
                        BrickType.ARMORED -> 3
                        BrickType.EXPLOSIVE -> 1
                        BrickType.UNBREAKABLE -> 999
                        BrickType.MOVING -> 2
                        BrickType.SPAWNING -> 2
                        BrickType.PHASE -> 3
                        BrickType.BOSS -> 6
                        BrickType.INVADER -> 1
                    }
                    bricks.add(BrickSpec(col, row, type, baseHp))
                }
            }
        }
        return bricks
    }

    private fun fillPatternGaps(layout: LevelLayout, difficulty: Float, seed: Int): LevelLayout {
        val existing = layout.bricks.associateBy { it.col to it.row }
        val random = kotlin.random.Random(seed)
        val bricks = layout.bricks.toMutableList()
        val intensity = ((difficulty - 1f) / 2f).coerceIn(0f, 1f)
        for (row in 0 until layout.rows) {
            for (col in 0 until layout.cols) {
                if (existing.containsKey(col to row)) continue
                val rowRatio = if (layout.rows > 1) row.toFloat() / (layout.rows - 1).toFloat() else 0f
                val density = (0.88f + intensity * 0.08f - rowRatio * 0.08f).coerceIn(0.78f, 0.97f)
                if (random.nextFloat() > density) continue
                val typeRoll = random.nextFloat()
                val type = when {
                    typeRoll < 0.025f + intensity * 0.035f -> BrickType.EXPLOSIVE
                    typeRoll < 0.045f + intensity * 0.04f -> BrickType.MOVING
                    typeRoll < 0.065f + intensity * 0.035f -> BrickType.SPAWNING
                    typeRoll > 0.985f - intensity * 0.02f -> BrickType.PHASE
                    typeRoll > 0.92f -> BrickType.REINFORCED
                    typeRoll < 0.13f -> BrickType.ARMORED
                    else -> BrickType.NORMAL
                }
                val baseHp = when (type) {
                    BrickType.NORMAL -> 1
                    BrickType.REINFORCED -> 2
                    BrickType.ARMORED -> 3
                    BrickType.EXPLOSIVE -> 1
                    BrickType.UNBREAKABLE -> 999
                    BrickType.MOVING -> 2
                    BrickType.SPAWNING -> 2
                    BrickType.PHASE -> 3
                    BrickType.BOSS -> 6
                    BrickType.INVADER -> 1
                }
                val hp = if (type == BrickType.UNBREAKABLE) baseHp else max(1, (baseHp * difficulty).roundToInt())
                bricks.add(BrickSpec(col, row, type, hp))
            }
        }
        return layout.copy(bricks = bricks)
    }

    fun buildInvaderLevel(index: Int, difficulty: Float): LevelLayout {
        val rows = (7 + (index / 3).coerceAtMost(3)).coerceIn(7, 10)
        val cols = (13 + (index / 4).coerceAtMost(4)).coerceIn(13, 17)
        val formation = index % 3
        val bricks = mutableListOf<BrickSpec>()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val edgeGap = (col == 0 || col == cols - 1) && row == 0
                val seed = index * 41 + row * 13 + col * 7
                val roll = kotlin.random.Random(seed).nextFloat()
                val density = when (formation) {
                    1 -> 0.92f - row * 0.018f
                    2 -> 0.9f - row * 0.016f
                    else -> 0.94f - row * 0.018f
                }
                val staggerGap = formation == 1 && row % 2 == 1 && col % 3 == 0 && roll < 0.12f
                val windowGap = formation == 0 && row % 3 == 1 && col % 4 == 1 && roll < 0.18f
                val center = cols / 2
                val hollowGap = formation == 2 && row < rows - 1 &&
                    kotlin.math.abs(col - center) <= 1 && roll < 0.6f
                if (edgeGap || roll > density || staggerGap || windowGap || hollowGap) continue
                val tier = when {
                    row == 0 -> 3
                    row == 1 -> 2
                    else -> 1
                }
                val hp = max(1, (tier * (1f + index * 0.04f) * difficulty).roundToInt())
                bricks.add(BrickSpec(col, row, BrickType.INVADER, hp))
            }
        }

        return LevelLayout(
            rows = rows,
            cols = cols,
            bricks = bricks,
            theme = LevelThemes.INVADERS,
            tip = "Invaders: dodge enemy fire and protect your shield."
        )
    }

    private fun generateProceduralLevel(
        index: Int,
        difficulty: Float,
        themePool: List<LevelTheme>,
        forcedTheme: LevelTheme? = null
    ): LevelLayout {
        val themes = if (themePool.isNotEmpty()) themePool else LevelThemes.baseThemes()
        val theme = forcedTheme ?: themes[index % themes.size] // Rotate themes every level for endless variety

        val baseRows = 8 + (index / 5).coerceAtMost(3) // Add rows as levels progress
        val baseCols = 12 + (index / 4).coerceAtMost(3) // Add columns as levels progress
        val rows = (baseRows * (0.8f + kotlin.random.Random(index).nextFloat() * 0.4f)).toInt().coerceIn(6, 12)
        val cols = (baseCols * (0.8f + kotlin.random.Random(index + 1).nextFloat() * 0.4f)).toInt().coerceIn(10, 15)

        val bricks = mutableListOf<BrickSpec>()
        val density = 0.78f + (index * 0.01f).coerceAtMost(0.22f) // Increase density over time
        val occupied = mutableSetOf<Pair<Int, Int>>()

        // Create random but structured brick layout
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val rowRatio = if (rows > 1) row.toFloat() / (rows - 1).toFloat() else 0f
                val rowDensity = (density - rowRatio * 0.06f + if (row <= 1) 0.03f else 0f).coerceIn(0.72f, 0.98f)
                if (kotlin.random.Random(index * 31 + row * 17 + col).nextFloat() < rowDensity) {
                    val type = when {
                        row < 2 && index > 8 && kotlin.random.Random(index * 5 + row * 7 + col).nextFloat() < 0.04f -> BrickType.BOSS
                        row < 3 && index > 5 && kotlin.random.Random(index * 9 + row * 13 + col).nextFloat() < 0.06f -> BrickType.PHASE
                        row < 2 && kotlin.random.Random(index * 7 + row * 11 + col).nextFloat() < 0.12f -> BrickType.UNBREAKABLE
                        row < 3 && kotlin.random.Random(index * 13 + row * 19 + col).nextFloat() < 0.08f -> BrickType.EXPLOSIVE
                        row < 4 && kotlin.random.Random(index * 23 + row * 29 + col).nextFloat() < 0.15f -> BrickType.ARMORED
                        row < 5 && kotlin.random.Random(index * 29 + row * 31 + col).nextFloat() < 0.1f -> BrickType.SPAWNING
                        row < 6 && kotlin.random.Random(index * 41 + row * 43 + col).nextFloat() < 0.08f -> BrickType.MOVING
                        kotlin.random.Random(index * 37 + row * 41 + col).nextFloat() < 0.2f -> BrickType.REINFORCED
                        else -> BrickType.NORMAL
                    }
                    val baseHp = when (type) {
                        BrickType.NORMAL -> 1
                        BrickType.REINFORCED -> 2
                        BrickType.ARMORED -> 3
                        BrickType.EXPLOSIVE -> 1
                        BrickType.UNBREAKABLE -> 999
                        BrickType.MOVING -> 2
                        BrickType.SPAWNING -> 2
                        BrickType.PHASE -> 3
                        BrickType.BOSS -> 6
                        BrickType.INVADER -> 1
                    }
                    val hitPoints = if (type == BrickType.UNBREAKABLE) baseHp else (baseHp * difficulty).toInt().coerceAtLeast(1)
                    bricks.add(BrickSpec(col, row, type, hitPoints))
                    occupied.add(col to row)
                }
            }
        }

        val minimumBricks = (rows * cols * 0.45f).roundToInt()
        if (bricks.size < minimumBricks) {
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    if (bricks.size >= minimumBricks) break
                    val cell = col to row
                    if (occupied.contains(cell)) continue
                    val rowRatio = if (rows > 1) row.toFloat() / (rows - 1).toFloat() else 0f
                    val fillChance = (0.7f - rowRatio * 0.25f).coerceIn(0.3f, 0.8f)
                    if (kotlin.random.Random(index * 59 + row * 23 + col * 17).nextFloat() > fillChance) continue
                    val type = if ((row + col + index) % 7 == 0) BrickType.REINFORCED else BrickType.NORMAL
                    val baseHp = if (type == BrickType.REINFORCED) 2 else 1
                    val hp = (baseHp * difficulty).roundToInt().coerceAtLeast(1)
                    bricks.add(BrickSpec(col, row, type, hp))
                    occupied.add(cell)
                }
            }
        }

        return LevelLayout(
            rows = rows,
            cols = cols,
            bricks = bricks,
            theme = theme,
            tip = "Endless mode: Adaptive difficulty and variety!"
        )
    }
}

data class LevelTheme(
    val name: String,
    val background: FloatArray,
    val paddle: FloatArray,
    val accent: FloatArray,
    val brickPalette: Map<BrickType, FloatArray>
)

object LevelThemes {
    fun baseThemes(): List<LevelTheme> = listOf(NEON, SUNSET, COBALT, AURORA, FOREST, LAVA)

    fun bonusThemes(): List<LevelTheme> = listOf(CIRCUIT, VAPOR, EMBER)

    fun themeByName(name: String): LevelTheme? {
        return (baseThemes() + bonusThemes() + listOf(INVADERS, DEFAULT)).firstOrNull { it.name == name }
    }

    val DEFAULT = LevelTheme(
        name = "Default",
        background = floatArrayOf(0.04f, 0.07f, 0.13f, 1f),
        paddle = floatArrayOf(0.97f, 0.97f, 1f, 1f),
        accent = floatArrayOf(0.19f, 0.88f, 0.97f, 1f),
        brickPalette = mapOf(
            BrickType.NORMAL to floatArrayOf(0.19f, 0.88f, 0.97f, 1f),
            BrickType.REINFORCED to floatArrayOf(0.99f, 0.78f, 0.34f, 1f),
            BrickType.ARMORED to floatArrayOf(0.67f, 0.47f, 0.98f, 1f),
            BrickType.EXPLOSIVE to floatArrayOf(1f, 0.36f, 0.27f, 1f),
            BrickType.UNBREAKABLE to floatArrayOf(0.62f, 0.64f, 0.72f, 1f),
            BrickType.MOVING to floatArrayOf(0.35f, 0.92f, 0.51f, 1f),
            BrickType.SPAWNING to floatArrayOf(0.88f, 0.51f, 0.92f, 1f),
            BrickType.PHASE to floatArrayOf(0.95f, 0.65f, 0.25f, 1f),
            BrickType.BOSS to floatArrayOf(0.8f, 0.2f, 0.2f, 1f),
            BrickType.INVADER to floatArrayOf(0.6f, 0.9f, 1f, 1f)
        )
    )

    val NEON = LevelTheme(
        name = "Neon",
        background = floatArrayOf(0.03f, 0.06f, 0.12f, 1f),
        paddle = floatArrayOf(0.97f, 0.97f, 1f, 1f),
        accent = floatArrayOf(0.19f, 0.88f, 0.97f, 1f),
        brickPalette = mapOf(
            BrickType.NORMAL to floatArrayOf(0.2f, 0.9f, 1f, 1f),      // Bright cyan
            BrickType.REINFORCED to floatArrayOf(1f, 0.2f, 0.8f, 1f),   // Magenta
            BrickType.ARMORED to floatArrayOf(0.3f, 1f, 0.3f, 1f),      // Bright green
            BrickType.EXPLOSIVE to floatArrayOf(1f, 0.5f, 0f, 1f),      // Orange
            BrickType.UNBREAKABLE to floatArrayOf(0.8f, 0.8f, 0.8f, 1f), // Light gray
            BrickType.MOVING to floatArrayOf(0.4f, 1f, 0.6f, 1f),       // Lime green
            BrickType.SPAWNING to floatArrayOf(1f, 0.4f, 1f, 1f),       // Pink
            BrickType.PHASE to floatArrayOf(1f, 0.8f, 0.2f, 1f),        // Gold
            BrickType.BOSS to floatArrayOf(1f, 0.1f, 0.1f, 1f),         // Red
            BrickType.INVADER to floatArrayOf(0.45f, 0.95f, 1f, 1f)     // Neon cyan
        )
    )

    val SUNSET = LevelTheme(
        name = "Sunset",
        background = floatArrayOf(0.14f, 0.05f, 0.18f, 1f),
        paddle = floatArrayOf(1f, 0.8f, 0.6f, 1f),
        accent = floatArrayOf(1f, 0.35f, 0.56f, 1f),
        brickPalette = mapOf(
            BrickType.NORMAL to floatArrayOf(1f, 0.6f, 0.2f, 1f),       // Orange
            BrickType.REINFORCED to floatArrayOf(1f, 0.3f, 0.5f, 1f),    // Pink
            BrickType.ARMORED to floatArrayOf(0.8f, 0.4f, 0.8f, 1f),     // Purple
            BrickType.EXPLOSIVE to floatArrayOf(1f, 0.2f, 0f, 1f),       // Red-orange
            BrickType.UNBREAKABLE to floatArrayOf(0.7f, 0.7f, 0.5f, 1f), // Tan
            BrickType.MOVING to floatArrayOf(1f, 0.7f, 0.3f, 1f),        // Peach
            BrickType.SPAWNING to floatArrayOf(0.9f, 0.5f, 0.7f, 1f),    // Rose
            BrickType.PHASE to floatArrayOf(1f, 0.9f, 0.4f, 1f),         // Light gold
            BrickType.BOSS to floatArrayOf(0.9f, 0.1f, 0.2f, 1f),        // Crimson
            BrickType.INVADER to floatArrayOf(0.9f, 0.6f, 0.9f, 1f)      // Rose purple
        )
    )

    val COBALT = LevelTheme(
        name = "Cobalt",
        background = floatArrayOf(0.05f, 0.09f, 0.2f, 1f),
        paddle = floatArrayOf(0.7f, 0.8f, 1f, 1f),
        accent = floatArrayOf(0.32f, 0.73f, 1f, 1f),
        brickPalette = mapOf(
            BrickType.NORMAL to floatArrayOf(0.3f, 0.7f, 1f, 1f),        // Sky blue
            BrickType.REINFORCED to floatArrayOf(0.5f, 0.5f, 1f, 1f),     // Periwinkle
            BrickType.ARMORED to floatArrayOf(0.2f, 0.9f, 0.8f, 1f),      // Teal
            BrickType.EXPLOSIVE to floatArrayOf(1f, 0.4f, 0.6f, 1f),      // Hot pink
            BrickType.UNBREAKABLE to floatArrayOf(0.6f, 0.6f, 0.8f, 1f),  // Light blue-gray
            BrickType.MOVING to floatArrayOf(0.4f, 0.8f, 1f, 1f),         // Light blue
            BrickType.SPAWNING to floatArrayOf(0.7f, 0.5f, 1f, 1f),       // Lavender
            BrickType.PHASE to floatArrayOf(0.8f, 0.9f, 1f, 1f),          // Pale blue
            BrickType.BOSS to floatArrayOf(0.1f, 0.3f, 1f, 1f),           // Deep blue
            BrickType.INVADER to floatArrayOf(0.55f, 0.85f, 1f, 1f)       // Ice blue
        )
    )

    val AURORA = LevelTheme(
        name = "Aurora",
        background = floatArrayOf(0.06f, 0.1f, 0.17f, 1f),
        paddle = floatArrayOf(0.5f, 1f, 0.7f, 1f),
        accent = floatArrayOf(0.38f, 0.88f, 0.66f, 1f),
        brickPalette = mapOf(
            BrickType.NORMAL to floatArrayOf(0.3f, 1f, 0.5f, 1f),         // Mint green
            BrickType.REINFORCED to floatArrayOf(0.5f, 0.8f, 1f, 1f),      // Light blue
            BrickType.ARMORED to floatArrayOf(0.8f, 1f, 0.4f, 1f),         // Yellow-green
            BrickType.EXPLOSIVE to floatArrayOf(1f, 0.6f, 0.3f, 1f),       // Peach
            BrickType.UNBREAKABLE to floatArrayOf(0.7f, 0.8f, 0.6f, 1f),   // Sage green
            BrickType.MOVING to floatArrayOf(0.4f, 1f, 0.8f, 1f),          // Aquamarine
            BrickType.SPAWNING to floatArrayOf(0.9f, 0.7f, 1f, 1f),        // Light lavender
            BrickType.PHASE to floatArrayOf(0.9f, 1f, 0.6f, 1f),           // Pale yellow
            BrickType.BOSS to floatArrayOf(0.2f, 0.8f, 0.4f, 1f),          // Forest green
            BrickType.INVADER to floatArrayOf(0.5f, 1f, 0.7f, 1f)          // Mint
        )
    )

    val FOREST = LevelTheme(
        name = "Forest",
        background = floatArrayOf(0.05f, 0.11f, 0.09f, 1f),
        paddle = floatArrayOf(0.8f, 0.9f, 0.6f, 1f),
        accent = floatArrayOf(0.35f, 0.85f, 0.51f, 1f),
        brickPalette = mapOf(
            BrickType.NORMAL to floatArrayOf(0.4f, 0.8f, 0.3f, 1f),        // Grass green
            BrickType.REINFORCED to floatArrayOf(0.6f, 0.5f, 0.8f, 1f),     // Dusty purple
            BrickType.ARMORED to floatArrayOf(0.8f, 0.7f, 0.2f, 1f),        // Olive
            BrickType.EXPLOSIVE to floatArrayOf(0.9f, 0.4f, 0.2f, 1f),      // Burnt orange
            BrickType.UNBREAKABLE to floatArrayOf(0.6f, 0.5f, 0.4f, 1f),    // Brown
            BrickType.MOVING to floatArrayOf(0.5f, 0.9f, 0.4f, 1f),         // Light green
            BrickType.SPAWNING to floatArrayOf(0.7f, 0.6f, 0.9f, 1f),       // Light purple
            BrickType.PHASE to floatArrayOf(0.9f, 0.8f, 0.3f, 1f),          // Mustard
            BrickType.BOSS to floatArrayOf(0.3f, 0.6f, 0.2f, 1f),           // Dark green
            BrickType.INVADER to floatArrayOf(0.55f, 0.8f, 0.4f, 1f)        // Moss
        )
    )

    val LAVA = LevelTheme(
        name = "Lava",
        background = floatArrayOf(0.11f, 0.05f, 0.05f, 1f),
        paddle = floatArrayOf(1f, 0.6f, 0.3f, 1f),
        accent = floatArrayOf(1f, 0.43f, 0.2f, 1f),
        brickPalette = mapOf(
            BrickType.NORMAL to floatArrayOf(1f, 0.4f, 0f, 1f),             // Bright orange
            BrickType.REINFORCED to floatArrayOf(0.8f, 0.2f, 0.6f, 1f),     // Burgundy
            BrickType.ARMORED to floatArrayOf(0.9f, 0.6f, 0f, 1f),          // Dark orange
            BrickType.EXPLOSIVE to floatArrayOf(1f, 0.1f, 0f, 1f),          // Fire red
            BrickType.UNBREAKABLE to floatArrayOf(0.5f, 0.3f, 0.2f, 1f),    // Dark brown
            BrickType.MOVING to floatArrayOf(1f, 0.5f, 0.1f, 1f),           // Flame orange
            BrickType.SPAWNING to floatArrayOf(0.9f, 0.3f, 0.7f, 1f),       // Deep pink
            BrickType.PHASE to floatArrayOf(1f, 0.7f, 0.1f, 1f),            // Amber
            BrickType.BOSS to floatArrayOf(0.8f, 0f, 0f, 1f),               // Blood red
            BrickType.INVADER to floatArrayOf(1f, 0.5f, 0.9f, 1f)           // Neon pink
        )
    )

    val CIRCUIT = LevelTheme(
        name = "Circuit",
        background = floatArrayOf(0.02f, 0.08f, 0.1f, 1f),
        paddle = floatArrayOf(0.7f, 1f, 0.7f, 1f),
        accent = floatArrayOf(0.2f, 1f, 0.75f, 1f),
        brickPalette = mapOf(
            BrickType.NORMAL to floatArrayOf(0.25f, 1f, 0.75f, 1f),
            BrickType.REINFORCED to floatArrayOf(0.95f, 0.9f, 0.35f, 1f),
            BrickType.ARMORED to floatArrayOf(0.4f, 0.9f, 1f, 1f),
            BrickType.EXPLOSIVE to floatArrayOf(1f, 0.55f, 0.2f, 1f),
            BrickType.UNBREAKABLE to floatArrayOf(0.65f, 0.75f, 0.8f, 1f),
            BrickType.MOVING to floatArrayOf(0.35f, 1f, 0.6f, 1f),
            BrickType.SPAWNING to floatArrayOf(0.75f, 0.45f, 1f, 1f),
            BrickType.PHASE to floatArrayOf(1f, 0.95f, 0.4f, 1f),
            BrickType.BOSS to floatArrayOf(0.9f, 0.2f, 0.35f, 1f),
            BrickType.INVADER to floatArrayOf(0.4f, 0.95f, 0.85f, 1f)
        )
    )

    val VAPOR = LevelTheme(
        name = "Vapor",
        background = floatArrayOf(0.08f, 0.04f, 0.14f, 1f),
        paddle = floatArrayOf(0.98f, 0.7f, 0.9f, 1f),
        accent = floatArrayOf(0.45f, 0.9f, 0.95f, 1f),
        brickPalette = mapOf(
            BrickType.NORMAL to floatArrayOf(0.7f, 0.9f, 1f, 1f),
            BrickType.REINFORCED to floatArrayOf(1f, 0.55f, 0.8f, 1f),
            BrickType.ARMORED to floatArrayOf(0.5f, 1f, 0.8f, 1f),
            BrickType.EXPLOSIVE to floatArrayOf(1f, 0.4f, 0.6f, 1f),
            BrickType.UNBREAKABLE to floatArrayOf(0.75f, 0.7f, 0.9f, 1f),
            BrickType.MOVING to floatArrayOf(0.55f, 0.95f, 1f, 1f),
            BrickType.SPAWNING to floatArrayOf(0.95f, 0.7f, 1f, 1f),
            BrickType.PHASE to floatArrayOf(1f, 0.9f, 0.55f, 1f),
            BrickType.BOSS to floatArrayOf(0.85f, 0.2f, 0.65f, 1f),
            BrickType.INVADER to floatArrayOf(0.6f, 0.9f, 1f, 1f)
        )
    )

    val EMBER = LevelTheme(
        name = "Ember",
        background = floatArrayOf(0.1f, 0.04f, 0.05f, 1f),
        paddle = floatArrayOf(1f, 0.78f, 0.55f, 1f),
        accent = floatArrayOf(1f, 0.52f, 0.22f, 1f),
        brickPalette = mapOf(
            BrickType.NORMAL to floatArrayOf(1f, 0.55f, 0.2f, 1f),
            BrickType.REINFORCED to floatArrayOf(0.95f, 0.35f, 0.4f, 1f),
            BrickType.ARMORED to floatArrayOf(0.85f, 0.65f, 0.2f, 1f),
            BrickType.EXPLOSIVE to floatArrayOf(1f, 0.25f, 0.2f, 1f),
            BrickType.UNBREAKABLE to floatArrayOf(0.7f, 0.55f, 0.45f, 1f),
            BrickType.MOVING to floatArrayOf(1f, 0.6f, 0.3f, 1f),
            BrickType.SPAWNING to floatArrayOf(0.9f, 0.4f, 0.7f, 1f),
            BrickType.PHASE to floatArrayOf(1f, 0.8f, 0.35f, 1f),
            BrickType.BOSS to floatArrayOf(0.85f, 0.1f, 0.2f, 1f),
            BrickType.INVADER to floatArrayOf(1f, 0.6f, 0.5f, 1f)
        )
    )

    val INVADERS = LevelTheme(
        name = "Invaders",
        background = floatArrayOf(0.03f, 0.05f, 0.12f, 1f),
        paddle = floatArrayOf(0.92f, 0.95f, 1f, 1f),
        accent = floatArrayOf(0.35f, 0.85f, 1f, 1f),
        brickPalette = mapOf(
            BrickType.NORMAL to floatArrayOf(0.28f, 0.8f, 1f, 1f),
            BrickType.REINFORCED to floatArrayOf(0.8f, 0.45f, 1f, 1f),
            BrickType.ARMORED to floatArrayOf(0.4f, 1f, 0.7f, 1f),
            BrickType.EXPLOSIVE to floatArrayOf(1f, 0.45f, 0.4f, 1f),
            BrickType.UNBREAKABLE to floatArrayOf(0.7f, 0.75f, 0.85f, 1f),
            BrickType.MOVING to floatArrayOf(0.4f, 0.9f, 1f, 1f),
            BrickType.SPAWNING to floatArrayOf(0.85f, 0.55f, 1f, 1f),
            BrickType.PHASE to floatArrayOf(1f, 0.85f, 0.4f, 1f),
            BrickType.BOSS to floatArrayOf(1f, 0.2f, 0.25f, 1f),
            BrickType.INVADER to floatArrayOf(0.6f, 0.9f, 1f, 1f)
        )
    )
}
