# Breakout+ iOS

A substantial SwiftUI/SpriteKit port under BreakoutPlus. Android is the gameplay
reference. Full Xcode was unavailable during the October 5, 2026 maintenance
milestone, so no current iOS build, simulator or XCTest success is claimed.
Earlier build claims are historical and need fresh verification.

Generated Swift .build directories, the compiled test binary and obsolete Xcode
project backup were removed from tracking. Authored Swift/assets remain. No
XCTest target or iOS CI qualification has been added in this stopping-point pass.

Use full Xcode and an installed simulator:

```sh
xcodebuild -project ios/BreakoutPlus/BreakoutPlus.xcodeproj \
  -scheme BreakoutPlus -configuration Debug \
  -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

See [build instructions](../Docs/BUILD.md), [parity gaps](../Docs/PARITY.md),
[remaining work](../Docs/ROADMAP.md) and [privacy](../Docs/PRIVACY_POLICY.md).
Daily reward application, high-speed collision, Volley/Tunnel recovery, Magnet,
HUD/level advance and deterministic fixture parity require substantive work.
UserDefaults may participate in Apple-managed backup; no app telemetry backend
exists. TestFlight upload now invokes a real upload command, but was not run.

BreakoutPlusMac is archived/experimental, a stale five-mode fork with no current
release support. Archive contains historical spikes/recaps, not current evidence.
