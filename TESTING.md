# Testing

## Unit Tests
```bash
./gradlew test
```

## Manual Test Checklist
- Launch app, complete splash, open each menu (Main, Modes, Scoreboard, Settings, How-To).
- Start Classic and Rush modes: break bricks, collect powerups, pause, level complete, game over.
- Confirm touch controls: drag paddle, launch ball, two-finger laser.
- Verify HUD: score, lives, time, level, powerup status with combo.
- Test all screens: mode selection, scoreboard display, settings toggles (sound/music/vibration/tips/left-handed/dark mode), how-to expandable sections.
- Foldable (if device available): test folded and unfolded states, confirm layouts and hinge padding.
- No placeholders or stub UI elements.

## Performance Targets
- 60 FPS or higher during gameplay on Z Fold 7.
- No frame spikes during multi-ball or heavy explosions.

## Build Verification
```bash
./gradlew assembleDebug
```

## Unit Tests (JVM)
```bash
./gradlew testDebugUnitTest
```

## What We Cover
- Level patterns include advanced brick types (moving/spawning/phase/boss).
- Difficulty scaling increases hit points.
- Mode launch speeds are positive and balanced.
