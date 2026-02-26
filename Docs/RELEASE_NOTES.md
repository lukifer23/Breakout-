# Release Notes

## 1.0.9 (2026-02-26)
- Fixed GOD-mode manual skip and GOD/ZEN auto-advance interaction to avoid unintended multi-level jumps under recovery timing.
- Restored GOD skip-level control parity on larger layouts (`sw600dp`, `sw720dp`) so tablet/slate pause UI matches phone behavior.
- Unified slate detection between HUD and gameplay board tuning to improve foldable/slate consistency in density behavior.
- Hardened release build signing safety: release tasks now fail fast when `BP_RELEASE_*` signing vars are missing.
- Aligned Tunnel mode accent color between mode selection and scoreboard surfaces.

## 1.0.8 (2026-02-13)
- Increased board density and vertical coverage tuning for foldable portrait and tablet/slate viewports across all gameplay modes.
- Reduced oversized HUD reservation on larger devices so gameplay gets more vertical space.
- Unified relayout behavior so row-boost padding stays consistent after viewport changes.
- Fixed Survival HUD timing display to avoid synthetic speed text drift and keep mode telemetry consistent.
- Updated testing guidance with explicit folded/unfolded viewport matrix checks for every mode.

## 1.0.7 (2026-02-13)
- Unified mode status handling for Volley, Tunnel, Survival, and Zen.
- Added missing How-To entries for Invader bricks and Zen mode.
- Added Zen fallback theme mapping and small audio-path safety cleanup.
