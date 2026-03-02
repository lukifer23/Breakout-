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
- `BP_DEBUG_PROGRESSION_PROBE=1` (debug-only deterministic board-clear assist for GOD/ZEN progression checks)

## Deterministic GOD/ZEN Progression Probe
```bash
tools/god_zen_progression_probe.sh
```

This verifies on-device that GOD/ZEN can complete a level and auto-advance to at least level 2 without fallback recovery.
If `adb` reports `more than one device/emulator`, set `BP_SERIAL` explicitly.

Optional env vars:
- `BP_SERIAL=<adb-serial>`
- `BP_PROGRESSION_MODES="GOD ZEN"`
- `BP_PROGRESSION_RUN_SECONDS=35` (minimum effective value is 20)
- `BP_PROGRESSION_WAIT_PAD_SECONDS=8`

## Deterministic All-Modes Progression Probe
```bash
tools/all_modes_progression_probe.sh
```

This wrapper forwards to `tools/god_zen_progression_probe.sh` and broadens `BP_PROGRESSION_MODES` by default.

Optional env vars:
- `BP_SERIAL=<adb-serial>`
- `BP_PROGRESSION_MODES="CLASSIC TIMED ENDLESS GOD RUSH VOLLEY TUNNEL SURVIVAL INVADERS ZEN"`
- `BP_PROGRESSION_RUN_SECONDS=30` (minimum effective value is 20)
- `BP_PROGRESSION_WAIT_PAD_SECONDS=8`

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
  - No false turn stalls from near-zero velocity jitter after balls settle.
- Verify Tunnel behavior:
  - Fortress ring remains identifiable, gate lane stays open, interior density remains high in later levels.
  - Supply readiness indicator progresses and reaches `Supply READY` under sustained shots.
  - Pity-drop path prevents prolonged supply starvation in extended pressure phases.
- Verify Invaders behavior:
  - Enemy shot telegraph/firing, shield hit/break feedback, paddle survival flow.
- Verify HUD behavior:
  - Responsive scaling across phone/tablet/foldable sizes.
  - No overlaps between score/meta/powerup chips/FPS/laser button.
  - HUD reserve height adapts per viewport without crowding gameplay surface.
- Verify gameplay VFX consistency:
  - Shake/impact/combo/level-clear responses feel consistent across `VOLLEY`, `TUNNEL`, `INVADERS`, and `CLASSIC`.
  - Tunnel pity-drop and supply-ready moments emit clear, non-jarring feedback.
- Verify controls:
  - Drag tracking and launch alignment with aim guide.
  - Laser button cooldown and visibility states.
- Verify pause/resume/restart and game-over flows.

## Performance Targets
- Stable 60+ FPS class behavior on target hardware.
- No major frame spikes during multi-ball, heavy FX, or large enemy volleys.
