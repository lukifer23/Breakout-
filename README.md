# Breakout+

This project was entirely vibecoded as an experiment to test out new model abilities.

Breakout+ is a GPU-accelerated brickbreaker built for foldables, tuned for the Samsung Galaxy Z Fold 7. It uses OpenGL ES for the game loop, fold-aware UI layout, and a full set of modes, powerups, audio, and multi-hit bricks.

## Highlights
- OpenGL ES 2.0 rendering via `GLSurfaceView` for hardware acceleration.
- 60 FPS continuous render loop with stable delta time.
- Foldable-optimized layouts (`sw600dp`) plus hinge-aware padding using Jetpack WindowManager.
- Multiple modes: Classic, Timed Challenge, Endless, God Mode, Level Rush.
- Powerups: Multi-ball, Laser paddle, Guardrail, Shield, Extra life, Wide paddle, Slow motion, Fireball.
- Brick variations: Standard, Reinforced, Armored, Explosive, Unbreakable.
- In-game HUD with score, lives, timer, level, and powerup status.
- Full set of screens: Splash, Title, Mode Select, Settings, Scoreboard, How-To, Game.
- Sound effects and music generated programmatically (no placeholders).

## Controls
- Drag anywhere to move the paddle.
- Tap to launch the ball when ready.
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

## Powerups
- **Multi-ball**: Adds two extra balls.
- **Laser**: Paddle fires beams.
- **Guardrail**: Temporary safety net.
- **Shield**: Absorbs a miss.
- **Extra Life**: +1 life.
- **Wide Paddle**: Expands paddle.
- **Slow**: Slows time briefly.
- **Fireball**: Ball pierces bricks.

## Build & Run (CLI only)
```bash
./gradlew assembleDebug
```

Install to device (USB debugging enabled):
```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.breakoutplus.debug/com.breakoutplus.SplashActivity
```

## Regenerate Audio
All audio is generated locally from `tools/generate_sfx.py`.
```bash
python3 tools/generate_sfx.py
```

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

## Icon Ideas
The current icon is a vector adaptive icon with brick and ball motifs. If you want alternates, easy variants:
- **Neon Orbit**: Dark background, cyan orbit ring, hot pink ball.
- **Folded Edge**: Split gradient background suggesting the hinge.
- **Minimal Plus**: Large “+” with a single brick row.

## Notes
- The HUD timer counts down in timed modes and counts up otherwise.
- Scores are stored locally in SharedPreferences.
- All assets are generated or defined locally—no placeholders or stubs.
