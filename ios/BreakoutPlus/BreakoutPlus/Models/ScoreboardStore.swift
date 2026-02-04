//
//  ScoreboardStore.swift
//  BreakoutPlus
//
//  Stub implementation for scoreboard functionality
//

import Foundation

struct ScoreEntry: Identifiable {
    let id = UUID()
    let mode: GameMode
    let level: Int
    let score: Int
    let durationSeconds: Int
}

class ScoreboardStore: ObservableObject {
    static let shared = ScoreboardStore()

    @Published var entries: [ScoreEntry] = []

    private init() {
        // Add some sample data for testing
        entries = [
            ScoreEntry(mode: .classic, level: 5, score: 1250, durationSeconds: 180),
            ScoreEntry(mode: .timed, level: 3, score: 980, durationSeconds: 120),
            ScoreEntry(mode: .endless, level: 8, score: 2100, durationSeconds: 300)
        ]
    }

    func reset() {
        entries.removeAll()
        // Add back sample data
        entries = [
            ScoreEntry(mode: .classic, level: 1, score: 100, durationSeconds: 30)
        ]
        objectWillChange.send()
    }
}