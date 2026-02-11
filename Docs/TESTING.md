# Testing

## Unit Tests
```bash
./gradlew test
```

## Automated Mode Smoke Test (Device)
Runs each gameplay mode on a connected Android device, verifies the activity launches, then checks logcat for fatal crashes.
```bash
tools/mode_smoke_test.sh
```
Requires at least one `adb`-visible device or emulator.
Optional env vars:
- `BP_SERIAL=<adb-serial>` to target a specific device.
- `BP_GAME_MODES="CLASSIC RUSH VOLLEY"` to limit modes.
- `BP_MODE_WAIT=6` to wait longer before log checks.
- `BP_AUTO_PLAY=1` to enable debug autoplay during each mode probe.
- `BP_AUTO_PLAY_SECONDS=20` to cap autoplay runtime per launch.
When autoplay is enabled, look for `BreakoutAutoPlay` log entries (`session_start`, `level_complete`, `game_over`) for viability/balance signals.

## Manual Test Checklist
- Launch app, confirm system splash, open each menu (Main, Modes, Scoreboard, Settings, How-To).
- Start every mode (Classic, Timed, Endless, God, Rush, Volley, Survival, Invaders): break bricks, collect powerups, pause, level complete, game over.
- Start Invaders mode: verify enemy shots, shield depletion, hit feedback, and laser usage.
- Confirm touch controls: drag paddle, launch ball, two-finger laser + FIRE button.
- Confirm aim accuracy: while dragging, the aim guide should match the actual launch trajectory and first bounce.
- Verify HUD: score/lives/time/level stay aligned on one line; no shifts when banners/powerups appear; powerup status and combo update inline.
- Verify overlays and critical controls: no tip popup blocks paddle/ball path; laser button stays near top HUD on phone and tablet layouts.
- Verify level flow: after Level Complete, tapping **Next Level** always advances to gameplay (never mode select/main menu).
- Verify negative powerups: Shrink reduces paddle size; Overdrive speeds gameplay for its duration.
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
