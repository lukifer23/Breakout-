//
//  LifetimeStatsStore.swift
//  BreakoutPlus
//
//  Persistent storage for lifetime statistics
//  Equivalent to Android LifetimeStatsManager
//

import Foundation

struct LifetimeStats {
    let totalBricksBroken: Int
    let totalLivesLost: Int
    let totalPlaySeconds: TimeInterval
    let longestRunSeconds: Int
    let gamesPlayed: Int
    let highestScore: Int
    let totalScore: Int

    var averageScore: Int {
        return gamesPlayed > 0 ? totalScore / gamesPlayed : 0
    }

    var playTimeHours: Int {
        return Int(totalPlaySeconds / 3600)
    }

    var playTimeMinutes: Int {
        return Int((totalPlaySeconds / 60).truncatingRemainder(dividingBy: 60))
    }
}

final class LifetimeStatsStore: ObservableObject {
    static let shared = LifetimeStatsStore()

    @Published private(set) var stats: LifetimeStats!

    private let bricksKey = "breakoutplus.lifetime.bricks_broken"
    private let livesKey = "breakoutplus.lifetime.lives_lost"
    private let secondsKey = "breakoutplus.lifetime.play_seconds"
    private let longestKey = "breakoutplus.lifetime.longest_run"
    private let gamesKey = "breakoutplus.lifetime.games_played"
    private let highestKey = "breakoutplus.lifetime.highest_score"
    private let totalScoreKey = "breakoutplus.lifetime.total_score"

    private init() {
        self.stats = load()
    }

    func recordRun(bricksBroken: Int, livesLost: Int, durationSeconds: Int, score: Int) {
        let current = stats!

        let newStats = LifetimeStats(
            totalBricksBroken: current.totalBricksBroken + bricksBroken,
            totalLivesLost: current.totalLivesLost + livesLost,
            totalPlaySeconds: current.totalPlaySeconds + Double(durationSeconds),
            longestRunSeconds: max(current.longestRunSeconds, durationSeconds),
            gamesPlayed: current.gamesPlayed + 1,
            highestScore: max(current.highestScore, score),
            totalScore: current.totalScore + score
        )

        stats = newStats
        save(newStats)
    }

    private func load() -> LifetimeStats {
        let defaults = UserDefaults.standard
        return LifetimeStats(
            totalBricksBroken: max(0, defaults.integer(forKey: bricksKey)),
            totalLivesLost: max(0, defaults.integer(forKey: livesKey)),
            totalPlaySeconds: max(0, defaults.double(forKey: secondsKey)),
            longestRunSeconds: max(0, defaults.integer(forKey: longestKey)),
            gamesPlayed: max(0, defaults.integer(forKey: gamesKey)),
            highestScore: max(0, defaults.integer(forKey: highestKey)),
            totalScore: max(0, defaults.integer(forKey: totalScoreKey))
        )
    }

    private func save(_ stats: LifetimeStats) {
        let defaults = UserDefaults.standard
        defaults.set(stats.totalBricksBroken, forKey: bricksKey)
        defaults.set(stats.totalLivesLost, forKey: livesKey)
        defaults.set(stats.totalPlaySeconds, forKey: secondsKey)
        defaults.set(stats.longestRunSeconds, forKey: longestKey)
        defaults.set(stats.gamesPlayed, forKey: gamesKey)
        defaults.set(stats.highestScore, forKey: highestKey)
        defaults.set(stats.totalScore, forKey: totalScoreKey)
    }

    func reset() {
        let emptyStats = LifetimeStats(
            totalBricksBroken: 0,
            totalLivesLost: 0,
            totalPlaySeconds: 0,
            longestRunSeconds: 0,
            gamesPlayed: 0,
            highestScore: 0,
            totalScore: 0
        )
        stats = emptyStats
        save(emptyStats)
    }
}