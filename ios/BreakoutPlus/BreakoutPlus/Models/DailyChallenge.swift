//
//  DailyChallenge.swift
//  BreakoutPlus
//
//  Daily challenge definitions and progress tracking
//

import Foundation

enum ChallengeType: String, Codable {
    case bricksDestroyed = "bricks_destroyed"
    case scoreAchieved = "score_achieved"
    case comboMultiplier = "combo_multiplier"
    case powerupsCollected = "powerups_collected"
    case perfectLevel = "perfect_level"
    case laserFired = "laser_fired"
    case timeUnderLimit = "time_under_limit"
    case multiBallActive = "multiball_active"
}

enum RewardType: String, Codable {
    case scoreMultiplier = "score_multiplier"
    case streakBonus = "streak_bonus"
    case cosmeticUnlock = "cosmetic_unlock"
    case themeUnlock = "theme_unlock"
}

struct DailyChallenge: Identifiable, Codable, Equatable {
    let id: String
    let title: String
    let description: String
    let type: ChallengeType
    let targetValue: Int
    let rewardType: RewardType
    let rewardValue: Int

    var progress: Int = 0
    var completed: Bool = false
    var claimed: Bool = false

    var progressText: String {
        "\(min(progress, targetValue))/\(targetValue)"
    }

    var isCompleted: Bool {
        progress >= targetValue
    }

    mutating func updateProgress(_ value: Int) {
        if completed { return }
        progress = min(progress + value, targetValue)
        if progress >= targetValue {
            completed = true
        }
    }

    static func == (lhs: DailyChallenge, rhs: DailyChallenge) -> Bool {
        lhs.id == rhs.id && lhs.progress == rhs.progress && lhs.completed == rhs.completed
    }
}

struct DailyChallengeManager {
    static func generateDailyChallenges() -> [DailyChallenge] {
        return [
            DailyChallenge(
                id: "bricks_25",
                title: "Brick Buster",
                description: "Destroy 25 bricks",
                type: .bricksDestroyed,
                targetValue: 25,
                rewardType: .scoreMultiplier,
                rewardValue: 10
            ),
            DailyChallenge(
                id: "score_500",
                title: "Score Hunter",
                description: "Achieve 500 points",
                type: .scoreAchieved,
                targetValue: 500,
                rewardType: .streakBonus,
                rewardValue: 5
            ),
            DailyChallenge(
                id: "combo_3x",
                title: "Combo Starter",
                description: "Achieve 3x combo multiplier",
                type: .comboMultiplier,
                targetValue: 3,
                rewardType: .streakBonus,
                rewardValue: 2
            ),
            DailyChallenge(
                id: "powerups_5",
                title: "Power Collector",
                description: "Collect 5 powerups",
                type: .powerupsCollected,
                targetValue: 5,
                rewardType: .cosmeticUnlock,
                rewardValue: 1
            ),
            DailyChallenge(
                id: "perfect_level",
                title: "Perfectionist",
                description: "Complete a level without losing a life",
                type: .perfectLevel,
                targetValue: 1,
                rewardType: .themeUnlock,
                rewardValue: 1
            )
        ]
    }

    static func updateChallengeProgress(challenges: inout [DailyChallenge], type: ChallengeType, value: Int = 1) -> [DailyChallenge] {
        var newlyCompleted: [DailyChallenge] = []
        for i in 0..<challenges.count {
            if challenges[i].type == type && !challenges[i].completed {
                let oldCompleted = challenges[i].completed
                challenges[i].updateProgress(value)
                if challenges[i].completed && !oldCompleted {
                    newlyCompleted.append(challenges[i])
                }
            }
        }
        return newlyCompleted
    }

    static func getRewardDescription(challenge: DailyChallenge) -> String {
        switch challenge.rewardType {
        case .scoreMultiplier:
            return "+\(challenge.rewardValue)% score multiplier"
        case .streakBonus:
            return "+\(challenge.rewardValue) streak bonus"
        case .cosmeticUnlock:
            return "Unlock cosmetic enhancement"
        case .themeUnlock:
            return "Unlock new theme"
        }
    }
}