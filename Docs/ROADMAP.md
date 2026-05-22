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
- `VOLLEY`: continue late-run performance tuning after turn-reliability hardening.
- `TUNNEL`: continue long-session pacing/perf tuning after gate-integrity/readiness consistency fixes.
- `GOD`/`ZEN`: monitor progression stability after bounded auto-advance retry hardening.

### 2. Visual + UX Consistency
- Continue animation/effect balancing after renderer feedback normalization rollout.
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
### Phase A (Completed)
- Extract status/text formatting and mode HUD status composition.
- Remove duplicated mode/brick lookup logic.
- Continue collision and hot-loop micro-optimizations.
- Unify device-class policy across HUD and board tuning (`DeviceLayoutPolicy`) to reduce slate/fold divergence.
- Normalize renderer-side visual timing paths (Volley danger pulse/smoothing) to reduce frame-rate dependent drift.
- Centralize next-level acceptance guards (`LevelAdvancePolicy`) and harden GOD/ZEN auto/manual progression paths.
- Centralize gameplay VFX event profiles to reduce per-mode effect inconsistency.

### Phase B (In Progress)
- Extract collision subsystem (ball/brick/beam/paddle interactions).
- Extract mode-state subsystem (`VOLLEY`, `TUNNEL`, `INVADERS` specialty flows).
- Reduce `GameEngine.kt` responsibility surface while preserving APIs.

Completed in 1.0.11 hardening tranche:
- `InvadersModeSystem` extracted (formation offset, pacing, shot caps).
- `VolleyModeSystem` expanded (starting ball constants, `shouldAwardBall`).
- `ModeAccent` centralized for UI color consistency.

Current Phase B priorities:
- Continue hot-loop scan/allocation reductions in `GameEngine.update` paths.
- Keep single-pass counters/caches for per-tick mode checks where behavior can be preserved exactly.
- Add long-session stress validation for dense boards, multi-ball, and heavy FX states.
- Keep docs/release notes synchronized at the end of every patch tranche.

### Phase C (After)
- Extract effects/powerup lifecycle subsystem.
- Align render-facing effect state with explicit update contracts.

## Validation Gates (Per Change Set)
- GitHub Actions CI (automatic on push/PR): `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
- Local (JDK 17 required):
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
