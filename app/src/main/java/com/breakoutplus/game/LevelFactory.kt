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
        ),
        // Pattern 9: Diamond formation
        LevelLayout(
            rows = 8,
            cols = 12,
            bricks = parse(
                listOf(
                    ".....NN.....",
                    "....NNNN....",
                    "...NNRRNN...",
                    "..NRR..RRN..",
                    ".NN......NN.",
                    "..NRR..RRN..",
                    "...NNRRNN...",
                    "....NNNN...."
                )
            ),
            theme = LevelThemes.FOREST,
            tip = "The diamond formation creates tricky ricochet angles."
        ),
        // Pattern 10: Spiral layout
        LevelLayout(
            rows = 9,
            cols = 11,
            bricks = parse(
                listOf(
                    ".....N.....",
                    "....NNN....",
                    "...N...N...",
                    "..N.....N..",
                    ".N.......N.",
                    "..N.....N..",
                    "...N...N...",
                    "....NNN....",
                    ".....N....."
                )
            ),
            theme = LevelThemes.LAVA,
            tip = "Follow the spiral path for maximum point efficiency."
        ),
        // Pattern 11: Fortress corridor
        LevelLayout(
            rows = 8,
            cols = 14,
            bricks = parse(
                listOf(
                    "UUUUUUUUUUUUUU",
                    "U............U",
                    "U.NN......NN.U",
                    "U..RR....RR..U",
                    "U..RR....RR..U",
                    "U.NN......NN.U",
                    "U............U",
                    "UUUUUUUUUUUUUU"
                )
            ),
            theme = LevelThemes.COBALT,
            tip = "Break through the fortress walls strategically."
        ),
        // Pattern 12: Wave formation
        LevelLayout(
            rows = 7,
            cols = 12,
            bricks = parse(
                listOf(
                    "..NN....NN..",
                    ".N..NN..N..N",
                    "N.NN..NN.NN.",
                    ".N..NN..N..N",
                    "..NN....NN..",
                    ".N..NN..N..N",
                    "N.NN..NN.NN."
                )
            ),
            theme = LevelThemes.SUNSET,
            tip = "Ride the wave pattern for combo opportunities."
        ),
        // Pattern 13: Checkerboard with bosses
        LevelLayout(
            rows = 8,
            cols = 8,
            bricks = parse(
                listOf(
                    "NRNRNRNR",
                    "RNRNRNRN",
                    "NRNRNRNR",
                    "RNRNRNRN",
                    "NRNRNRNR",
                    "RNB.NBRN",
                    "NRNRNRNR",
                    "RNRNRNRN"
                )
            ),
            theme = LevelThemes.AURORA,
            tip = "Boss bricks in the checkerboard require careful planning."
        ),
        // Pattern 14: Moving brick maze
        LevelLayout(
            rows = 8,
            cols = 12,
            bricks = parse(
                listOf(
                    "MMMMMMMMMMMM",
                    "M..........M",
                    "M.NN....NN.M",
                    "M..MM..MM..M",
                    "M..MM..MM..M",
                    "M.NN....NN.M",
                    "M..........M",
                    "MMMMMMMMMMMM"
                )
            ),
            theme = LevelThemes.NEON,
            tip = "Moving bricks create dynamic challenges."
        ),
        // Pattern 15: Spawning fortress
        LevelLayout(
            rows = 9,
            cols = 11,
            bricks = parse(
                listOf(
                    ".....U.....",
                    "....UUU....",
                    "...U...U...",
                    "..U.....U..",
                    ".U..SSS..U.",
                    "..U.....U..",
                    "...U...U...",
                    "....UUU....",
                    ".....U....."
                )
            ),
            theme = LevelThemes.CIRCUIT,
            tip = "Spawning bricks multiply as you destroy them."
        ),
        // Pattern 16: Boss arena
        LevelLayout(
            rows = 6,
            cols = 10,
            bricks = parse(
                listOf(
                    "....BB....",
                    "...B..B...",
                    "..B....B..",
                    "...B..B...",
                    "....BB....",
                    "NNNNNNNNNN"
                )
            ),
            theme = LevelThemes.VAPOR,
            tip = "Boss bricks require multiple hits - plan your approach."
        ),
        // Pattern 17: Diamond formation
        LevelLayout(
            rows = 8,
            cols = 10,
            bricks = parse(
                listOf(
                    "...NNN....",
                    "..NRRRN...",
                    ".N.....N..",
                    "N..AAA..N.",
                    "N..AAA..N.",
                    ".N.....N..",
                    "..NRRRN...",
                    "...NNN...."
                )
            ),
            theme = LevelThemes.COBALT,
            tip = "Diamond core requires precision targeting."
        ),
        // Pattern 18: Spiral cascade
        LevelLayout(
            rows = 8,
            cols = 10,
            bricks = parse(
                listOf(
                    "N.........",
                    "RN........",
                    "ARN.......",
                    "EARN......",
                    ".EARN.....",
                    "..EARN....",
                    "...EARN...",
                    "....EARN.."
                )
            ),
            theme = LevelThemes.SUNSET,
            tip = "Follow the spiral for maximum combo potential."
        ),
        // Pattern 19: Pillar defense
        LevelLayout(
            rows = 8,
            cols = 10,
            bricks = parse(
                listOf(
                    "N..N..N..N",
                    "R..R..R..R",
                    "A..A..A..A",
                    "N..N..N..N",
                    "R..R..R..R",
                    "A..A..A..A",
                    "N..N..N..N",
                    "R..R..R..R"
                )
            ),
            theme = LevelThemes.AURORA,
            tip = "Pillar formations create strategic routing challenges."
        ),
        // Pattern 20: Grid gaps
        LevelLayout(
            rows = 8,
            cols = 10,
            bricks = parse(
                listOf(
                    "N.N.N.N.N.",
                    ".N.N.N.N.N",
                    "N.N.N.N.N.",
                    ".N.N.N.N.N",
                    "N.N.N.N.N.",
                    ".N.N.N.N.N",
                    "N.N.N.N.N.",
                    ".N.N.N.N.N"
                )
            ),
            theme = LevelThemes.NEON,
            tip = "Grid patterns reward careful angle selection."
        ),
        // Pattern 21: Cascade waterfall
        LevelLayout(
            rows = 8,
            cols = 10,
            bricks = parse(
                listOf(
                    "....N.....",
                    "...NRN....",
                    "..N...N...",
                    ".NR...RN..",
                    "N..R.R..N.",
                    ".NR...RN..",
                    "..N...N...",
                    "...NRN...."
                )
            ),
            theme = LevelThemes.CIRCUIT,
            tip = "Cascade patterns flow like water - find the current."
        ),
        // Pattern 22: Concentric rings
        LevelLayout(
            rows = 8,
            cols = 10,
            bricks = parse(
                listOf(
                    "...NNN....",
                    "..N...N...",
                    ".N..E..N..",
                    "N..R.R..N.",
                    "N..R.R..N.",
                    ".N..E..N..",
                    "..N...N...",
                    "...NNN...."
                )
            ),
            theme = LevelThemes.VAPOR,
            tip = "Ring formations require working from outside in."
        ),
        // Pattern 23: Dense block formation
        LevelLayout(
            rows = 8,
            cols = 12,
            bricks = parse(
                listOf(
                    "NNNNNNNNNNNN",
                    "NNNNNNNNNNNN",
                    "NNNNNNNNNNNN",
                    "NNNNNNNNNNNN",
                    "NNNNNNNNNNNN",
                    "NNNNNNNNNNNN",
                    "..RRRRRRRR..",
                    "..RRRRRRRR.."
                )
            ),
            theme = LevelThemes.COBALT,
            tip = "Dense formations reward strategic clearing patterns."
        ),
        // Pattern 24: Compact fortress
        LevelLayout(
            rows = 9,
            cols = 11,
            bricks = parse(
                listOf(
                    "NNNNNNNNNNN",
                    "NNNNNNNNNNN",
                    "NNNNNNNNNNN",
                    "NNNNNNNNNNN",
                    "NNNNNNNNNNN",
                    "NNNNNNNNNNN",
                    "NNNNNNNNNNN",
                    "..AAAAAAA..",
                    "..AAAAAAA.."
                )
            ),
            theme = LevelThemes.LAVA,
            tip = "Fortress walls protect the armored core within."
        ),
        // Pattern 25: Solid mass with weak points
        LevelLayout(
            rows = 8,
            cols = 12,
            bricks = parse(
                listOf(
                    "NNNNNNNNNNNN",
                    "NNNNNNNNNNNN",
                    "NNNNNNNNNNNN",
                    "NNNNN..NNNNN",
                    "NNNN....NNNN",
                    "NNNN....NNNN",
                    "NNNNN..NNNNN",
                    "NNNNNNNNNNNN"
                )
            ),
            theme = LevelThemes.FOREST,
            tip = "Find the weak points in this solid mass formation."
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
                fillPatternGaps(themedLayout, difficulty, index, seed = index * 31 + 7)
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

    private fun fillPatternGaps(layout: LevelLayout, difficulty: Float, levelIndex: Int, seed: Int): LevelLayout {
        val existing = layout.bricks.associateBy { it.col to it.row }
        val random = kotlin.random.Random(seed)
        val bricks = layout.bricks.toMutableList()
        val intensity = ((difficulty - 1f) / 2f).coerceIn(0f, 1f)
        // Gradual density ramp based on level ranges
        val baseDensity = when {
            levelIndex <= 5 -> 0.55f + levelIndex * 0.02f // Levels 0-5: 55% to 65%
            levelIndex <= 10 -> 0.68f + (levelIndex - 5) * 0.025f // Levels 6-10: 68% to 80%
            else -> 0.82f // Levels 11+: 82%
        }.coerceIn(0.55f, 0.9f)
        for (row in 0 until layout.rows) {
            for (col in 0 until layout.cols) {
                if (existing.containsKey(col to row)) continue
                val rowRatio = if (layout.rows > 1) row.toFloat() / (layout.rows - 1).toFloat() else 0f
                val density = (baseDensity + intensity * 0.09f - rowRatio * 0.08f).coerceIn(0.45f, 0.9f)
                if (random.nextFloat() > density) continue
                val typeRoll = random.nextFloat()
                val type = when {
                    typeRoll < 0.045f + intensity * 0.055f -> BrickType.EXPLOSIVE
                    typeRoll < 0.075f + intensity * 0.06f -> BrickType.MOVING
                    typeRoll < 0.095f + intensity * 0.055f -> BrickType.SPAWNING
                    typeRoll > 0.975f - intensity * 0.03f -> BrickType.PHASE
                    typeRoll > 0.88f -> BrickType.REINFORCED
                    typeRoll < 0.18f -> BrickType.ARMORED
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

    fun buildTunnelLevel(index: Int, difficulty: Float, theme: LevelTheme): LevelLayout {
        val rows = (12 + (index / 4).coerceAtMost(4)).coerceIn(12, 16)
        val cols = (13 + (index / 5).coerceAtMost(3)).coerceIn(13, 16)
        val bricks = mutableListOf<BrickSpec>()
        val occupied = HashSet<Pair<Int, Int>>(rows * cols)

        fun addBrick(col: Int, row: Int, type: BrickType, hp: Int) {
            if (col !in 0 until cols || row !in 0 until rows) return
            if (!occupied.add(col to row)) return
            bricks.add(BrickSpec(col, row, type, hp))
        }

        fun baseHp(type: BrickType): Int {
            return when (type) {
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
        }

        // Castle Layout Dimensions
        val marginX = 1
        val marginY = 1
        val castleLeft = marginX
        val castleRight = cols - 1 - marginX
        val castleTop = marginY
        val castleBottom = rows - 4

        // 1. Towers (Corners)
        val towerSize = 2
        val corners = listOf(
            castleLeft to castleTop,
            castleRight - towerSize + 1 to castleTop,
            castleLeft to castleBottom - towerSize + 1,
            castleRight - towerSize + 1 to castleBottom - towerSize + 1
        )
        for ((tx, ty) in corners) {
            for (dx in 0 until towerSize) {
                for (dy in 0 until towerSize) {
                    addBrick(tx + dx, ty + dy, BrickType.UNBREAKABLE, 999)
                }
            }
        }

        // 2. Walls with Weak Points
        // Horizontal Walls
        for (col in (castleLeft + towerSize) until (castleRight - towerSize + 1)) {
            // Top Wall: Periodically weak
            val isWeakTop = (col + index) % 5 == 0
            val topType = if (isWeakTop) BrickType.ARMORED else BrickType.UNBREAKABLE
            addBrick(col, castleTop, topType, if (isWeakTop) (4 * difficulty).toInt() else 999)
        }

        // Vertical Walls
        for (row in (castleTop + towerSize) until (castleBottom - towerSize + 1)) {
            val isWeakSide = (row + index) % 4 == 0
            val sideType = if (isWeakSide) BrickType.ARMORED else BrickType.UNBREAKABLE
            addBrick(castleLeft, row, sideType, if (isWeakSide) (4 * difficulty).toInt() else 999)
            addBrick(castleRight, row, sideType, if (isWeakSide) (4 * difficulty).toInt() else 999)
        }

        // 3. The Gate (Bottom Center)
        val gateWidth = 2 + (index % 2)
        val gateCenter = cols / 2
        val gateLeft = gateCenter - gateWidth / 2
        val gateRight = gateLeft + gateWidth - 1

        // Bottom Wall (interrupted by Gate)
        for (col in (castleLeft + towerSize) until (castleRight - towerSize + 1)) {
            if (col in gateLeft..gateRight) {
                // The Gate: Reinforced Portcullis
                if (index > 2) {
                    addBrick(col, castleBottom, BrickType.REINFORCED, (3 * difficulty).toInt())
                }
            } else {
                val isWeakBot = (col + index * 2) % 6 == 0
                val botType = if (isWeakBot) BrickType.ARMORED else BrickType.UNBREAKABLE
                addBrick(col, castleBottom, botType, if (isWeakBot) (4 * difficulty).toInt() else 999)
            }
        }

        // 4. Entry Tunnel (Leading to Gate)
        for (row in (castleBottom + 1) until rows) {
            addBrick(gateLeft - 1, row, BrickType.UNBREAKABLE, 999)
            addBrick(gateRight + 1, row, BrickType.UNBREAKABLE, 999)
        }

        // 5. Interior (The Keep & Guards)
        val interiorDensity = 0.65f + (index * 0.012f).coerceAtMost(0.25f)
        for (row in (castleTop + 1) until castleBottom) {
            for (col in (castleLeft + 1) until castleRight) {
                if (occupied.contains(col to row)) continue
                
                // Keep the path immediately behind the gate clear for entry
                if (col in gateLeft..gateRight && row >= castleBottom - 2) continue

                val seed = index * 101 + row * 43 + col * 17
                val roll = kotlin.random.Random(seed).nextFloat()
                
                // Higher density in the "Keep" (center)
                val distFromCenter = kotlin.math.abs(col - gateCenter) + kotlin.math.abs(row - (castleTop + castleBottom) / 2)
                val centerBonus = if (distFromCenter < 3) 0.2f else 0f
                val effectiveDensity = (interiorDensity + centerBonus).coerceAtMost(0.95f)

                if (roll < effectiveDensity) {
                    val typeRoll = kotlin.random.Random(seed + 1).nextFloat()
                     val type = when {
                        typeRoll < 0.05f -> BrickType.EXPLOSIVE
                        typeRoll < 0.25f -> BrickType.REINFORCED
                        typeRoll < 0.40f -> BrickType.ARMORED
                        typeRoll < 0.45f -> BrickType.PHASE
                        typeRoll < 0.48f -> BrickType.SPAWNING
                        typeRoll < 0.52f -> BrickType.MOVING
                        else -> BrickType.NORMAL
                    }
                    val hp = max(1, (baseHp(type) * difficulty * (1f + index * 0.02f)).roundToInt())
                    addBrick(col, row, type, hp)
                }
            }
        }

        return LevelLayout(
            rows = rows,
            cols = cols,
            bricks = bricks,
            theme = theme,
            tip = "Siege: Breach the castle walls or storm the gate!"
        )
    }

    private fun generateProceduralLevel(
        index: Int,
        difficulty: Float,
        themePool: List<LevelTheme>,
        forcedTheme: LevelTheme? = null
    ): LevelLayout {
        val themes = if (themePool.isNotEmpty()) themePool else LevelThemes.baseThemes()
        val theme = forcedTheme ?: themes[index % themes.size]

        val baseRows = 8 + (index / 5).coerceAtMost(3)
        val baseCols = 12 + (index / 4).coerceAtMost(3)
        val rows = (baseRows * (0.8f + kotlin.random.Random(index).nextFloat() * 0.4f)).toInt().coerceIn(6, 12)
        val cols = (baseCols * (0.8f + kotlin.random.Random(index + 1).nextFloat() * 0.4f)).toInt().coerceIn(10, 15)

        val bricks = mutableListOf<BrickSpec>()
        val occupied = mutableSetOf<Pair<Int, Int>>()

        // Choose a procedural template based on level
        val templateType = (index + kotlin.random.Random(index).nextInt(5)) % 7
        when (templateType) {
            0 -> generateSymmetricLayout(rows, cols, index, difficulty, bricks, occupied)
            1 -> generateClusterLayout(rows, cols, index, difficulty, bricks, occupied)
            2 -> generateWaveLayout(rows, cols, index, difficulty, bricks, occupied)
            3 -> generateFortressLayout(rows, cols, index, difficulty, bricks, occupied)
            4 -> generateMazeLayout(rows, cols, index, difficulty, bricks, occupied)
            5 -> generateScatterLayout(rows, cols, index, difficulty, bricks, occupied)
            6 -> generateMirrorLayout(rows, cols, index, difficulty, bricks, occupied)
        }

        // Add special brick types based on difficulty and level
        addSpecialBricks(rows, cols, index, difficulty, bricks, occupied)

        val minimumDensity = (0.56f + (index * 0.003f).coerceAtMost(0.12f)).coerceIn(0.56f, 0.7f)
        val minimumBricks = (rows * cols * minimumDensity).roundToInt()
        if (bricks.size < minimumBricks) {
            fillRemainingSpaces(rows, cols, index, difficulty, bricks, occupied, minimumBricks)
        }

        return LevelLayout(
            rows = rows,
            cols = cols,
            bricks = bricks,
            theme = theme,
            tip = "Endless mode: Adaptive difficulty and variety!"
        )
    }

    private fun generateSymmetricLayout(rows: Int, cols: Int, index: Int, difficulty: Float,
                                       bricks: MutableList<BrickSpec>, occupied: MutableSet<Pair<Int, Int>>) {
        val random = kotlin.random.Random(index * 13)
        val symmetryType = random.nextInt(3) // 0=horizontal, 1=vertical, 2=both

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (occupied.contains(col to row)) continue

                var shouldPlace = false
                val centerRow = rows / 2
                val centerCol = cols / 2

                when (symmetryType) {
                    0 -> { // Horizontal symmetry
                        val mirrorRow = rows - 1 - row
                        if (mirrorRow >= row) {
                            val density = 0.65f + (index * 0.008f).coerceAtMost(0.25f)
                            shouldPlace = random.nextFloat() < density
                        }
                    }
                    1 -> { // Vertical symmetry
                        val mirrorCol = cols - 1 - col
                        if (mirrorCol >= col) {
                            val density = 0.65f + (index * 0.008f).coerceAtMost(0.25f)
                            shouldPlace = random.nextFloat() < density
                        }
                    }
                    2 -> { // Radial symmetry
                        val distFromCenter = kotlin.math.sqrt(((row - centerRow) * (row - centerRow) +
                                (col - centerCol) * (col - centerCol)).toFloat())
                        val maxDist = kotlin.math.sqrt((centerRow * centerRow + centerCol * centerCol).toFloat())
                        val density = 0.6f + (1f - distFromCenter / maxDist) * 0.3f + (index * 0.005f).coerceAtMost(0.15f)
                        shouldPlace = random.nextFloat() < density
                    }
                }

                if (shouldPlace) {
                    val type = if (random.nextFloat() < 0.15f) BrickType.REINFORCED else BrickType.NORMAL
                    val baseHp = if (type == BrickType.REINFORCED) 2 else 1
                    val hp = (baseHp * difficulty).toInt().coerceAtLeast(1)
                    bricks.add(BrickSpec(col, row, type, hp))
                    occupied.add(col to row)

                    // Add symmetric counterparts
                    when (symmetryType) {
                        0 -> {
                            val mirrorRow = rows - 1 - row
                            if (mirrorRow != row && mirrorRow >= 0 && mirrorRow < rows) {
                                bricks.add(BrickSpec(col, mirrorRow, type, hp))
                                occupied.add(col to mirrorRow)
                            }
                        }
                        1 -> {
                            val mirrorCol = cols - 1 - col
                            if (mirrorCol != col && mirrorCol >= 0 && mirrorCol < cols) {
                                bricks.add(BrickSpec(mirrorCol, row, type, hp))
                                occupied.add(mirrorCol to row)
                            }
                        }
                        2 -> {
                            val mirrorRow = rows - 1 - row
                            val mirrorCol = cols - 1 - col
                            if (mirrorRow >= 0 && mirrorRow < rows) {
                                bricks.add(BrickSpec(col, mirrorRow, type, hp))
                                occupied.add(col to mirrorRow)
                            }
                            if (mirrorCol >= 0 && mirrorCol < cols) {
                                bricks.add(BrickSpec(mirrorCol, row, type, hp))
                                occupied.add(mirrorCol to row)
                            }
                            if (mirrorRow >= 0 && mirrorRow < rows && mirrorCol >= 0 && mirrorCol < cols) {
                                bricks.add(BrickSpec(mirrorCol, mirrorRow, type, hp))
                                occupied.add(mirrorCol to mirrorRow)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun generateClusterLayout(rows: Int, cols: Int, index: Int, difficulty: Float,
                                     bricks: MutableList<BrickSpec>, occupied: MutableSet<Pair<Int, Int>>) {
        val random = kotlin.random.Random(index * 17)
        val numClusters = 3 + random.nextInt(4) // 3-6 clusters

        repeat(numClusters) { clusterIndex ->
            val clusterSize = 4 + random.nextInt(6) // 4-9 bricks per cluster
            val startRow = random.nextInt(rows - 3)
            val startCol = random.nextInt(cols - 3)

            repeat(clusterSize) { brickIndex ->
                val offsetRow = random.nextInt(3) - 1 // -1 to +1
                val offsetCol = random.nextInt(3) - 1
                val row = (startRow + offsetRow).coerceIn(0, rows - 1)
                val col = (startCol + offsetCol).coerceIn(0, cols - 1)

                if (!occupied.contains(col to row)) {
                    val type = if (random.nextFloat() < 0.2f) BrickType.REINFORCED else BrickType.NORMAL
                    val baseHp = if (type == BrickType.REINFORCED) 2 else 1
                    val hp = (baseHp * difficulty).toInt().coerceAtLeast(1)
                    bricks.add(BrickSpec(col, row, type, hp))
                    occupied.add(col to row)
                }
            }
        }
    }

    private fun generateWaveLayout(rows: Int, cols: Int, index: Int, difficulty: Float,
                                  bricks: MutableList<BrickSpec>, occupied: MutableSet<Pair<Int, Int>>) {
        val random = kotlin.random.Random(index * 19)
        val waveFrequency = 2 + random.nextInt(3) // 2-4 waves

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val waveOffset = kotlin.math.sin(col.toFloat() / cols * waveFrequency * kotlin.math.PI * 2) *
                                (rows.toFloat() / 4f)
                val targetRow = row + waveOffset.toInt()
                val rowMatch = kotlin.math.abs(row - targetRow) <= 1

                if (rowMatch && random.nextFloat() < 0.75f && !occupied.contains(col to row)) {
                    val type = if (random.nextFloat() < 0.15f) BrickType.REINFORCED else BrickType.NORMAL
                    val baseHp = if (type == BrickType.REINFORCED) 2 else 1
                    val hp = (baseHp * difficulty).toInt().coerceAtLeast(1)
                    bricks.add(BrickSpec(col, row, type, hp))
                    occupied.add(col to row)
                }
            }
        }
    }

    private fun generateFortressLayout(rows: Int, cols: Int, index: Int, difficulty: Float,
                                      bricks: MutableList<BrickSpec>, occupied: MutableSet<Pair<Int, Int>>) {
        val random = kotlin.random.Random(index * 23)
        // Castle Layout for Endless Mode
        val towerSize = 2
        val margin = 1
        
        // Define Towers
        val corners = listOf(
            margin to margin,
            cols - 1 - margin to margin,
            margin to rows - 1 - margin,
            cols - 1 - margin to rows - 1 - margin
        )
        // Build Towers
        for ((tx, ty) in corners) {
            for (dx in 0 until towerSize) {
                for (dy in 0 until towerSize) {
                    // Check if inside bounds (account for tower size direction)
                    // Simplified: just build block around tx,ty depending on corner
                    // Note: tx,ty are top-left, top-right, bot-left, bot-right
                    // We need to adjust direction.
                    // Easier: Just treat tx,ty as Top-Left of the tower block, adjusting for corner.
                }
            }
        }
        // Actually, let's just use simple loops like in buildTunnelLevel but adapted
        val castleLeft = margin
        val castleRight = cols - 1 - margin
        val castleTop = margin
        val castleBottom = rows - 1 - margin
        
        val towerCorners = listOf(
            castleLeft to castleTop,
            castleRight - towerSize + 1 to castleTop,
            castleLeft to castleBottom - towerSize + 1,
            castleRight - towerSize + 1 to castleBottom - towerSize + 1
        )
        for ((tx, ty) in towerCorners) {
            for (dx in 0 until towerSize) {
                for (dy in 0 until towerSize) {
                    val cx = tx + dx
                    val cy = ty + dy
                    if (cx in 0 until cols && cy in 0 until rows && !occupied.contains(cx to cy)) {
                        bricks.add(BrickSpec(cx, cy, BrickType.UNBREAKABLE, 999))
                        occupied.add(cx to cy)
                    }
                }
            }
        }

        // Walls
        // Top & Bottom
        for (col in (castleLeft + towerSize) until (castleRight - towerSize + 1)) {
            if (!occupied.contains(col to castleTop)) {
                val weak = random.nextFloat() < 0.15f
                val type = if (weak) BrickType.ARMORED else BrickType.UNBREAKABLE
                bricks.add(BrickSpec(col, castleTop, type, if (weak) (3 * difficulty).toInt() else 999))
                occupied.add(col to castleTop)
            }
            if (!occupied.contains(col to castleBottom)) {
                if (kotlin.math.abs(col - cols / 2) <= 1) {
                     // Gate
                     bricks.add(BrickSpec(col, castleBottom, BrickType.REINFORCED, (2 * difficulty).toInt()))
                     occupied.add(col to castleBottom)
                } else {
                    val weak = random.nextFloat() < 0.15f
                    val type = if (weak) BrickType.ARMORED else BrickType.UNBREAKABLE
                    bricks.add(BrickSpec(col, castleBottom, type, if (weak) (3 * difficulty).toInt() else 999))
                    occupied.add(col to castleBottom)
                }
            }
        }
        // Sides
        for (row in (castleTop + towerSize) until (castleBottom - towerSize + 1)) {
            if (!occupied.contains(castleLeft to row)) {
                val weak = random.nextFloat() < 0.15f
                val type = if (weak) BrickType.ARMORED else BrickType.UNBREAKABLE
                bricks.add(BrickSpec(castleLeft, row, type, if (weak) (3 * difficulty).toInt() else 999))
                occupied.add(castleLeft to row)
            }
             if (!occupied.contains(castleRight to row)) {
                val weak = random.nextFloat() < 0.15f
                val type = if (weak) BrickType.ARMORED else BrickType.UNBREAKABLE
                bricks.add(BrickSpec(castleRight, row, type, if (weak) (3 * difficulty).toInt() else 999))
                occupied.add(castleRight to row)
            }
        }

        // Interior
         val interiorDensity = 0.6f + (index * 0.01f).coerceAtMost(0.3f)
        for (row in (castleTop + 1) until castleBottom) {
            for (col in (castleLeft + 1) until castleRight) {
                if (occupied.contains(col to row)) continue
                if (random.nextFloat() < interiorDensity) {
                    val type = if (random.nextFloat() < 0.25f) BrickType.REINFORCED else BrickType.NORMAL
                    val baseHp = if (type == BrickType.REINFORCED) 2 else 1
                    val hp = (baseHp * difficulty).toInt().coerceAtLeast(1)
                    bricks.add(BrickSpec(col, row, type, hp))
                    occupied.add(col to row)
                }
            }
        }
    }

    private fun generateMazeLayout(rows: Int, cols: Int, index: Int, difficulty: Float,
                                 bricks: MutableList<BrickSpec>, occupied: MutableSet<Pair<Int, Int>>) {
        val random = kotlin.random.Random(index * 31)
        val mazeDensity = 0.35f + (index * 0.005f).coerceAtMost(0.25f)

        // Create maze-like corridors
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (occupied.contains(col to row)) continue

                // Create corridors with some randomness
                val inCorridor = (row % 3 == 0 || col % 4 == 0) ||
                                (row % 3 == 1 && random.nextFloat() < 0.3f) ||
                                (col % 4 == 1 && random.nextFloat() < 0.2f)

                if (inCorridor && random.nextFloat() < mazeDensity) {
                    val type = if (random.nextFloat() < 0.2f) BrickType.REINFORCED else BrickType.NORMAL
                    val baseHp = if (type == BrickType.REINFORCED) 2 else 1
                    val hp = (baseHp * difficulty).toInt().coerceAtLeast(1)
                    bricks.add(BrickSpec(col, row, type, hp))
                    occupied.add(col to row)
                }
            }
        }
    }

    private fun generateScatterLayout(rows: Int, cols: Int, index: Int, difficulty: Float,
                                    bricks: MutableList<BrickSpec>, occupied: MutableSet<Pair<Int, Int>>) {
        val random = kotlin.random.Random(index * 37)
        val scatterCount = 12 + (index / 3).coerceAtMost(8) // 12-20 scattered bricks
        val specialChance = 0.25f + (index * 0.005f).coerceAtMost(0.2f)

        repeat(scatterCount) {
            var attempts = 0
            while (attempts < 10) {
                val col = random.nextInt(cols)
                val row = random.nextInt(rows)
                if (!occupied.contains(col to row)) {
                    val type = when {
                        random.nextFloat() < specialChance -> BrickType.EXPLOSIVE
                        random.nextFloat() < 0.3f -> BrickType.REINFORCED
                        random.nextFloat() < 0.15f -> BrickType.ARMORED
                        else -> BrickType.NORMAL
                    }
                    val baseHp = when (type) {
                        BrickType.NORMAL -> 1
                        BrickType.REINFORCED -> 2
                        BrickType.ARMORED -> 3
                        BrickType.EXPLOSIVE -> 1
                        else -> 1
                    }
                    val hp = (baseHp * difficulty).toInt().coerceAtLeast(1)
                    bricks.add(BrickSpec(col, row, type, hp))
                    occupied.add(col to row)
                    break
                }
                attempts++
            }
        }
    }

    private fun generateMirrorLayout(rows: Int, cols: Int, index: Int, difficulty: Float,
                                   bricks: MutableList<BrickSpec>, occupied: MutableSet<Pair<Int, Int>>) {
        val random = kotlin.random.Random(index * 41)
        val mirrorType = random.nextInt(2) // 0=horizontal, 1=diagonal

        // Generate bricks on one side, mirror to the other
        val halfCols = cols / 2
        for (row in 0 until rows) {
            for (col in 0 until halfCols) {
                if (occupied.contains(col to row)) continue

                if (random.nextFloat() < 0.6f) {
                    val type = if (random.nextFloat() < 0.2f) BrickType.REINFORCED else BrickType.NORMAL
                    val baseHp = if (type == BrickType.REINFORCED) 2 else 1
                    val hp = (baseHp * difficulty).toInt().coerceAtLeast(1)

                    // Place on left side
                    bricks.add(BrickSpec(col, row, type, hp))
                    occupied.add(col to row)

                    // Mirror to right side
                    val mirrorCol = cols - 1 - col
                    if (mirrorCol != col && !occupied.contains(mirrorCol to row)) {
                        val mirrorRow = if (mirrorType == 1) rows - 1 - row else row // diagonal vs horizontal mirror
                        bricks.add(BrickSpec(mirrorCol, mirrorRow, type, hp))
                        occupied.add(mirrorCol to mirrorRow)
                    }
                }
            }
        }
    }

    private fun addSpecialBricks(rows: Int, cols: Int, index: Int, difficulty: Float,
                                bricks: MutableList<BrickSpec>, occupied: MutableSet<Pair<Int, Int>>) {
        val random = kotlin.random.Random(index * 29)

        // Add special bricks based on level progression
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (!occupied.contains(col to row)) continue

                // Add explosive bricks in clusters
                if (index > 3 && random.nextFloat() < 0.03f) {
                    val specIndex = bricks.indexOfFirst { it.col == col && it.row == row }
                    if (specIndex >= 0) {
                        bricks[specIndex] = bricks[specIndex].copy(type = BrickType.EXPLOSIVE)
                    }
                }

                // Add armored bricks in later levels
                if (index > 5 && row < 3 && random.nextFloat() < 0.08f) {
                    val specIndex = bricks.indexOfFirst { it.col == col && it.row == row }
                    if (specIndex >= 0 && bricks[specIndex].type == BrickType.NORMAL) {
                        bricks[specIndex] = bricks[specIndex].copy(type = BrickType.ARMORED, hitPoints = (3 * difficulty).toInt().coerceAtLeast(1))
                    }
                }
            }
        }
    }

    private fun fillRemainingSpaces(rows: Int, cols: Int, index: Int, difficulty: Float,
                                   bricks: MutableList<BrickSpec>, occupied: MutableSet<Pair<Int, Int>>,
                                   minimumBricks: Int) {
        val random = kotlin.random.Random(index * 37)
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (bricks.size >= minimumBricks) return
                if (occupied.contains(col to row)) continue

                val rowRatio = row.toFloat() / (rows - 1).coerceAtLeast(1)
                val fillChance = (0.7f - rowRatio * 0.22f).coerceIn(0.35f, 0.82f)
                if (random.nextFloat() > fillChance) continue

                val type = if (random.nextFloat() < 0.15f) BrickType.REINFORCED else BrickType.NORMAL
                val baseHp = if (type == BrickType.REINFORCED) 2 else 1
                val hp = (baseHp * difficulty).toInt().coerceAtLeast(1)
                bricks.add(BrickSpec(col, row, type, hp))
                occupied.add(col to row)
            }
        }
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
