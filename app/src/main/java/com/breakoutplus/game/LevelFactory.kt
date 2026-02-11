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
                val density = (0.78f + intensity * 0.09f - rowRatio * 0.08f).coerceIn(0.64f, 0.88f)
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

    fun buildTunnelLevel(index: Int, difficulty: Float, theme: LevelTheme): LevelLayout {
        val rows = (12 + (index / 5).coerceAtMost(3)).coerceIn(12, 15)
        val cols = (13 + (index / 6).coerceAtMost(2)).coerceIn(13, 15)
        val fortressLeft = 1
        val fortressRight = cols - 2
        val fortressTop = 1
        val fortressBottom = rows - 4
        val wallThickness = if (index >= 8) 2 else 1
        val requestedGateWidth = when {
            index >= 12 -> 3
            index >= 6 -> 2
            else -> 1
        }
        // Keep gate open through every wall layer; thickness must never exceed gate width.
        val gateWidth = requestedGateWidth.coerceAtLeast(wallThickness).coerceAtMost(3)
        val gateCenter = (cols / 2 + (index % 3 - 1))
            .coerceIn(fortressLeft + wallThickness + 1, fortressRight - wallThickness - 1)
        val gateStart = (gateCenter - gateWidth / 2)
            .coerceIn(fortressLeft + wallThickness, fortressRight - wallThickness - gateWidth + 1)
        val gateEnd = gateStart + gateWidth - 1
        val tunnelLeftWall = (gateStart - 1).coerceAtLeast(0)
        val tunnelRightWall = (gateEnd + 1).coerceAtMost(cols - 1)
        val levelScale = 1f + index * 0.055f
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

        // Fortress shell: only unbreakable wall bricks with one gate opening.
        repeat(wallThickness) { layer ->
            val left = fortressLeft + layer
            val right = fortressRight - layer
            val top = fortressTop + layer
            val bottom = fortressBottom - layer
            if (left >= right || top >= bottom) return@repeat
            val gateLayerStart = (gateStart + layer).coerceAtMost(gateEnd)
            val gateLayerEnd = (gateEnd - layer).coerceAtLeast(gateStart)
            for (row in top..bottom) {
                for (col in left..right) {
                    val onFortressEdge = row == top || row == bottom || col == left || col == right
                    val isGate = row == bottom && col in gateLayerStart..gateLayerEnd
                    if (onFortressEdge && !isGate) {
                        addBrick(col, row, BrickType.UNBREAKABLE, 999)
                    }
                }
            }
        }

        // Entry tunnel walls from gate down to the launch zone.
        for (row in (fortressBottom + 1) until rows) {
            addBrick(tunnelLeftWall, row, BrickType.UNBREAKABLE, 999)
            addBrick(tunnelRightWall, row, BrickType.UNBREAKABLE, 999)
        }

        // Fill fortress interior with breakable bricks only.
        val coreTop = fortressTop + wallThickness
        val coreBottomExclusive = fortressBottom - wallThickness + 1
        val coreLeft = fortressLeft + wallThickness
        val coreRightExclusive = fortressRight - wallThickness + 1
        var interiorCount = 0
        for (row in coreTop until coreBottomExclusive) {
            for (col in coreLeft until coreRightExclusive) {
                // Keep a narrow open channel near the gate to preserve the "small entry point" identity.
                if (col in gateStart..gateEnd && row >= fortressBottom - 2) continue
                val seed = index * 131 + row * 41 + col * 17
                val inGateLane = col in gateStart..gateEnd
                val distanceFromGate = kotlin.math.abs(col - gateCenter)
                val nearCoreCenter = kotlin.math.abs(col - cols / 2) <= 2
                val densityBase = if (inGateLane) 0.74f else 0.95f
                val lanePenalty = if (distanceFromGate <= 1) 0.03f else 0f
                val centerBonus = if (nearCoreCenter) 0.02f else 0f
                val density = (densityBase + index * 0.006f - lanePenalty + centerBonus).coerceAtMost(0.98f)
                if (kotlin.random.Random(seed).nextFloat() > density.coerceAtMost(0.96f)) continue
                val typeRoll = kotlin.random.Random(seed + 19).nextFloat()
                val type = when {
                    typeRoll < 0.08f -> BrickType.EXPLOSIVE
                    typeRoll < 0.33f -> BrickType.REINFORCED
                    typeRoll < 0.47f -> BrickType.ARMORED
                    else -> BrickType.NORMAL
                }
                val hp = max(1, (baseHp(type) * levelScale * difficulty).roundToInt())
                addBrick(col, row, type, hp)
                interiorCount += 1
            }
        }

        // Guarantee interior pressure so Tunnel never reads as a sparse normal mode.
        val interiorWidth = (coreRightExclusive - coreLeft).coerceAtLeast(1)
        val interiorHeight = (coreBottomExclusive - coreTop).coerceAtLeast(1)
        val minimumInteriorBricks = (interiorWidth * interiorHeight * 0.72f).roundToInt().coerceAtLeast(26)
        if (interiorCount < minimumInteriorBricks) {
            for (row in coreTop until coreBottomExclusive) {
                for (col in coreLeft until coreRightExclusive) {
                    if (interiorCount >= minimumInteriorBricks) break
                    if ((col to row) in occupied) continue
                    if (col in gateStart..gateEnd && row >= fortressBottom - 2) continue
                    val seed = index * 211 + row * 31 + col * 23
                    val typeRoll = kotlin.random.Random(seed).nextFloat()
                    val type = when {
                        typeRoll < 0.12f -> BrickType.EXPLOSIVE
                        typeRoll < 0.38f -> BrickType.REINFORCED
                        typeRoll < 0.54f -> BrickType.ARMORED
                        else -> BrickType.NORMAL
                    }
                    val hp = max(1, (baseHp(type) * levelScale * difficulty).roundToInt())
                    addBrick(col, row, type, hp)
                    interiorCount += 1
                }
            }
        }

        return LevelLayout(
            rows = rows,
            cols = cols,
            bricks = bricks,
            theme = theme,
            tip = "Tunnel Siege: only the gate is open. Thread shots through the tunnel."
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
        val templateType = (index + kotlin.random.Random(index).nextInt(3)) % 4
        when (templateType) {
            0 -> generateSymmetricLayout(rows, cols, index, difficulty, bricks, occupied)
            1 -> generateClusterLayout(rows, cols, index, difficulty, bricks, occupied)
            2 -> generateWaveLayout(rows, cols, index, difficulty, bricks, occupied)
            3 -> generateFortressLayout(rows, cols, index, difficulty, bricks, occupied)
        }

        // Add special brick types based on difficulty and level
        addSpecialBricks(rows, cols, index, difficulty, bricks, occupied)

        val minimumBricks = (rows * cols * 0.48f).roundToInt()
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
        val wallThickness = 1 + random.nextInt(2) // 1-2 brick walls

        // Create outer walls
        for (thickness in 0 until wallThickness) {
            // Top and bottom walls
            for (col in thickness until cols - thickness) {
                if (!occupied.contains(col to thickness)) {
                    bricks.add(BrickSpec(col, thickness, BrickType.UNBREAKABLE, 999))
                    occupied.add(col to thickness)
                }
                if (!occupied.contains(col to (rows - 1 - thickness))) {
                    bricks.add(BrickSpec(col, rows - 1 - thickness, BrickType.UNBREAKABLE, 999))
                    occupied.add(col to (rows - 1 - thickness))
                }
            }
            // Left and right walls
            for (row in thickness until rows - thickness) {
                if (!occupied.contains(thickness to row)) {
                    bricks.add(BrickSpec(thickness, row, BrickType.UNBREAKABLE, 999))
                    occupied.add(thickness to row)
                }
                if (!occupied.contains((cols - 1 - thickness) to row)) {
                    bricks.add(BrickSpec(cols - 1 - thickness, row, BrickType.UNBREAKABLE, 999))
                    occupied.add((cols - 1 - thickness) to row)
                }
            }
        }

        // Fill interior with random bricks
        val interiorDensity = 0.5f + (index * 0.01f).coerceAtMost(0.3f)
        for (row in wallThickness until rows - wallThickness) {
            for (col in wallThickness until cols - wallThickness) {
                if (random.nextFloat() < interiorDensity && !occupied.contains(col to row)) {
                    val type = if (random.nextFloat() < 0.25f) BrickType.REINFORCED else BrickType.NORMAL
                    val baseHp = if (type == BrickType.REINFORCED) 2 else 1
                    val hp = (baseHp * difficulty).toInt().coerceAtLeast(1)
                    bricks.add(BrickSpec(col, row, type, hp))
                    occupied.add(col to row)
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
