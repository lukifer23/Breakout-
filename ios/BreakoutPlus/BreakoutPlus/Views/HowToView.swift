//
//  HowToView.swift
//  BreakoutPlus
//
//  How-to-play screen
//

import SwiftUI

struct HowToView: View {
    @EnvironmentObject var gameViewModel: GameViewModel

    var body: some View {
        ZStack {
            Color(hex: "0F1419").edgesIgnoringSafeArea(.all)

            VStack(spacing: 16) {
                HStack {
                    Text("How to Play")
                        .font(.system(size: 34, weight: .bold))
                        .foregroundColor(.white)
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

                ScrollView {
                    VStack(spacing: 14) {
                        SectionCard(title: "Quick Start", lines: [
                            "Swipe anywhere to move the paddle.",
                            "Tap to launch the ball.",
                            "Collect powerups to stack abilities.",
                            "Watch the HUD for score, lives, and timers."
                        ])

                        SectionCard(title: "Goal", lines: [
                            "Clear all breakable bricks to advance.",
                            "Keep the ball in play - lose all lives and the run ends.",
                            "Stack powerups to create huge combos."
                        ])

                        SectionCard(title: "Controls", lines: [
                            "Drag anywhere to move the paddle.",
                            "Release your finger to launch the ball (when it’s resting on the paddle).",
                            "Laser powerup: tap FIRE to shoot."
                        ])

                        SectionCard(title: "Brick Types", lines: [
                            "Normal: 1 hit.",
                            "Reinforced: 2 hits.",
                            "Armored: 3 hits.",
                            "Explosive: damages nearby bricks when destroyed.",
                            "Unbreakable: only breaks with Fireball (or sustained lasers).",
                            "Moving: slides horizontally.",
                            "Spawning: splits into smaller bricks when destroyed.",
                            "Phase: tougher brick with bonus drops.",
                            "Boss: heavyweight brick with guaranteed reward.",
                            "Invader: moving fleet brick used in Invaders mode."
                        ])

                        SectionCard(title: "Powerups", lines: [
                            "Multi-ball: spawns extra balls.",
                            "Laser: enables FIRE beams.",
                            "Guardrail: bottom safety net while active.",
                            "Shield: saves you from a miss.",
                            "Extra life: +1 life (not in God Mode).",
                            "Wide paddle: easier catches.",
                            "Shrink: temporarily reduces paddle width.",
                            "Slow motion: reduces game speed.",
                            "Overdrive: speeds up the action.",
                            "Freeze: near-stop time briefly.",
                            "Fireball: smash unbreakable bricks; passes through.",
                            "Pierce: pass through multiple bricks.",
                            "Magnet: attracts falling powerups to the paddle.",
                            "Gravity well: bends ball paths toward center.",
                            "Ball splitter: multiplies your current balls.",
                            "Ricochet: grants extra wall bounces.",
                            "Time warp: slows world movement while keeping ball speed.",
                            "2x score: doubles score rewards briefly."
                        ])

                        SectionCard(title: "Modes", lines: [
                            "Classic: balanced progression.",
                            "Timed: 2:30 to maximize score.",
                            "Endless: infinite levels with scaling challenge.",
                            "God Mode: infinite lives for practice.",
                            "Rush: 55 seconds per level, one life.",
                            "Volley: aim once, launch a chain of balls, then brace for descending rows.",
                            "Tunnel Siege: breach a fortress wall through a narrow tunnel gate.",
                            "Survival: one life with faster speed ramps.",
                            "Invaders: bounce shots to clear ships while dodging fire.",
                            "Zen Mode: relaxed pacing with no score or life pressure."
                        ])
                    }
                    .padding(.vertical, 6)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 60)
            .padding(.bottom, 24)
        }
    }
}

private struct SectionCard: View {
    let title: String
    let lines: [String]

    var bodyView: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .foregroundColor(.white)
                .font(.system(size: 16, weight: .bold))
            ForEach(lines, id: \.self) { line in
                Text("• \(line)")
                    .foregroundColor(.white.opacity(0.78))
                    .font(.system(size: 14))
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color(hex: "1A1F26"))
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .stroke(Color(hex: "31E1F7").opacity(0.16), lineWidth: 1)
        )
        .cornerRadius(14)
    }

    var body: some View { bodyView }
}

#Preview {
    HowToView()
        .environmentObject(GameViewModel())
}
