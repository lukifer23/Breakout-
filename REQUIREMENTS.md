# Breakout+ Requirements

## Product Goals
- Ship a foldable-optimized Breakout/brickbreaker for Samsung Galaxy Z Fold 7.
- Deliver smooth 60 FPS gameplay with hardware-accelerated rendering.
- Provide multiple modes, levels, powerups, and brick variants with escalating difficulty.
- Include full UI/UX flow: splash, title, modes, gameplay, scoreboard, settings, how-to.
- Fully functional via CLI build and install (no Android Studio dependency).

## Target Device
- Samsung Galaxy Z Fold 7
- Android 14+ (app minSdk 26, targetSdk 35)
- Folded and unfolded layouts with hinge-aware padding.

## Functional Requirements

### Core Gameplay
- **Touch controls**: `GameEngine.handleTouch()` - drag moves paddle, release launches ball
- **Powerups**: `PowerUpType` enum, `GameEngine.applyPowerup()`, HUD shows active with timers
- **Multiple effects**: Multi-ball, laser beams, guardrail, shield, extra life, wide paddle, slow motion, fireball, magnet, gravity well, ball splitter, freeze, pierce
- **Brick variants**: `BrickType` enum with hit point scaling and special behaviors
- **Game modes**: `GameMode` enum controlling lives, timers, and rules

### UI/UX Features
- **Multiple screens**: `SplashActivity`, `MainActivity`, `ModeSelectActivity`, `GameActivity`, `SettingsActivity`, `ScoreboardActivity`, `HowToActivity`
- **Scoreboard**: `ScoreboardManager` with JSON persistence in SharedPreferences
- **Settings**: `SettingsManager` with toggles for sound/music/vibration/tips/sensitivity/handedness
- **How-to**: Expandable sections with controls, powerups, brick types, modes
- **Audio**: `GameAudioManager` with procedurally generated SFX via `tools/generate_sfx.py`

## Non-Functional Requirements

### Performance & Rendering
- **GPU acceleration**: OpenGL ES 2.0 via `GLSurfaceView` and `Renderer2D`
- **Stable timing**: Delta time clamping in `GameRenderer.onDrawFrame()` (50ms max)
- **60 FPS target**: Continuous render loop with optimized rect/circle geometry

### Device Optimization
- **Foldable support**: `layout-sw600dp`, `layout-sw720dp` variants, `FoldAwareActivity` with hinge padding
- **Z Fold 7 tuned**: Layouts optimized for 7.6" inner display and hinge behavior

### Development & Quality
- **CLI-only workflow**: Gradle build system, no Android Studio required
- **No placeholders**: All assets generated locally (audio, icons, layouts)
- **Complete implementation**: Full feature set with no stubs or mocks

## Success Criteria
- Clean build from CLI: `./gradlew assembleDebug`.
- Installable APK that runs on Z Fold 7 via ADB.
- Smooth gameplay in both folded and unfolded states.
- Core gameplay loop and all modes are playable.
