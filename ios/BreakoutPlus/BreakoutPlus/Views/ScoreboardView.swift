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

    var body: some View {
        ZStack {
            Color(hex: "0F1419").edgesIgnoringSafeArea(.all)

            VStack(spacing: 16) {
                HStack {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Scoreboard")
                            .font(.system(size: 34, weight: .bold))
                            .foregroundColor(.white)
                        Text("Top runs across all modes.")
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

                if store.entries.isEmpty {
                    Spacer()
                    VStack(spacing: 10) {
                        Text("No scores yet.")
                            .foregroundColor(.white.opacity(0.9))
                            .font(.system(size: 18, weight: .semibold))
                        Text("Play a run to post your first score.")
                            .foregroundColor(.white.opacity(0.6))
                            .font(.system(size: 14))
                    }
                    Spacer()
                } else {
                    ScrollView {
                        VStack(spacing: 12) {
                            ForEach(Array(store.entries.enumerated()), id: \.element.id) { idx, entry in
                                ScoreRow(rank: idx + 1, entry: entry)
                            }
                        }
                        .padding(.vertical, 6)
                    }

                    HStack {
                        Button("Reset") {
                            store.reset()
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
                Text(entry.mode.displayName)
                    .foregroundColor(.white)
                    .font(.system(size: 16, weight: .semibold))
                Text("Level \(entry.level) • \(formatDuration(entry.durationSeconds))")
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

#Preview {
    ScoreboardView()
        .environmentObject(GameViewModel())
}
