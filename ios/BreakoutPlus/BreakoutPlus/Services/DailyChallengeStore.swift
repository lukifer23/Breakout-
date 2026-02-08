//
//  DailyChallengeStore.swift
//  BreakoutPlus
//
//  Persistent storage for daily challenges
//

import Foundation

final class DailyChallengeStore: ObservableObject {
    static let shared = DailyChallengeStore()

    @Published private(set) var challenges: [DailyChallenge] = []

    private let key = "breakoutplus.daily_challenges.v1"
    private let dateKey = "breakoutplus.challenges_date.v1"

    private init() {
        loadChallenges()
    }

    func loadChallenges() {
        let today = Calendar.current.startOfDay(for: Date())
        let storedDate = UserDefaults.standard.object(forKey: dateKey) as? Date ?? Date.distantPast

        // Check if we need to generate new challenges
        if !Calendar.current.isDate(storedDate, inSameDayAs: today) {
            challenges = DailyChallengeManager.generateDailyChallenges()
            saveChallenges()
            UserDefaults.standard.set(today, forKey: dateKey)
        } else {
            challenges = loadFromStorage()
        }
    }

    func updateProgress(type: ChallengeType, value: Int = 1) {
        let newlyCompleted = DailyChallengeManager.updateChallengeProgress(challenges: &challenges, type: type, value: value)
        saveChallenges()

        // Handle rewards for newly completed challenges
        for challenge in newlyCompleted {
            claimReward(challenge)
        }
    }

    func claimReward(_ challenge: DailyChallenge) {
        if challenge.completed && !challenge.claimed {
            // Mark as claimed
            if let index = challenges.firstIndex(where: { $0.id == challenge.id }) {
                challenges[index].claimed = true
                saveChallenges()
            }

            // Apply reward
            switch challenge.rewardType {
            case .scoreMultiplier:
                // Could apply temporary score multiplier
                print("Applied \(challenge.rewardValue)% score multiplier")
            case .streakBonus:
                // Could apply temporary streak bonus
                print("Applied \(challenge.rewardValue) streak bonus")
            case .cosmeticUnlock:
                // Could unlock cosmetic enhancement
                print("Unlocked cosmetic enhancement")
            case .themeUnlock:
                // Could unlock new theme
                print("Unlocked new theme")
            }
        }
    }

    private func loadFromStorage() -> [DailyChallenge] {
        guard let data = UserDefaults.standard.data(forKey: key) else {
            return DailyChallengeManager.generateDailyChallenges()
        }
        return (try? JSONDecoder().decode([DailyChallenge].self, from: data)) ?? DailyChallengeManager.generateDailyChallenges()
    }

    private func saveChallenges() {
        guard let data = try? JSONEncoder().encode(challenges) else { return }
        UserDefaults.standard.set(data, forKey: key)
    }

    func reset() {
        challenges = DailyChallengeManager.generateDailyChallenges()
        saveChallenges()
        UserDefaults.standard.removeObject(forKey: dateKey)
    }
}