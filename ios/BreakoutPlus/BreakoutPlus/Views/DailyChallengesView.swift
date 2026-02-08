//
//  DailyChallengesView.swift
//  BreakoutPlus
//
//  Daily challenges screen
//

import SwiftUI

struct DailyChallengesView: View {
    @EnvironmentObject var gameViewModel: GameViewModel
    @ObservedObject private var challengeStore = DailyChallengeStore.shared

    var body: some View {
        ZStack {
            Color(hex: "0F1419").edgesIgnoringSafeArea(.all)

            VStack(spacing: 16) {
                HStack {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Daily Challenges")
                            .font(.system(size: 34, weight: .bold))
                            .foregroundColor(.white)
                        Text("Complete for rewards")
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

                if challengeStore.challenges.isEmpty {
                    Spacer()
                    VStack(spacing: 10) {
                        Text("No challenges available")
                            .foregroundColor(.white.opacity(0.9))
                            .font(.system(size: 18, weight: .semibold))
                        Text("Check back tomorrow for new challenges.")
                            .foregroundColor(.white.opacity(0.6))
                            .font(.system(size: 14))
                    }
                    Spacer()
                } else {
                    ScrollView {
                        VStack(spacing: 12) {
                            ForEach(challengeStore.challenges) { challenge in
                                ChallengeRow(challenge: challenge)
                            }
                        }
                        .padding(.vertical, 6)
                    }

                    HStack {
                        Button("Reset (Debug)") {
                            challengeStore.reset()
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

private struct ChallengeRow: View {
    let challenge: DailyChallenge

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(challenge.title)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                    Text(challenge.description)
                        .font(.system(size: 14, weight: .regular))
                        .foregroundColor(.white.opacity(0.7))
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 4) {
                    Text(challenge.progressText)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(progressColor)
                    if challenge.completed {
                        Text("✓")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.green)
                    }
                }
            }

            if challenge.completed {
                Text("Reward: \(DailyChallengeManager.getRewardDescription(challenge: challenge))")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(.yellow.opacity(0.8))
                    .padding(.vertical, 4)
                    .padding(.horizontal, 8)
                    .background(Color.yellow.opacity(0.1))
                    .cornerRadius(6)
            }

            // Progress bar
            GeometryReader { geometry in
                ZStack(alignment: .leading) {
                    Rectangle()
                        .fill(Color.white.opacity(0.2))
                        .frame(height: 4)
                        .cornerRadius(2)

                    Rectangle()
                        .fill(progressColor)
                        .frame(width: geometry.size.width * min(CGFloat(challenge.progress) / CGFloat(challenge.targetValue), 1.0), height: 4)
                        .cornerRadius(2)
                }
            }
            .frame(height: 4)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color(hex: "1A1F26"))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(challenge.completed ? Color.green.opacity(0.3) : Color(hex: "31E1F7").opacity(0.16), lineWidth: 1)
        )
        .cornerRadius(12)
    }

    private var progressColor: Color {
        if challenge.completed {
            return .green
        } else if challenge.progress > 0 {
            return .yellow
        } else {
            return .gray
        }
    }
}

#Preview {
    DailyChallengesView()
        .environmentObject(GameViewModel())
}