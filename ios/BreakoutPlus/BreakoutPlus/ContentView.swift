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
                PrivacyView()
            case .dailyChallenges:
                DailyChallengesView()
            }
        }
        .navigationBarHidden(true)
    }
}

#Preview {
    ContentView()
        .environmentObject(GameViewModel())
}