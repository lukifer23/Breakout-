# iOS Roadmap (Current)

## Objective

Ship iOS gameplay quality at Android parity for core loops, then close visual and balancing gaps mode-by-mode.

## Current State

- CLI simulator workflow is working (`xcodebuild` + `simctl` + `run_ios_sim.sh`).
- All core mode types are present, including Invaders, Tunnel Siege, and Zen.
- Core systems are in place (scoreboard, progression, powerups, settings).
- Remaining work is quality, consistency, and tuning rather than missing-mode scaffolding.

## Priority Backlog

## 1) Gameplay Feel and Balance (Highest)

- Tune launch/velocity curves per mode (especially Volley and Tunnel) on real touch hardware.
- Normalize difficulty ramps so level completion targets feel fair and intentional.
- Verify no mode regresses into generic layout/rules after generation refactors.

## 2) Visual and FX Polish

- Upgrade brick destruction readability and impact feel.
- Improve collision feedback consistency across themes and modes.
- Keep powerup visuals readable without obscuring playfield.

## 3) Input and UX Reliability

- Validate continuous aiming behavior at center and edge cases.
- Keep HUD non-blocking and out of paddle control lanes.
- Cross-check simulator vs device gesture behavior before locking control tweaks.

## 4) Mode-Specific Hardening

- Tunnel Siege: preserve fortress wall identity and gate routing each run.
- Invaders: preserve smooth fleet motion and prevent repetitive collision/audio chatter.
- Volley: preserve descending-row logic and fully breakable intended brick sets.
- Zen: keep low-pressure identity (minimal HUD, no score/life pressure).

## 5) Release Readiness

- Keep docs synced to implemented behavior.
- Run manual regression checklist after mode/balance changes.
- Prepare stable iOS release branch only after mode tuning and visual polish pass complete.

## Deferred for Now

- Large-scale automated gameplay testing expansion.
- App Store/TestFlight packaging workflow finalization.
- Non-critical platform extras (IAP, cloud sync, Game Center achievements).
