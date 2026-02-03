//
//  ContentView.swift
//  BreakoutPlusMac
//
//  Main navigation view for macOS Breakout+
//  Mouse-based controls instead of touch
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
            }
        }
        .navigationBarHidden(true)
    }
}

#Preview {
    ContentView()
        .environmentObject(GameViewModel())
        .frame(width: 800, height: 600)
}