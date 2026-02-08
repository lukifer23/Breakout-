//
//  ScoreboardStore.swift
//  BreakoutPlus
//
//  Persistent local scoreboard (UserDefaults + Codable).
//

import Foundation

struct ScoreEntry: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    let score: Int
    let mode: GameMode
    let level: Int
    let durationSeconds: Int
    let timestamp: Date
}

final class ScoreboardStore: ObservableObject {
    static let shared = ScoreboardStore()

    @Published private(set) var entries: [ScoreEntry] = []

    private let key = "breakoutplus.scoreboard.v1"
    private let maxEntriesPerMode = 10

    private init() {
        entries = load()
    }

    func add(score: Int, mode: GameMode, level: Int, durationSeconds: Int) {
        let entry = ScoreEntry(
            score: score,
            mode: mode,
            level: level,
            durationSeconds: durationSeconds,
            timestamp: Date()
        )
        var next = entries
        next.append(entry)

        // Keep only top scores per mode
        next = filterTopScoresPerMode(next)
        entries = next
        save(entries)
    }

    func getHighScoresForMode(_ mode: GameMode) -> [ScoreEntry] {
        return entries
            .filter { $0.mode == mode }
            .sorted {
                if $0.score != $1.score { return $0.score > $1.score }
                return $0.durationSeconds < $1.durationSeconds
            }
            .prefix(maxEntriesPerMode)
            .map { $0 }
    }

    func reset() {
        entries = []
        save(entries)
    }

    private func filterTopScoresPerMode(_ allEntries: [ScoreEntry]) -> [ScoreEntry] {
        var result: [ScoreEntry] = []
        let modes = Set(allEntries.map { $0.mode })

        for mode in modes {
            let modeEntries = allEntries.filter { $0.mode == mode }
                .sorted {
                    if $0.score != $1.score { return $0.score > $1.score }
                    return $0.durationSeconds < $1.durationSeconds
                }
                .prefix(maxEntriesPerMode)
            result.append(contentsOf: modeEntries)
        }

        return result
    }

    private func load() -> [ScoreEntry] {
        guard let data = UserDefaults.standard.data(forKey: key) else { return [] }
        return (try? JSONDecoder().decode([ScoreEntry].self, from: data)) ?? []
    }

    private func save(_ entries: [ScoreEntry]) {
        guard let data = try? JSONEncoder().encode(entries) else { return }
        UserDefaults.standard.set(data, forKey: key)
    }
}

