# Testing

## Unit Tests
```bash
./gradlew test
```

## Manual Test Checklist
- Launch app, confirm system splash, open each menu (Main, Modes, Scoreboard, Settings, How-To).
- Start Classic and Rush modes: break bricks, collect powerups, pause, level complete, game over.
- Start Invaders mode: verify enemy shots, shield depletion, hit feedback, and laser usage.
- Confirm touch controls: drag paddle, launch ball, two-finger laser + FIRE button.
- Verify HUD: score/lives/time/level stay aligned on one line; no shifts when banners/powerups appear; powerup status and combo update inline.
- Test all screens: mode selection, scoreboard display, settings toggles (sound/music/vibration/tips/left-handed/dark mode), how-to expandable sections, Daily Challenges, Privacy Policy.
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
