# Roadmap

## Foundation (Completed)
- Android gameplay runtime implemented and shipping:
  - OpenGL ES renderer + fixed-step simulation
  - 10 modes, 10 brick types, 18 powerups
  - progression/unlocks/challenges/scoreboards/lifetime stats
  - fold/large-screen adaptation paths

## Current Release Objective (Android)
Deliver a stable, consistent, performant Android release with no feature removals and no mode regressions.

## Active Workstreams
### 1. Mode Stability + Identity
- `VOLLEY`: improve turn reliability, pressure readability, late-run performance.
- `TUNNEL`: strengthen gate/breach clarity and supply-drop pacing.
- `GOD`/`ZEN`: guarantee seamless level progression and auto-advance reliability.

### 2. Visual + UX Consistency
- Unify animation/effect behavior across game events and device classes.
- Keep HUD scaling and control placement consistent on phone/fold/slate.
- Preserve gameplay readability while increasing slate vertical density.

### 3. Performance + Reliability
- Reduce frame-time spikes in high-entity scenes.
- Remove avoidable allocations and repeated collection scans in hot paths.
- Validate long-session stability under heavy mode pressure.

### 4. Complexity Reduction (No Feature Cuts)
- Break large runtime files into dedicated systems.
- Keep gameplay behavior unchanged during extraction phases.
- Add test coverage with each extraction to protect behavior.

## Architecture Decomposition Plan
### Phase A (In Progress)
- Extract status/text formatting and mode HUD status composition.
- Remove duplicated mode/brick lookup logic.
- Continue collision and hot-loop micro-optimizations.

### Phase B (Next)
- Extract collision subsystem (ball/brick/beam/paddle interactions).
- Extract mode-state subsystem (`VOLLEY`, `TUNNEL`, `INVADERS` specialty flows).
- Reduce `GameEngine.kt` responsibility surface while preserving APIs.

### Phase C (After)
- Extract effects/powerup lifecycle subsystem.
- Align render-facing effect state with explicit update contracts.

## Validation Gates (Per Change Set)
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:lintDebug`
- `./gradlew :app:assembleDebug`
- Device smoke run across all modes (`tools/mode_smoke_test.sh`)
- Deterministic GOD/ZEN progression probe (`tools/god_zen_progression_probe.sh`)
- Deterministic all-modes progression probe (`tools/all_modes_progression_probe.sh`)

## Non-Negotiables
- No stubs, placeholders, fake behavior, or temporary feature bypasses.
- No feature removals.
- No intentional regressions.
