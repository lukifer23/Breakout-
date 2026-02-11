//
//  ScoreboardView.swift
//  BreakoutPlus
//
//  Scoreboard screen
//

import SwiftUI

struct ScoreboardView: View {
    @EnvironmentObject var gameViewModel: GameViewModel
    @ObservedObject private var store = ScoreboardStore.shared
    @ObservedObject private var progression = ProgressionStore.shared
    @ObservedObject private var lifetime = LifetimeStatsStore.shared
    @State private var selectedMode: GameMode = .classic

    var body: some View {
        ZStack {
            Color(hex: "0F1419").edgesIgnoringSafeArea(.all)

            VStack(spacing: 16) {
                HStack {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Scoreboard")
                            .font(.system(size: 34, weight: .bold))
                            .foregroundColor(.white)
                        Text("\(selectedMode.displayName) - Top 10")
                            .foregroundColor(.white.opacity(0.6))
                            .font(.system(size: 14, weight: .medium))
                    }
                    Spacer()
                    Button("Menu") {
                        gameViewModel.exitToMenu()
                    }
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(Color(hex: "31E1F7"))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(Color(hex: "1A1F26"))
                    .cornerRadius(10)
                }

                // Mode selector
                HStack(spacing: 12) {
                    ForEach(GameMode.allCases) { mode in
                        ModeTab(mode: mode, isSelected: selectedMode == mode) {
                            selectedMode = mode
                        }
                    }
                }
                .padding(.horizontal, 4)

                let modeScores = store.getHighScoresForMode(selectedMode)

                if modeScores.isEmpty {
                    Spacer()
                    VStack(spacing: 10) {
                        Text("No \(selectedMode.displayName) scores yet.")
                            .foregroundColor(.white.opacity(0.9))
                            .font(.system(size: 18, weight: .semibold))
                        Text("Play a \(selectedMode.displayName) run to post your first score.")
                            .foregroundColor(.white.opacity(0.6))
                            .font(.system(size: 14))
                            .multilineTextAlignment(.center)
                    }
                    Spacer()
                } else {
                    ScrollView {
                        VStack(spacing: 12) {
                            ForEach(Array(modeScores.enumerated()), id: \.element.id) { idx, entry in
                                ScoreRow(rank: idx + 1, entry: entry)
                            }
                        }
                        .padding(.vertical, 6)
                    }

                    // Journey Progress
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Journey")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.white)

                        let chapter = progression.chapterForLevel(progression.bestLevel)
                        let stage = progression.stageForLevel(progression.bestLevel)
                        Text("Chapter \(chapter)-\(stage) • \(progression.totalXp) XP")
                            .foregroundColor(.white.opacity(0.8))
                            .font(.system(size: 14))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.vertical, 12)
                    .padding(.horizontal, 16)
                    .background(Color(hex: "1A1F26"))
                    .cornerRadius(12)

                    // Lifetime Stats
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Lifetime Stats")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.white)

                        VStack(alignment: .leading, spacing: 4) {
                            Text("\(lifetime.stats.totalBricksBroken) bricks broken")
                                .foregroundColor(.white.opacity(0.8))
                                .font(.system(size: 14))

                            Text("\(lifetime.stats.totalLivesLost) lives lost")
                                .foregroundColor(.white.opacity(0.8))
                                .font(.system(size: 14))

                            let hours = lifetime.stats.playTimeHours
                            let minutes = lifetime.stats.playTimeMinutes
                            Text("\(hours)h \(minutes)m play time")
                                .foregroundColor(.white.opacity(0.8))
                                .font(.system(size: 14))

                            Text("\(lifetime.stats.longestRunSeconds)s longest run")
                                .foregroundColor(.white.opacity(0.8))
                                .font(.system(size: 14))

                            Text("\(lifetime.stats.averageScore) avg score")
                                .foregroundColor(.white.opacity(0.8))
                                .font(.system(size: 14))
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.vertical, 12)
                    .padding(.horizontal, 16)
                    .background(Color(hex: "1A1F26"))
                    .cornerRadius(12)

                    HStack {
                        Button("Reset All Data") {
                            store.reset()
                            progression.reset()
                            lifetime.reset()
                        }
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 18)
                        .padding(.vertical, 12)
                        .background(Color(hex: "2A323D"))
                        .cornerRadius(12)

                        Spacer()
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 60)
            .padding(.bottom, 24)
        }
    }
}

private struct ScoreRow: View {
    let rank: Int
    let entry: ScoreEntry

    var body: some View {
        HStack(spacing: 12) {
            Text("#\(rank)")
                .foregroundColor(rankColor)
                .font(.system(size: 16, weight: .bold))
                .frame(width: 44, alignment: .leading)

            VStack(alignment: .leading, spacing: 4) {
                Text(entry.name)
                    .foregroundColor(.white)
                    .font(.system(size: 16, weight: .semibold))
                Text("\(entry.mode.displayName) • Level \(entry.level) • \(formatDuration(entry.durationSeconds))")
                    .foregroundColor(.white.opacity(0.6))
                    .font(.system(size: 13))
            }

            Spacer()

            Text("\(entry.score)")
                .foregroundColor(.white)
                .font(.system(size: 18, weight: .bold))
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color(hex: "1A1F26"))
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .stroke(Color(hex: "31E1F7").opacity(0.16), lineWidth: 1)
        )
        .cornerRadius(14)
    }

    private func formatDuration(_ seconds: Int) -> String {
        if seconds <= 0 { return "--" }
        let m = seconds / 60
        let s = seconds % 60
        return String(format: "%02d:%02d", m, s)
    }

    private var rankColor: Color {
        switch rank {
        case 1: return Color(hex: "FFC857")
        case 2: return Color(hex: "31E1F7")
        case 3: return Color(hex: "FF4FD8")
        default: return Color(hex: "9AA4B2")
        }
    }
}

private struct ModeTab: View {
    let mode: GameMode
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(mode.displayName)
                .font(.system(size: 14, weight: isSelected ? .semibold : .medium))
                .foregroundColor(isSelected ? .white : .white.opacity(0.6))
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(isSelected ? Color(hex: "31E1F7").opacity(0.2) : Color.clear)
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(isSelected ? Color(hex: "31E1F7") : Color.white.opacity(0.2), lineWidth: 1)
                        )
                )
        }
    }
}

#Preview {
    ScoreboardView()
        .environmentObject(GameViewModel())
}
