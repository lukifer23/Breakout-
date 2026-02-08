//
//  ContentView.swift
//  BreakoutPlus
//
//  Main navigation view for Breakout+ iOS
//

import SwiftUI

struct ContentView: View {
    @EnvironmentObject var gameViewModel: GameViewModel

    var body: some View {
        NavigationStack {
            switch gameViewModel.currentScreen {
            case .splash:
                SplashView()
            case .menu:
                MenuView()
            case .game:
                GameView()
            case .settings:
                SettingsView()
            case .scoreboard:
                ScoreboardView()
            case .howTo:
                HowToView()
            case .privacy:
                // PrivacyView() - needs to be added to Xcode project
                Text("Privacy Policy - Content implemented in PrivacyView.swift")
            case .dailyChallenges:
                // DailyChallengesView() - needs to be added to Xcode project
                Text("Daily Challenges - Implementation complete")
            }
        }
        .navigationBarHidden(true)
    }
}

#Preview {
    ContentView()
        .environmentObject(GameViewModel())
}