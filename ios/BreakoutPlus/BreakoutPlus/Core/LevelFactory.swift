//
//  LevelFactory.swift
//  BreakoutPlus
//
//  Patterned + procedural generation with mode-specific routing.
//

import Foundation

struct LevelLayout {
    let rows: Int
    let cols: Int
    let lines: [String]
    let theme: LevelTheme
    let tip: String
}

enum LevelFactory {
    private static let basePatterns: [LevelLayout] = [
        LevelLayout(
            rows: 6,
            cols: 10,
            lines: [
                "NNNNNNNNNN",
                "NRRNNRRNNR",
                "NNENNENNNN",
                "NRRNNRRNNR",
                "NNNNNNNNNN",
                "..U....U.."
            ],
            theme: .neon,
            tip: "Aim for explosive bricks to clear clusters."
        ),
        LevelLayout(
            rows: 7,
            cols: 10,
            lines: [
                "..NNNNNN..",
                ".NRRAARRN.",
                ".NNEEEENN.",
                "NRRNNNNRRN",
                "NNA....ANN",
                ".NRR..RRN.",
                "..NNNNNN.."
            ],
            theme: .sunset,
            tip: "Laser powerups help cut through armor."
        ),
        LevelLayout(
            rows: 7,
            cols: 11,
            lines: [
                "NNNNNNNNNNN",
                "NRRRNNNRRRN",
                "NNNEEENNENN",
                "NNAANNNNAAN",
                "NNNNNNNNNNN",
                "..U.....U..",
                "..U.....U.."
            ],
            theme: .cobalt,
            tip: "Stack guardrail with shield for safe rallies."
        ),
        LevelLayout(
            rows: 8,
            cols: 12,
            lines: [
                "NNNNNNNNNNNN",
                "NRRNNRRNNRRN",
                "NNAAANNNAAAN",
                "NNEEENNEEENN",
                "NNAAANNNAAAN",
                "NRRNNRRNNRRN",
                "NNNNNNNNNNNN",
                "..U..U..U..."
            ],
            theme: .aurora,
            tip: "Explosives chain through reinforced rows."
        ),
        LevelLayout(
            rows: 9,
            cols: 12,
            lines: [
                "NNNNNNNNNNNN",
                "NRRMMRRMMRRN",
                "NNS..SS..SSN",
                "NNAAANNNAAAN",
                "NNEEENNNEEEN",
                "NNNNNNNNNNNN",
                "..U..U..U...",
                "..M....M....",
                "..P..P..P..."
            ],
            theme: .forest,
            tip: "Moving + spawning bricks change the tempo."
        ),
        LevelLayout(
            rows: 9,
            cols: 12,
            lines: [
                "NNPPNNPPNNPP",
                "NRRAAARRAARR",
                "NNSNNSSNNSSN",
                "NNNNNNNNNNNN",
                "NNEEENNNEEEN",
                "..U..U..U...",
                "..B....B....",
                "..P..P..P...",
                "NNNNNNNNNNNN"
            ],
            theme: .lava,
            tip: "Boss bricks reward big plays. Save lasers."
        )
    ]

    private static let invaderPattern = LevelLayout(
        rows: 6,
        cols: 12,
        lines: [
            "IIIIIIIIIIII",
            "IAAAIAAAIAAA",
            "IIIIIIIIIIII",
            "IRRIIIRRIIIR",
            "IIIIIIIIIIII",
            "..U......U.."
        ],
        theme: .invaders,
        tip: "Invaders: dodge enemy fire and protect your shield."
    )

    static func buildLevel(
        index: Int,
        worldWidth: Float,
        worldHeight: Float,
        mode: GameMode,
        endless: Bool,
        difficulty: Float? = nil
    ) -> (bricks: [Brick], theme: LevelTheme, tip: String) {
        let diff = difficulty ?? difficultyFor(index: index)
        let modeTheme = themeFor(mode: mode, index: index)

        if mode == .tunnel {
            return buildTunnelLevel(
                index: index,
                worldWidth: worldWidth,
                worldHeight: worldHeight,
                difficulty: diff,
                theme: modeTheme
            )
        }

        if mode == .invaders {
            let bricks = buildFrom(
                layout: invaderPattern,
                worldWidth: worldWidth,
                worldHeight: worldHeight,
                difficulty: max(1.0, diff * 0.96)
            )
            return (bricks, .invaders, invaderPattern.tip)
        }

        if !endless {
            let layout = basePatterns[index % basePatterns.count]
            let bricks = buildFrom(layout: layout, worldWidth: worldWidth, worldHeight: worldHeight, difficulty: diff)
            return (bricks, modeTheme, layout.tip)
        }

        // Endless: procedural mix of types with scaling difficulty.
        let cols = 12 + min(2, index / 5)
        let rows = 8 + min(4, index / 4)
        let brickWidth = worldWidth / Float(cols)
        let brickHeight: Float = 6.0
        let startY = worldHeight * 0.62

        var bricks: [Brick] = []
        bricks.reserveCapacity(rows * cols)
        for row in 0..<rows {
            for col in 0..<cols {
                if hashUnit(index * 719 + row * 29 + col * 11) < 0.12 { continue }
                let t: BrickType = randomBrickType(level: index, row: row)
                let hp = t == .unbreakable ? t.baseHitPoints : max(1, Int(Float(t.baseHitPoints) * diff))
                let x = Float(col) * brickWidth + brickWidth / 2
                let y = startY + Float(row) * brickHeight
                bricks.append(Brick(x: x, y: y, width: brickWidth - 1, height: brickHeight - 1, type: t, hitPoints: hp))
            }
        }
        return (bricks, modeTheme, "Endless: adaptive layouts and escalating difficulty.")
    }

    private static func buildFrom(layout: LevelLayout, worldWidth: Float, worldHeight: Float, difficulty: Float) -> [Brick] {
        let brickWidth = worldWidth / Float(layout.cols)
        let brickHeight: Float = 6.0
        let startY = worldHeight * 0.62

        var bricks: [Brick] = []
        bricks.reserveCapacity(layout.rows * layout.cols)

        for (row, line) in layout.lines.enumerated() {
            for (col, ch) in line.enumerated() {
                guard let t = charToType(ch) else { continue }
                let hp = t == .unbreakable ? t.baseHitPoints : max(1, Int(Float(t.baseHitPoints) * difficulty))
                let x = Float(col) * brickWidth + brickWidth / 2
                let y = startY + Float(row) * brickHeight
                bricks.append(Brick(x: x, y: y, width: brickWidth - 1, height: brickHeight - 1, type: t, hitPoints: hp))
            }
        }
        return bricks
    }

    private static func buildTunnelLevel(
        index: Int,
        worldWidth: Float,
        worldHeight: Float,
        difficulty: Float,
        theme: LevelTheme
    ) -> (bricks: [Brick], theme: LevelTheme, tip: String) {
        let rows = max(12, min(15, 12 + index / 5))
        let cols = max(13, min(15, 13 + index / 6))
        let fortressLeft = 1
        let fortressRight = cols - 2
        let fortressTop = 1
        let fortressBottom = rows - 4
        let wallThickness = index >= 8 ? 2 : 1
        let requestedGateWidth: Int
        if index >= 12 {
            requestedGateWidth = 3
        } else if index >= 6 {
            requestedGateWidth = 2
        } else {
            requestedGateWidth = 1
        }
        let gateWidth = min(3, max(wallThickness, requestedGateWidth))
        let gateCenter = max(
            fortressLeft + wallThickness + 1,
            min(fortressRight - wallThickness - 1, cols / 2 + (index % 3 - 1))
        )
        let gateStart = max(
            fortressLeft + wallThickness,
            min(fortressRight - wallThickness - gateWidth + 1, gateCenter - gateWidth / 2)
        )
        let gateEnd = gateStart + gateWidth - 1
        let tunnelLeftWall = max(0, gateStart - 1)
        let tunnelRightWall = min(cols - 1, gateEnd + 1)
        let levelScale: Float = 1.0 + Float(index) * 0.055

        let areaTop = worldHeight * 0.84
        let areaBottom = worldHeight * 0.22
        let cellHeight = (areaTop - areaBottom) / Float(rows)
        let cellWidth = worldWidth / Float(cols)
        let brickHeight = max(2.2, cellHeight - 0.8)
        let brickWidth = max(2.4, cellWidth - 0.8)

        var bricks: [Brick] = []
        bricks.reserveCapacity(rows * cols)
        var occupied = Set<Int64>()

        func key(_ col: Int, _ row: Int) -> Int64 {
            (Int64(col) << 32) | Int64(row & 0xffffffff)
        }

        func addGridBrick(_ col: Int, _ row: Int, _ type: BrickType, _ hp: Int) {
            if col < 0 || col >= cols || row < 0 || row >= rows { return }
            let k = key(col, row)
            if occupied.contains(k) { return }
            occupied.insert(k)
            let x = Float(col) * cellWidth + cellWidth / 2
            let y = areaTop - (Float(row) + 0.5) * cellHeight
            bricks.append(Brick(x: x, y: y, width: brickWidth, height: brickHeight, type: type, hitPoints: hp))
        }

        // Fortress shell: unbreakable ring with a narrow gate.
        for layer in 0..<wallThickness {
            let left = fortressLeft + layer
            let right = fortressRight - layer
            let top = fortressTop + layer
            let bottom = fortressBottom - layer
            if left >= right || top >= bottom { continue }
            let gateLayerStart = min(gateEnd, gateStart + layer)
            let gateLayerEnd = max(gateStart, gateEnd - layer)
            for row in top...bottom {
                for col in left...right {
                    let onEdge = row == top || row == bottom || col == left || col == right
                    let isGate = row == bottom && col >= gateLayerStart && col <= gateLayerEnd
                    if onEdge && !isGate {
                        addGridBrick(col, row, .unbreakable, 999)
                    }
                }
            }
        }

        // Entry tunnel walls from gate down toward the launch zone.
        if fortressBottom + 1 < rows {
            for row in (fortressBottom + 1)..<rows {
                addGridBrick(tunnelLeftWall, row, .unbreakable, 999)
                addGridBrick(tunnelRightWall, row, .unbreakable, 999)
            }
        }

        // Interior pressure (breakable core).
        let coreTop = fortressTop + wallThickness
        let coreBottomExclusive = fortressBottom - wallThickness + 1
        let coreLeft = fortressLeft + wallThickness
        let coreRightExclusive = fortressRight - wallThickness + 1
        var interiorCount = 0

        if coreTop < coreBottomExclusive && coreLeft < coreRightExclusive {
            for row in coreTop..<coreBottomExclusive {
                for col in coreLeft..<coreRightExclusive {
                    if col >= gateStart && col <= gateEnd && row >= fortressBottom - 2 { continue }
                    let inGateLane = col >= gateStart && col <= gateEnd
                    let distanceFromGate = abs(col - gateCenter)
                    let nearCoreCenter = abs(col - cols / 2) <= 2
                    let densityBase: Float = inGateLane ? 0.74 : 0.95
                    let lanePenalty: Float = distanceFromGate <= 1 ? 0.03 : 0
                    let centerBonus: Float = nearCoreCenter ? 0.02 : 0
                    let density = min(0.98, densityBase + Float(index) * 0.006 - lanePenalty + centerBonus)
                    let keep = Float(hashUnit(index * 131 + row * 41 + col * 17))
                    if keep > min(0.96, density) { continue }

                    let typeRoll = hashUnit(index * 173 + row * 29 + col * 19)
                    let type: BrickType
                    if typeRoll < 0.08 {
                        type = .explosive
                    } else if typeRoll < 0.33 {
                        type = .reinforced
                    } else if typeRoll < 0.47 {
                        type = .armored
                    } else {
                        type = .normal
                    }
                    let hp = max(1, Int(round(Float(baseHitPoints(type)) * levelScale * difficulty)))
                    addGridBrick(col, row, type, hp)
                    interiorCount += 1
                }
            }
        }

        // Guarantee interior never looks sparse.
        let interiorWidth = max(1, coreRightExclusive - coreLeft)
        let interiorHeight = max(1, coreBottomExclusive - coreTop)
        let minInterior = max(26, Int(round(Float(interiorWidth * interiorHeight) * 0.72)))
        if interiorCount < minInterior && coreTop < coreBottomExclusive && coreLeft < coreRightExclusive {
            for row in coreTop..<coreBottomExclusive {
                for col in coreLeft..<coreRightExclusive {
                    if interiorCount >= minInterior { break }
                    if occupied.contains(key(col, row)) { continue }
                    if col >= gateStart && col <= gateEnd && row >= fortressBottom - 2 { continue }
                    let typeRoll = hashUnit(index * 211 + row * 31 + col * 23)
                    let type: BrickType
                    if typeRoll < 0.12 {
                        type = .explosive
                    } else if typeRoll < 0.38 {
                        type = .reinforced
                    } else if typeRoll < 0.54 {
                        type = .armored
                    } else {
                        type = .normal
                    }
                    let hp = max(1, Int(round(Float(baseHitPoints(type)) * levelScale * difficulty)))
                    addGridBrick(col, row, type, hp)
                    interiorCount += 1
                }
            }
        }

        return (
            bricks: bricks,
            theme: theme,
            tip: "Tunnel Siege: only the gate is open. Thread shots through the tunnel."
        )
    }

    private static func difficultyFor(index: Int) -> Float {
        min(3.0, 1.0 + Float(index) * 0.12)
    }

    private static func themeFor(mode: GameMode, index: Int) -> LevelTheme {
        if mode == .invaders { return .invaders }
        let rotation: [LevelTheme]
        switch mode {
        case .classic:
            rotation = [.neon, .cobalt, .aurora]
        case .timed:
            rotation = [.sunset, .lava, .aurora]
        case .endless:
            rotation = [.neon, .sunset, .cobalt, .aurora, .forest, .lava]
        case .god:
            rotation = [.aurora, .forest, .cobalt]
        case .rush:
            rotation = [.lava, .sunset, .forest]
        case .volley:
            rotation = [.cobalt, .lava, .neon, .aurora]
        case .tunnel:
            rotation = [.cobalt, .forest, .sunset, .lava]
        case .survival:
            rotation = [.forest, .cobalt, .lava]
        case .invaders:
            rotation = [.invaders]
        }
        return rotation[index % max(1, rotation.count)]
    }

    private static func baseHitPoints(_ type: BrickType) -> Int {
        switch type {
        case .normal: return 1
        case .reinforced: return 2
        case .armored: return 3
        case .explosive: return 1
        case .unbreakable: return 999
        case .moving: return 2
        case .spawning: return 2
        case .phase: return 3
        case .boss: return 6
        case .invader: return 1
        }
    }

    private static func charToType(_ ch: Character) -> BrickType? {
        switch ch {
        case "N": return .normal
        case "R": return .reinforced
        case "A": return .armored
        case "E": return .explosive
        case "U": return .unbreakable
        case "M": return .moving
        case "S": return .spawning
        case "P": return .phase
        case "B": return .boss
        case "I": return .invader
        default: return nil
        }
    }

    private static func randomBrickType(level: Int, row: Int) -> BrickType {
        let r = hashUnit(level * 89 + row * 17) + Double(level) * 0.015
        if r < 0.55 { return .normal }
        if r < 0.73 { return .reinforced }
        if r < 0.84 { return .armored }
        if r < 0.90 { return .explosive }
        if r < 0.94 { return .moving }
        if r < 0.97 { return .spawning }
        if r < 0.99 { return .phase }
        return row < 2 ? .unbreakable : .boss
    }

    private static func hashUnit(_ seed: Int) -> Double {
        let x = Foundation.sin(Double(seed) * 12.9898 + 78.233) * 43758.5453
        return x - floor(x)
    }
}
