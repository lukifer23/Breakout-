# Breakout+ iOS Development

## Current Status

**Playability: Playable (Core Loop + UI)** ✅

SwiftUI + SpriteKit implementation with a real game loop, pause/end overlays, multiple modes, brick/powerup variety, and a persistent local scoreboard.

## What's Built

### ✅ Core Architecture
- **SwiftUI Navigation**: ContentView with screen routing
- **GameViewModel**: State management with ObservableObject
- **SpriteKit Integration**: GameScene with real-time rendering

### ✅ Game Models (Direct Ports)
- **GameMode**: All 5 modes (Classic, Timed, Endless, God, Rush)
- **BrickType**: 9 brick types with behaviors
- **PowerUpType**: 13 powerup types with effects
- **LevelTheme**: Multiple themes with unique palettes

### ✅ Core Engine
- **GameEngine**: Physics, collisions, level progression, powerups, and safe iteration (no mutation-crash traps)
- **Ball/Brick/Paddle/PowerUp**: Game object models
- **LevelFactory**: Patterned levels + endless procedural generation
- **Game Loop**: 60 FPS target with SpriteKit integration

### ✅ UI Framework
- **SplashView**: Animated launch screen
- **MenuView**: Game mode selection with styled buttons
- **GameView**: SpriteKit game scene with HUD + Pause/Resume + Fire button (Laser)
- **Overlays**: Pause, level complete, and game over
- **Settings**: Real toggles (stored via AppStorage/UserDefaults)
- **Scoreboard**: Persistent local highscores (UserDefaults + Codable)
- **How-To**: In-app guide

## Build & Run

### Prerequisites
- Xcode 15+
- iOS simulator/device supported by your Xcode install
- Swift 5.9+

### CLI Build (Simulator)
From repo root:
```bash
xcodebuild -project ios/BreakoutPlus/BreakoutPlus.xcodeproj \
  -scheme BreakoutPlus -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.2' build
```

### CLI Install + Launch (Booted Simulator)
```bash
OUT=$(xcodebuild -project ios/BreakoutPlus/BreakoutPlus.xcodeproj -scheme BreakoutPlus -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.2' -showBuildSettings)

TARGET_BUILD_DIR=$(printf "%s" "$OUT" | rg -m1 '^\\s*TARGET_BUILD_DIR\\s*=\\s*(.*)$' -r '$1')
WRAPPER_NAME=$(printf "%s" "$OUT" | rg -m1 '^\\s*WRAPPER_NAME\\s*=\\s*(.*)$' -r '$1')
BUNDLE_ID=$(printf "%s" "$OUT" | rg -m1 '^\\s*PRODUCT_BUNDLE_IDENTIFIER\\s*=\\s*(.*)$' -r '$1')

xcrun simctl bootstatus booted -b || true
xcrun simctl install booted "$TARGET_BUILD_DIR/$WRAPPER_NAME"
xcrun simctl launch booted "$BUNDLE_ID"
```

### CLI-Only TestFlight Deployment
For Cursor-only development without Xcode GUI:

#### Prerequisites
- Apple Developer Program membership ($99/year)
- App Store Connect API key

#### Automated CLI Workflow
```bash
cd ios/BreakoutPlus

# 1. Setup API key (run once)
./setup_testflight_api.sh

# 2. Build release IPA
./build_testflight.sh

# 3. Upload to TestFlight
./upload_testflight.sh
```

#### Manual Upload Alternative
1. Run `./build_testflight.sh` to create IPA
2. Go to https://appstoreconnect.apple.com/
3. Navigate to Breakout+ → TestFlight
4. Upload the generated IPA file

### Current Gameplay Features
- ✅ Ball physics and paddle control
- ✅ Brick destruction and scoring
- ✅ Patterned levels + endless procedural levels
- ✅ Powerup spawning and collection (laser/guardrail/shield/etc)
- ✅ Combo system
- ✅ Lives, pause, level complete, and game over logic

## Next Steps (Recommended)
- **Tune feedback**: throttle/tune SFX + haptics intensity (bounces can be noisy) and verify pause/resume behavior.
- **Visual polish**: richer brick hit states (boss/phase/moving), more distinct powerup visuals, and smoother transitions.
- **Repo hygiene**: stop tracking SwiftPM build artifacts (`ios/.build/`) so diffs stay clean.
- **Tests**: add lightweight engine behavior tests (life loss, scoring, powerups) without needing Xcode UI.

## Architecture Decisions

### SwiftUI + SpriteKit Hybrid
**Why this approach:**
- SwiftUI for modern, declarative UI (menus, settings)
- SpriteKit for high-performance 2D game rendering
- Clean separation of concerns
- Future-proof architecture

### Direct Android Port Strategy
**Benefits:**
- Feature parity maintained
- Proven game design
- Faster development
- Consistent player experience

## Development Notes

### Code Organization
```
BreakoutPlus/
├── BreakoutPlusApp.swift      # App entry point
├── ContentView.swift          # Navigation router
├── ViewModels/
│   └── GameViewModel.swift    # State management
├── Views/                     # SwiftUI screens
├── Core/                      # Game logic
│   ├── GameEngine.swift       # Main game controller
│   ├── LevelFactory.swift     # Level generation
│   └── Models/               # Game objects
├── Services/                  # Persistence (scoreboard)
└── Resources/                # Assets (future)
```

### Performance Targets
- **Frame Rate**: 60 FPS minimum
- **Memory**: <50MB during gameplay
- **Load Times**: <2 seconds for level transitions
- **Responsiveness**: <16ms input lag

## Testing Status

### Functional Tests ✅
- App launches and navigates between screens
- Game mode selection works
- Basic gameplay loop functional
- Touch controls responsive

### Game Logic Tests 🚧
- Physics calculations verified
- Collision detection working
- Scoring system functional
- Powerup mechanics implemented

### UI/UX Tests 🚧
- Responsive layouts
- Visual feedback
- Animation performance
- Accessibility compliance

---

## Contact & Development

This iOS port is developed in parallel with the Android version, maintaining feature parity and cross-platform consistency.

**Status**: Active development - playable core loop
**Compatibility**: Simulator + device via Xcode CLI tools
