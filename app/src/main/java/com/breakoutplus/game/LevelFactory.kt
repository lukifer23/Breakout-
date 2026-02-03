package com.breakoutplus.game

import kotlin.math.max
import kotlin.math.roundToInt

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
        )
    )

    fun buildLevel(index: Int, difficulty: Float): LevelLayout {
        val base = levelPatterns[index % levelPatterns.size]
        val scaled = scaleLayout(base, difficulty)
        return scaled
    }

    private fun scaleLayout(layout: LevelLayout, difficulty: Float): LevelLayout {
        val bricks = layout.bricks.map { spec ->
            val baseHp = when (spec.type) {
                BrickType.NORMAL -> 1
                BrickType.REINFORCED -> 2
                BrickType.ARMORED -> 3
                BrickType.EXPLOSIVE -> 1
                BrickType.UNBREAKABLE -> 999
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
                    else -> null
                }
                if (type != null) {
                    val baseHp = when (type) {
                        BrickType.NORMAL -> 1
                        BrickType.REINFORCED -> 2
                        BrickType.ARMORED -> 3
                        BrickType.EXPLOSIVE -> 1
                        BrickType.UNBREAKABLE -> 999
                    }
                    bricks.add(BrickSpec(col, row, type, baseHp))
                }
            }
        }
        return bricks
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
            BrickType.UNBREAKABLE to floatArrayOf(0.62f, 0.64f, 0.72f, 1f)
        )
    )

    val NEON = DEFAULT.copy(
        name = "Neon",
        background = floatArrayOf(0.03f, 0.06f, 0.12f, 1f),
        paddle = floatArrayOf(0.97f, 0.97f, 1f, 1f)
    )

    val SUNSET = DEFAULT.copy(
        name = "Sunset",
        background = floatArrayOf(0.14f, 0.05f, 0.18f, 1f),
        accent = floatArrayOf(1f, 0.35f, 0.56f, 1f)
    )

    val COBALT = DEFAULT.copy(
        name = "Cobalt",
        background = floatArrayOf(0.05f, 0.09f, 0.2f, 1f),
        accent = floatArrayOf(0.32f, 0.73f, 1f, 1f)
    )

    val AURORA = DEFAULT.copy(
        name = "Aurora",
        background = floatArrayOf(0.06f, 0.1f, 0.17f, 1f),
        accent = floatArrayOf(0.38f, 0.88f, 0.66f, 1f)
    )

    val FOREST = DEFAULT.copy(
        name = "Forest",
        background = floatArrayOf(0.05f, 0.11f, 0.09f, 1f),
        accent = floatArrayOf(0.35f, 0.85f, 0.51f, 1f)
    )

    val LAVA = DEFAULT.copy(
        name = "Lava",
        background = floatArrayOf(0.11f, 0.05f, 0.05f, 1f),
        accent = floatArrayOf(1f, 0.43f, 0.2f, 1f)
    )
}
