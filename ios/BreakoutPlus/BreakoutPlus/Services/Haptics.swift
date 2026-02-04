//
//  Haptics.swift
//  BreakoutPlus
//
//  Simple haptics layer for gameplay feedback.
//

import Foundation
import UIKit

enum HapticEvent: Equatable {
    case light
    case medium
    case heavy
    case success
    case warning
    case error
}

final class Haptics {
    static let shared = Haptics()

    private let light = UIImpactFeedbackGenerator(style: .light)
    private let medium = UIImpactFeedbackGenerator(style: .medium)
    private let heavy = UIImpactFeedbackGenerator(style: .heavy)
    private let notify = UINotificationFeedbackGenerator()

    private init() {
        // Prime generators for lower latency.
        light.prepare()
        medium.prepare()
        heavy.prepare()
        notify.prepare()
    }

    func trigger(_ event: HapticEvent) {
        DispatchQueue.main.async {
            switch event {
            case .light:
                self.light.impactOccurred(intensity: 0.6)
                self.light.prepare()
            case .medium:
                self.medium.impactOccurred(intensity: 0.8)
                self.medium.prepare()
            case .heavy:
                self.heavy.impactOccurred(intensity: 0.95)
                self.heavy.prepare()
            case .success:
                self.notify.notificationOccurred(.success)
                self.notify.prepare()
            case .warning:
                self.notify.notificationOccurred(.warning)
                self.notify.prepare()
            case .error:
                self.notify.notificationOccurred(.error)
                self.notify.prepare()
            }
        }
    }
}

