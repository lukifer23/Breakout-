# Breakout+

A premium brickbreaker game designed specifically for foldable devices like the Samsung Galaxy Z Fold 7, with a native iOS port for cross-platform gaming. Features advanced physics, multiple game modes, dynamic brick behaviors, and comprehensive powerup systems with GPU-accelerated rendering at 60+ FPS.

## Goals

**Product Goal**: Deliver Breakout+ as the premier foldable-first brickbreaker for Samsung Galaxy Z Fold 7 and modern Android devices, with a polished cross-platform iOS port. Achieve 60+ FPS gameplay through GPU acceleration, provide a complete feature set (9 brick types, 13 powerups, 5 game modes, 6 visual themes), and maintain full CLI-only development workflow with no external dependencies or placeholders.

**Key Success Criteria**:
- Smooth 60 FPS gameplay on Z Fold 7 (folded and unfolded)
- Complete feature parity between Android and iOS implementations
- Production-ready code with comprehensive documentation
- CLI-only build and deployment capability
- No stubs, mocks, or placeholder assets

## About

Breakout+ elevates the classic brickbreaker genre with modern mobile optimizations and innovative gameplay mechanics. Built from the ground up for foldable devices, it leverages the unique form factor for enhanced gameplay experiences across folded and unfolded states.

### Key Features
- **Cross-Platform**: Native Android (production-ready) and iOS (in development) implementations
- **GPU Acceleration**: OpenGL ES 2.0 (Android) / Metal (iOS) rendering at 60+ FPS
- **Advanced Physics**: Accurate collision detection with momentum preservation
- **Multiple Game Modes**: Classic, Timed Challenge, Endless, God Mode, and Level Rush
- **Dynamic Brick System**: 9 brick types with unique behaviors (moving, spawning, phase, boss)
- **Comprehensive Powerups**: 13 distinct powerups with visual effects and timers
- **Combo System**: Score multipliers for consecutive brick destruction
- **6 Visual Themes**: Unique color palettes and animated backgrounds
- **Audio System**: Procedural sound generation with individual volume controls
- **Data Logging**: Built-in analytics for debugging
- **Foldable Optimization**: Android version optimized for Samsung Galaxy Z Fold 7

Key principles:
- **Foldable-first design**: Optimized layouts and hinge-aware UI for seamless folded/unfolded transitions
- **Hardware acceleration**: OpenGL ES rendering for smooth 60 FPS gameplay
- **Complete feature set**: Multiple game modes, powerups, brick variations, and full audio
- **CLI-only development**: No Android Studio dependency, pure command-line workflow
- **Production-ready**: All assets generated locally, no external dependencies

## Platforms

### Android: Samsung Galaxy Z Fold 7
**Status**: Production-ready (v1.0.0)

#### Setup Instructions
1. Enable USB debugging in Developer Options
2. Connect device via USB
3. Verify connection: `adb devices`
4. Install APK: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

### iOS: iPhone & iPad
**Status**: Playable MVP (v0.1.0) - CLI-buildable, simulator-tested

#### CLI Development Setup
1. Ensure Xcode 15+ is installed
2. Build: `cd ios/BreakoutPlus && xcodebuild -scheme BreakoutPlus -sdk iphonesimulator -configuration Debug build`
3. Boot simulator: `xcrun simctl boot <device-id>`
4. Install: `xcrun simctl install booted "$(find ~/Library/Developer/Xcode/DerivedData -name 'BreakoutPlus.app' -type d | head -1)"`
5. Launch: `xcrun simctl launch booted com.breakoutplus.ios`

**Current iOS Features**: Core gameplay, ball physics, brick destruction, powerups, all game modes, SpriteKit rendering

Breakout+ is a GPU-accelerated brickbreaker built for foldables, tuned for the Samsung Galaxy Z Fold 7. It uses OpenGL ES for the game loop, fold-aware UI layout, and a full set of modes, powerups, audio, and multi-hit bricks.

## Highlights
- OpenGL ES 2.0 rendering via `GLSurfaceView` for hardware acceleration.
- Vsync-paced rendering using Choreographer-driven frame pacing with surface frame-rate hints.
- Optimized for modern mobile GPUs with simple geometry (rects/circles) for consistent performance and frame pacing stability.
- Foldable-optimized layouts (`sw600dp`, `sw720dp`) plus hinge-aware padding using Jetpack WindowManager.
- Multiple modes: Classic, Timed Challenge, Endless, God Mode, Level Rush (45s with speed boost).
- Powerups: Multi-ball, Laser, Guardrail, Shield, Extra life, Wide paddle, Slow motion, Fireball, Magnet, Gravity Well, Ball Splitter, Freeze, Pierce.
- Brick variations: Standard, Reinforced, Armored, Explosive, Unbreakable, Moving, Spawning, Phase, Boss.
- Combo system: Score multipliers (x1.5-5x) for consecutive brick breaks within 2 seconds.
- Visual themes: 6 distinct themes (Neon, Sunset, Cobalt, Aurora, Forest, Lava) with unique color palettes and animated backgrounds.
- In-game HUD with score, lives, timer, level, combo indicators, and powerup chips with countdown timers.
- Daily Challenges with tracked progress.
- Advanced audio: Individual volume controls (Master/Effects/Music), context-aware sounds per brick type.
- Enhanced visuals: Unique brick colors per theme, 3D bevel effects, animated powerups, particle systems.
- Full set of screens: Splash (animated), Title, Mode Select, Settings (volume controls), Scoreboard, How-To (expandable), Game.
- Privacy Policy screen with in-app disclosure.
- Audio: Android generates SFX/music locally; iOS uses bundled WAV SFX + looping music (still local assets, no placeholders).

## Features

### Game Modes
- **Classic**: Standard breakout progression with escalating difficulty
- **Timed Challenge**: Score as high as possible in 2:30 (30% faster ball speed)
- **Endless**: Infinite procedurally generated levels with scaling difficulty
- **God Mode**: Practice mode with infinite lives, no penalties
- **Level Rush**: Beat each stage before 45-second timer expires (50% faster ball speed)

### Powerups (13 Total)
- **Core**: Multi-ball, Laser, Guardrail, Shield, Extra life, Wide paddle, Slow motion, Fireball
- **Advanced**: Magnet (attracts powerups), Gravity Well (attractive force), Ball Splitter (creates extra balls), Freeze (time stop), Pierce (through bricks)

### Brick Types (9 Total)
- **Standard**: Normal (1 hit), Reinforced (2 hits), Armored (3 hits), Explosive (chain damage), Unbreakable (requires special attacks)
- **Dynamic**: Moving (slides horizontally), Spawning (creates child bricks), Phase (multi-stage destruction), Boss (powerful multi-phase)

### Gameplay Systems
- **Combo System**: x1.5-5x score multipliers for consecutive brick breaks within 2 seconds
- **Dynamic Physics**: Mode-specific ball speeds, collision detection, momentum preservation
- **Progressive Difficulty**: Scaling brick counts, hit points, and spawn rates per level
- **Visual Themes**: 6 unique themes with distinct color palettes and animated backgrounds

### Audio System
- **Procedural Generation**: All SFX and music generated algorithmically
- **Individual Controls**: Separate volume sliders for Master/Effects/Music
- **Context-Aware**: Different sounds for each brick type and impact type
- **Haptic Feedback**: Vibration for significant game events

### Audio & Controls
- Individual volume controls: Master, Effects, Music (independent of system volume)
- Context-aware sound effects: Different sounds for each brick type and impact
- Enhanced audio feedback: Haptic vibration, screen flash effects

### Gameplay Balance
- Optimized ball speed and physics for better gameplay feel
- Increased powerup drop rates for more frequent special abilities
- Fine-tuned ball and brick sizing for denser, more challenging layouts
- Enhanced visual variety with improved brick color gradients

## Controls
- Drag anywhere to move the paddle.
- Release drag or tap to launch the ball.
- Two-finger tap fires lasers when Laser powerup is active.

## Modes
- **Classic**: Standard progression with escalating difficulty.
- **Timed Challenge**: 2:30 to score as high as possible.
- **Endless**: Infinite levels with increasing speed and density.
- **God Mode**: No life loss for practice or testing.
- **Level Rush**: 45 seconds per stage, one life.

## Brick Types
- **Standard**: One hit.
- **Reinforced**: Two hits.
- **Armored**: Three hits.
- **Explosive**: Damages surrounding bricks on destruction.
- **Unbreakable**: Requires Fireball or Laser barrages to break.
- **Moving**: Slides horizontally while you play.
- **Spawning**: Breaks into child bricks.
- **Phase**: Changes phases with new HP totals.
- **Boss**: Multi-phase heavyweight brick.

## Powerups
- **Multi-ball**: Adds two extra balls.
- **Laser**: Paddle fires beams.
- **Guardrail**: Temporary safety net.
- **Shield**: Absorbs a miss.
- **Extra Life**: +1 life.
- **Wide Paddle**: Expands paddle.
- **Slow**: Slows time briefly.
- **Fireball**: Ball pierces bricks.
- **Magnet**: Pulls powerups toward the paddle.
- **Gravity Well**: Bends ball paths toward center.
- **Ball Splitter**: Splits balls into more.
- **Freeze**: Nearly stops time for a moment.
- **Pierce**: Balls pass through bricks.

## Build & Run

### Android (CLI only)

#### Prerequisites
- JDK 17, Android SDK (API 35 installed), Python 3, ADB in PATH

#### Build APK
```bash
./gradlew assembleDebug
```

#### Install to Device
```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.breakoutplus.debug/com.breakoutplus.SplashActivity
```

#### Release APK (signed)
Set signing environment variables, then build:
```bash
export BP_RELEASE_STORE_FILE="/absolute/path/to/keystore.jks"
export BP_RELEASE_STORE_PASSWORD="your_store_password"
export BP_RELEASE_KEY_ALIAS="your_key_alias"
export BP_RELEASE_KEY_PASSWORD="your_key_password"
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

If signing variables are not set, the release build will use the debug keystore for local testing.

#### Play Store Bundle (AAB)
Build the bundle required for Play Store uploads:
```bash
./gradlew bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab`

### iOS (Xcode)

#### Prerequisites
- Xcode 15+, iOS 15+, Swift 5.9+, macOS

#### Build and Run
1. Open `ios/BreakoutPlus/BreakoutPlus.xcodeproj` in Xcode
2. Select iOS Simulator or device
3. Build: ⌘+B, Run: ⌘+R

#### Alternative: Swift Package Manager
```bash
cd ios
swift build
```

### Regenerate Audio
All sound effects are procedurally generated:
```bash
python3 tools/generate_sfx.py
```

### Troubleshooting
- **SDK missing**: Install platform 35 via `sdkmanager "platforms;android-35"`
- **ADB not found**: Device not connected or USB debugging disabled
- **Build fails**: Ensure JDK 17 and correct Android SDK path

## Foldable Optimization
- `layout-sw600dp/` layouts increase spacing and scale on the Z Fold 7 open state.
- `FoldAwareActivity` adjusts padding to avoid hinge overlap.
- Game world scales to the available aspect ratio.

## Project Layout

### Android (Kotlin)
```
app/src/main/java/com/breakoutplus
├── MainActivity.kt, GameActivity.kt, SettingsActivity.kt
├── ScoreboardActivity.kt, ModeSelectActivity.kt, HowToActivity.kt
├── DailyChallengesActivity.kt, PrivacyActivity.kt
├── FoldAwareActivity.kt, SettingsManager.kt, ScoreboardManager.kt, DailyChallengeStore.kt
└── game/
    ├── GameGLSurfaceView.kt, GameRenderer.kt, GameEngine.kt
    ├── LevelFactory.kt, Renderer2D.kt, GameAudioManager.kt
    ├── GameMode.kt, BrickType.kt, PowerUpType.kt
    └── GameLogger.kt
```

### iOS (Swift)
```
ios/BreakoutPlus/BreakoutPlus/
├── BreakoutPlusApp.swift, ContentView.swift
├── ViewModels/GameViewModel.swift
├── Views/ (SplashView, MenuView, GameView, etc.)
├── Core/GameEngine.swift
├── Core/Models/ (Ball, Brick, Paddle, PowerUp, LevelTheme)
└── Models/ (GameMode, BrickType, PowerUpType)
```

## Docs Index
Project docs live in `Docs/`:
- **Requirements**: `Docs/REQUIREMENTS.md`
- **Architecture**: `Docs/ARCHITECTURE.md`
- **Design/UX**: `Docs/DESIGN.md`
- **Gameplay**: `Docs/GAMEPLAY.md`
- **Build/Run**: `Docs/BUILD.md`
- **Testing**: `Docs/TESTING.md`
- **Assets**: `Docs/ASSETS.md`
- **Roadmap**: `Docs/ROADMAP.md`
- **Data Safety**: `Docs/DATA_SAFETY.md`
- **Privacy Policy**: `Docs/PRIVACY_POLICY.md`
- **Release**: `Docs/RELEASE_CHECKLIST.md`, `Docs/STORE_LISTING.md`
- **iOS Port**: `ios/README.md`, `ios/ARCHITECTURE.md`, `ios/ROADMAP.md`

## Icon Variants
The current icon is a neon orbit vector adaptive icon (cyan ring, hot pink ball, plus mark). If you want alternates, easy variants:
- **Folded Edge**: Split gradient background suggesting the hinge.
- **Minimal Plus**: Large “+” with a single brick row.
Icon foregrounds live in `app/src/main/res/drawable/` as:
- `ic_launcher_foreground_neon.xml`
- `ic_launcher_foreground_fold.xml`
- `ic_launcher_foreground_minimal.xml`

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Notes
- The HUD timer counts down in timed modes and counts up otherwise.
- Scores are stored locally in SharedPreferences.
- All assets are generated or defined locally—no placeholders or stubs.
