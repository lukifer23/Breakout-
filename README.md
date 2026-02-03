# Breakout+

A premium brickbreaker game designed specifically for foldable devices like the Samsung Galaxy Z Fold 7. Features advanced physics, multiple game modes, dynamic brick behaviors, and comprehensive powerup systems with GPU-accelerated rendering at 60+ FPS.

## About

Breakout+ elevates the classic brickbreaker genre with modern mobile optimizations and innovative gameplay mechanics. Built from the ground up for foldable devices, it leverages the unique form factor for enhanced gameplay experiences across folded and unfolded states.

### Key Features
- **Foldable-First Design**: Optimized layouts for Samsung Galaxy Z Fold 7 (folded 7.6" and unfolded 12.4" displays)
- **GPU Acceleration**: OpenGL ES 2.0 rendering for smooth 60+ FPS gameplay
- **Advanced Physics**: Accurate collision detection with momentum preservation
- **Multiple Game Modes**: Classic, Timed Challenge, Endless, God Mode, and Level Rush
- **Dynamic Brick System**: 9 brick types with unique behaviors (moving, spawning, phase, boss)
- **Comprehensive Powerups**: 13 distinct powerups with visual effects and timers
- **Combo System**: Score multipliers for consecutive brick destruction
- **6 Visual Themes**: Unique color palettes and animated backgrounds
- **Audio System**: Procedural sound generation with individual volume controls
- **Data Logging**: Built-in analytics for debugging and potential AI training

Key principles:
- **Foldable-first design**: Optimized layouts and hinge-aware UI for seamless folded/unfolded transitions
- **Hardware acceleration**: OpenGL ES rendering for smooth 60 FPS gameplay
- **Complete feature set**: Multiple game modes, powerups, brick variations, and full audio
- **CLI-only development**: No Android Studio dependency, pure command-line workflow
- **Production-ready**: All assets generated locally, no external dependencies

## Target Device: Samsung Galaxy Z Fold 7

### Setup Instructions
1. Enable USB debugging in Developer Options
2. Connect device via USB
3. Verify connection: `adb devices`
4. Install APK: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
5. Launch: `adb shell am start -n com.breakoutplus.debug/com.breakoutplus.SplashActivity`

Breakout+ is a GPU-accelerated brickbreaker built for foldables, tuned for the Samsung Galaxy Z Fold 7. It uses OpenGL ES for the game loop, fold-aware UI layout, and a full set of modes, powerups, audio, and multi-hit bricks.

## Highlights
- OpenGL ES 2.0 rendering via `GLSurfaceView` for hardware acceleration.
- 60 FPS target with continuous render loop and stable delta time clamping (50ms max).
- Optimized for modern mobile GPUs with simple geometry (rects/circles) for consistent performance.
- Foldable-optimized layouts (`sw600dp`, `sw720dp`) plus hinge-aware padding using Jetpack WindowManager.
- Multiple modes: Classic, Timed Challenge, Endless, God Mode, Level Rush (45s with speed boost).
- Powerups: Multi-ball, Laser, Guardrail, Shield, Extra life, Wide paddle, Slow motion, Fireball, Magnet, Gravity Well, Ball Splitter, Freeze, Pierce.
- Brick variations: Standard, Reinforced, Armored, Explosive, Unbreakable, Moving, Spawning, Phase, Boss.
- Combo system: Score multipliers (x1.5-5x) for consecutive brick breaks within 2 seconds.
- Visual themes: 6 distinct themes (Neon, Sunset, Cobalt, Aurora, Forest, Lava) with unique color palettes and animated backgrounds.
- In-game HUD with score, lives, timer, level, combo indicators, and powerup status with countdown timers.
- Advanced audio: Individual volume controls (Master/Effects/Music), context-aware sounds per brick type.
- Enhanced visuals: Unique brick colors per theme, 3D bevel effects, animated powerups, particle systems.
- Full set of screens: Splash (animated), Title, Mode Select, Settings (volume controls), Scoreboard, How-To (expandable), Game.
- Sound effects and music generated programmatically (no placeholders).

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

## Build & Run (CLI only)

### Prerequisites
- JDK 17
- Android SDK (API 35 installed)
- Python 3 (for audio generation)
- ADB in PATH

### Build APK
```bash
./gradlew assembleDebug
```

### Install to Device
```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.breakoutplus.debug/com.breakoutplus.SplashActivity
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
```
app/src/main/java/com/breakoutplus
  MainActivity.kt
  GameActivity.kt
  SettingsActivity.kt
  ScoreboardActivity.kt
  ModeSelectActivity.kt
  HowToActivity.kt
  FoldAwareActivity.kt
  SettingsManager.kt
  ScoreboardManager.kt
app/src/main/java/com/breakoutplus/game
  GameGLSurfaceView.kt
  GameRenderer.kt
  GameEngine.kt
  LevelFactory.kt
  Renderer2D.kt
  GameAudioManager.kt
```

## Docs Index
- Requirements: `REQUIREMENTS.md`
- Architecture: `ARCHITECTURE.md`
- Design/UX: `DESIGN.md`
- Gameplay: `GAMEPLAY.md`
- Build/Run: `BUILD.md`
- Testing: `TESTING.md`
- Assets: `ASSETS.md`
- Roadmap: `ROADMAP.md`

## Icon Variants
The current icon is a vector adaptive icon with brick and ball motifs. If you want alternates, easy variants:
- **Neon Orbit**: Dark background, cyan orbit ring, hot pink ball.
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
