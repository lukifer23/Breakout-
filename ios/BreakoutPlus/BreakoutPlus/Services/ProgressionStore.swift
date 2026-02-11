//
//  ProgressionStore.swift
//  BreakoutPlus
//
//  Persistent storage for player progression (XP, levels, chapters)
//  Equivalent to Android ProgressionManager
//

import Foundation

final class ProgressionStore: ObservableObject {
    static let shared = ProgressionStore()

    @Published private(set) var totalXp: Int = 0
    @Published private(set) var bestLevel: Int = 1

    private let xpKey = "breakoutplus.progression.xp"
    private let levelKey = "breakoutplus.progression.best_level"

    private init() {
        load()
    }

    func addXp(_ amount: Int) {
        let safeAmount = max(0, amount)
        totalXp += safeAmount
        save()
    }

    func updateBestLevel(_ level: Int) {
        let safeLevel = max(1, level)
        if safeLevel > bestLevel {
            bestLevel = safeLevel
            save()
        }
    }

    func chapterForLevel(_ level: Int) -> Int {
        return ((max(1, level) - 1) / 10) + 1
    }

    func stageForLevel(_ level: Int) -> Int {
        return ((max(1, level) - 1) % 10) + 1
    }

    func xpForLevel(_ level: Int) -> Int {
        let base = 10
        let bonus = (max(1, level) - 1) * 2
        return base + bonus
    }

    private func load() {
        totalXp = UserDefaults.standard.integer(forKey: xpKey)
        bestLevel = UserDefaults.standard.integer(forKey: levelKey)
        if bestLevel < 1 {
            bestLevel = 1
        }
    }

    private func save() {
        UserDefaults.standard.set(totalXp, forKey: xpKey)
        UserDefaults.standard.set(bestLevel, forKey: levelKey)
    }

    func reset() {
        totalXp = 0
        bestLevel = 1
        save()
    }
}