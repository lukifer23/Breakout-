# Testing

## Prerequisites
- **JDK 17** — set `JAVA_HOME` before running Gradle (see [`BUILD.md`](BUILD.md))
- Android SDK platform 35 for assemble/lint tasks

## CI (Automated)
GitHub Actions (`.github/workflows/android.yml`) runs on every push/PR:
```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
CI=true ./gradlew :app:assembleRelease
```

## JVM Unit Tests
```bash
./gradlew :app:testDebugUnitTest
```
Current suite: **108+ tests** covering mode systems, layout policy, collision math, progression policy, Zen behavior, and UI tokens.

Key test files added/expanded in 1.0.12:
- `ZenModeBehaviorTest` — Zen vs God flags, challenge mode mapping
- `LayoutParityTest` — overlay glass panels, shield track parity across buckets

Key test files added/expanded in 1.0.11:
- `VolleyModeSystemTest` — turn decision flow, starting ball count (5)
- `TunnelModeSystemTest` — supply gates, pity drops, readiness
- `InvadersModeSystemTest` — formation offset, pacing, shot caps
- `ModeAccentTest` — distinct Tunnel vs Survival accents
- `LayoutParityTest` — slate row-density parity, GOD skip control across layout buckets

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

Serial selection note:
- Use `adb devices -l` and copy the serial exactly; wireless targets may use full IDs like `adb-<device>._adb-tls-connect._tcp`.

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
- `BP_PROGRESSION_MODE_ATTEMPTS=2` (per-mode retry count for flaky wireless `adb` sessions)

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
- `BP_PROGRESSION_MODE_ATTEMPTS=2`

## Manual Gameplay Checklist
- Launch app and verify Main, Mode Select, Scoreboard, Settings, How-To, Daily Challenges, Privacy screens.
- Start every mode: `CLASSIC`, `TIMED`, `ENDLESS`, `GOD`, `RUSH`, `VOLLEY`, `TUNNEL`, `SURVIVAL`, `INVADERS`, `ZEN`.
- Run viewport matrix checks for every mode:
  - Folded portrait phone.
  - Unfolded/tablet portrait.
  - Unfolded/tablet landscape.
- Verify level flow:
  - Level-complete overlay advances correctly in normal modes.
  - `GOD` and `ZEN` auto-advance/continue without blocking progression; transient handoff misses should retry once before manual fallback UI appears.
  - `ZEN` must **not** show level-complete overlay (silent auto-advance only).
- Verify Volley behavior:
  - Starts with **5 balls**; can grow to 20.
  - Turn launch queue, row descent, return anchor reposition, breach game-over.
  - No turn desync from stalled non-moving balls after queue drains (balls should be nudged back in-flight, then resolve normally).
  - Slate/tablet layouts maintain denser vertical rows than fold/phone profiles without clipping HUD elements.
- Verify Tunnel behavior:
  - Fortress ring remains identifiable, gate lane stays open, interior density remains high in later levels.
  - Gate integrity % reacts to partial gate damage immediately (no stale HUD/cache values between hits).
  - Supply readiness indicator progresses and reaches `Supply READY` under sustained shots.
  - Pity-drop path prevents prolonged supply starvation in extended pressure phases.
- Verify Invaders behavior:
  - Enemy shot telegraph/firing, shield hit/break feedback, paddle survival flow.
- Verify mode accent colors on Mode Select and Scoreboard:
  - Tunnel (`bp_orange`) and Survival (`bp_flame`) are visually distinct.
- Verify HUD behavior:
  - Responsive scaling across phone/tablet/foldable sizes.
  - No overlaps between score/meta/powerup chips/FPS/laser button.
  - HUD reserve height adapts per viewport without crowding gameplay surface.
  - Tablet/slate controls and text do not appear oversized relative to gameplay area after orientation changes.
- Verify gameplay VFX consistency:
  - Shake/impact/combo/level-clear responses feel consistent across `VOLLEY`, `TUNNEL`, `INVADERS`, and `CLASSIC`.
  - Burst-heavy moments (explosions, boss breaks, chain clears) avoid sudden full-screen whiteout spikes while retaining clear feedback intensity differences.
  - Tunnel pity-drop and supply-ready moments emit clear, non-jarring feedback.
- Verify controls:
  - Drag tracking and launch alignment with aim guide.
  - Laser button cooldown and visibility states.
- Verify pause/resume/restart and game-over flows.

## Performance Targets
- Stable 60+ FPS class behavior on target hardware (120 Hz on supported foldables when high refresh enabled).
- No major frame spikes during multi-ball, heavy FX, or large enemy volleys.

## Sign-Off Reference
Android 1.0.11 hardening automated gates: [`HARDENING_SIGNOFF.md`](HARDENING_SIGNOFF.md)
