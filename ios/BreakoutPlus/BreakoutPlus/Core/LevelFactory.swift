//
//  LevelFactory.swift
//  BreakoutPlus
//
//  Procedural + patterned level generation (ported from Android).
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
    private static let patterns: [LevelLayout] = [
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
        ),
        LevelLayout(
            rows: 6,
            cols: 12,
            lines: [
                "RRRRRRRRRRRR",
                "RAAARAAARAAAR",
                "RRRRRRRRRRRR",
                "NAAANAAANAAAN",
                "RRRRRRRRRRRR",
                "..U......U.."
            ],
            theme: .invaders,
            tip: "Invaders: dodge enemy fire and protect your shield."
        )
    ]

    static func buildLevel(index: Int, worldWidth: Float, worldHeight: Float, endless: Bool, difficulty: Float? = nil) -> (bricks: [Brick], theme: LevelTheme, tip: String) {
        if !endless {
            let layout = patterns[index % patterns.count]
            let diff = difficulty ?? difficultyFor(index: index)
            let bricks = buildFrom(layout: layout, worldWidth: worldWidth, worldHeight: worldHeight, difficulty: diff)
            return (bricks, layout.theme, layout.tip)
        }

        // Endless: procedural mix of types with scaling difficulty.
        let cols = 12
        let rows = 8 + min(4, index / 4)
        let theme = [LevelTheme.neon, .sunset, .cobalt, .aurora, .forest, .lava][index % 6]
        let diff = difficulty ?? difficultyFor(index: index)
        let brickWidth = worldWidth / Float(cols)
        let brickHeight: Float = 6.0
        let startY = worldHeight * 0.62

        var bricks: [Brick] = []
        bricks.reserveCapacity(rows * cols)
        for row in 0..<rows {
            for col in 0..<cols {
                if Double.random(in: 0..<1) < 0.12 { continue }
                let t: BrickType = randomBrickType(level: index, row: row)
                let hp = t == .unbreakable ? t.baseHitPoints : max(1, Int(Float(t.baseHitPoints) * diff))
                let x = Float(col) * brickWidth + brickWidth / 2
                let y = startY + Float(row) * brickHeight
                bricks.append(Brick(x: x, y: y, width: brickWidth - 1, height: brickHeight - 1, type: t, hitPoints: hp))
            }
        }
        return (bricks, theme, "Endless: adaptive layouts and escalating difficulty.")
    }

    private static func buildFrom(layout: LevelLayout, worldWidth: Float, worldHeight: Float, difficulty: Float) -> [Brick] {
        let brickWidth = worldWidth / Float(layout.cols)
        let brickHeight: Float = 6.0
        let startY = worldHeight * 0.62

        var bricks: [Brick] = []
        bricks.reserveCapacity(layout.rows * layout.cols)

        for (row, line) in layout.lines.enumerated() {
            for (col, ch) in line.enumerated() {
                let type = charToType(ch)
                if type == nil { continue }
                let t = type!
                let hp = t == .unbreakable ? t.baseHitPoints : max(1, Int(Float(t.baseHitPoints) * difficulty))
                let x = Float(col) * brickWidth + brickWidth / 2
                let y = startY + Float(row) * brickHeight
                bricks.append(Brick(x: x, y: y, width: brickWidth - 1, height: brickHeight - 1, type: t, hitPoints: hp))
            }
        }
        return bricks
    }

    private static func difficultyFor(index: Int) -> Float {
        // Rough match to Android scaling: slow ramp early, faster later.
        return min(3.0, 1.0 + Float(index) * 0.12)
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
        default: return nil
        }
    }

    private static func randomBrickType(level: Int, row: Int) -> BrickType {
        // Weighted selection that trends harder with level.
        let r = Double.random(in: 0..<1) + Double(level) * 0.015
        if r < 0.55 { return .normal }
        if r < 0.73 { return .reinforced }
        if r < 0.84 { return .armored }
        if r < 0.90 { return .explosive }
        if r < 0.94 { return .moving }
        if r < 0.97 { return .spawning }
        if r < 0.99 { return .phase }
        return row < 2 ? .unbreakable : .boss
    }
}
