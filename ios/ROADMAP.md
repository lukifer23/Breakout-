# iOS Roadmap (Current)

## Objective

Ship iOS gameplay quality at Android parity for core loops, then close visual and balancing gaps mode-by-mode.

**Gate:** Do not start iOS engine parity ports until Android 1.0.11 hardening is signed off and Play release path is underway. See [`Docs/HARDENING_SIGNOFF.md`](../Docs/HARDENING_SIGNOFF.md) and [`Docs/PARITY.md`](../Docs/PARITY.md).

## Current State

- CLI simulator workflow is working (`xcodebuild` + `simctl` + `run_ios_sim.sh`).
- All 10 mode types are present in UI and level generation.
- Core systems are in place (scoreboard, progression, powerups, settings).
- Engine depth lags Android — see parity matrix for subsystem gaps.
- `BreakoutPlusMac` is frozen; do not extend.

## Priority Backlog (After Android Sign-Off)

### 1) Engine Parity (Highest — Full Ports, No Stubs)

Port from Android reference implementation:
- `TunnelModeSystem` (supply readiness, pity drops, gate telemetry)
- `VolleyModeSystem` (stall detection, nudge, turn resolution)
- Spatial hash + ball sub-stepping in collision
- `LevelAdvancePolicy` for GOD/ZEN flow
- `ModeStatusText` HUD row
- Magnet ball catch
- Daily challenge reward application

### 2) Gameplay Feel and Balance

- Tune launch/velocity curves per mode (especially Volley and Tunnel) on real touch hardware.
- Normalize difficulty ramps so level completion targets feel fair and intentional.
- Verify no mode regresses into generic layout/rules after generation refactors.

### 3) Visual and FX Polish

- Upgrade brick destruction readability and impact feel.
- Improve collision feedback consistency across themes and modes.
- Keep powerup visuals readable without obscuring playfield.
- Add SwiftUI design token file matching Android `ModeAccent` / `colors.xml`.

### 4) Input and UX Reliability

- Validate continuous aiming behavior at center and edge cases.
- Keep HUD non-blocking and out of paddle control lanes.
- Cross-check simulator vs device gesture behavior before locking control tweaks.
- Fix Settings `@AppStorage` duplication (single source of truth with `GameViewModel`).
- Add missing settings: dark mode, FPS counter, high refresh toggle.

### 5) Release Readiness

- Keep docs synced to implemented behavior (`PARITY.md`).
- Run manual regression checklist after mode/balance changes.
- Prepare stable iOS release branch only after engine parity pass complete.

## Deferred for Now

- Large-scale automated gameplay testing expansion.
- App Store/TestFlight packaging workflow finalization.
- Non-critical platform extras (IAP, cloud sync, Game Center achievements).
- BreakoutPlusMac maintenance or parity.
