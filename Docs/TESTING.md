# Testing

## JVM Unit Tests
```bash
./gradlew :app:testDebugUnitTest
```

## Build/Lint Verification
```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

## Device Smoke Test (All Modes)
```bash
tools/mode_smoke_test.sh
```
Requires at least one `adb`-visible device/emulator.

Optional env vars:
- `BP_SERIAL=<adb-serial>`
- `BP_GAME_MODES="CLASSIC TIMED ENDLESS GOD RUSH VOLLEY TUNNEL SURVIVAL INVADERS ZEN"`
- `BP_MODE_WAIT=6`
- `BP_AUTO_PLAY=1`
- `BP_AUTO_PLAY_SECONDS=20`

## Manual Gameplay Checklist
- Launch app and verify Main, Mode Select, Scoreboard, Settings, How-To, Daily Challenges, Privacy screens.
- Start every mode: `CLASSIC`, `TIMED`, `ENDLESS`, `GOD`, `RUSH`, `VOLLEY`, `TUNNEL`, `SURVIVAL`, `INVADERS`, `ZEN`.
- Run viewport matrix checks for every mode:
  - Folded portrait phone.
  - Unfolded/tablet portrait.
  - Unfolded/tablet landscape.
- Verify level flow:
  - Level-complete overlay advances correctly in normal modes.
  - `GOD` and `ZEN` auto-advance/continue without blocking progression.
- Verify Volley behavior:
  - Turn launch queue, row descent, return anchor reposition, breach game-over.
- Verify Tunnel behavior:
  - Fortress ring remains identifiable, gate lane stays open, interior density remains high in later levels.
- Verify Invaders behavior:
  - Enemy shot telegraph/firing, shield hit/break feedback, paddle survival flow.
- Verify HUD behavior:
  - Responsive scaling across phone/tablet/foldable sizes.
  - No overlaps between score/meta/powerup chips/FPS/laser button.
  - HUD reserve height adapts per viewport without crowding gameplay surface.
- Verify controls:
  - Drag tracking and launch alignment with aim guide.
  - Laser button cooldown and visibility states.
- Verify pause/resume/restart and game-over flows.

## Performance Targets
- Stable 60+ FPS class behavior on target hardware.
- No major frame spikes during multi-ball, heavy FX, or large enemy volleys.
