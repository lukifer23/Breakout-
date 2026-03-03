# Release Notes

## 1.0.11 (Unreleased)
- Made gameplay simulation deterministic in `GameRenderer` by removing variable-step fallback updates when no fixed tick is due.
- Hardened next-level transition handling in `GameActivity` with a shared request/recovery path for both manual and auto advance flows.
- Expanded GOD next-level acceptance to support READY-state manual skip and cleared-board recovery through centralized `LevelAdvancePolicy`.
- Added `LevelAdvancePolicyTest` unit coverage to lock progression gate behavior and prevent regression.
- Updated testing guidance with explicit multi-device `adb` targeting for progression probes (`BP_SERIAL`).
- Tuned `ModeLayoutPolicy` to increase Volley vertical density on slate profiles with smoother per-level progression.
- Unified Volley danger pressure with status pressure inputs to reduce visual-rule drift.
- Consolidated Tunnel board status/integrity calculations into single-pass `ModeBoardMetrics.tunnelBoardMetrics`.
- Rebalanced slate/fold HUD reserved-height clamps in `GameActivity` to reduce over-allocation and keep more gameplay space visible.
- Added `VolleyModeSystem.isBallInFlight` thresholding to avoid turn-resolution stalls caused by near-zero motion jitter.
- Added Tunnel supply readiness telemetry in HUD status and introduced pity-drop forcing in `TunnelModeSystem` to avoid long supply droughts.
- Expanded mode regression coverage for Tunnel supply decision/readiness and Volley in-flight classification.
- Added persistent Tunnel gate-lane telegraph rendering and gate readiness fill cues for clearer Siege-state readability.
- Added stronger visual feedback (impact flash + shake) when pity-drop forcing triggers after sustained Tunnel pressure.
- Unified gameplay event VFX through centralized visual feedback profiles in `GameEngine` (shake/impact/combo/level-clear consistency across modes).
- Reduced hot-loop scan pressure by replacing per-tick breakable completion scans with a maintained alive-breakable counter.
- Reduced Volley turn-resolution overhead by switching stuck/in-flight ball state detection to a single-pass count.
- Removed READY-state per-frame explosive-brick scans by maintaining an alive-explosive counter for tip gating.
- Extracted collision combo/audio feedback math into `BrickCollisionFeedback` and added focused unit coverage to preserve score multiplier and dynamic pitch behavior.
- Hardened progression probes for wireless-device stability with serial auto-resolution, device-ready waits, and per-mode retries (`BP_PROGRESSION_MODE_ATTEMPTS`).
- Reduced repeated stuck-ball scans in `GameEngine` by centralizing paddle-attached ball detection in a helper used by input, aim-guide, and autoplay paths.
- Removed duplicated Tunnel breakthrough-state scans by centralizing active/queued breakthrough checks for HUD status and supply-drop gating.

## 1.0.10 (2026-02-27)
- Added shared `DeviceLayoutPolicy` to unify slate/foldable classification used by both HUD sizing (`GameActivity`) and gameplay board tuning (`GameEngine`).
- Added `ModeLayoutPolicy` and moved Volley row-boost selection into a centralized helper.
- Increased Volley vertical brick density on slate/tablet profiles while preserving tall-phone compaction behavior.
- Stabilized Volley danger overlay timing by moving pulse animation to frame-accumulated visual time instead of wall-clock sampling.
- Normalized Volley danger smoothing to use delta-time response, reducing device/frame-rate dependent intensity drift.
- Hardened GOD-mode next-level recovery path so cleared-board progression can advance even when `awaitingNextLevel` desynchronizes.
- Added unit coverage for shared device layout classification and aspect normalization.

## 1.0.9 (2026-02-26)
- Fixed GOD-mode manual skip and GOD/ZEN auto-advance interaction to avoid unintended multi-level jumps under recovery timing.
- Restored GOD skip-level control parity on larger layouts (`sw600dp`, `sw720dp`) so tablet/slate pause UI matches phone behavior.
- Unified slate detection between HUD and gameplay board tuning to improve foldable/slate consistency in density behavior.
- Hardened release build signing safety: release tasks now fail fast when `BP_RELEASE_*` signing vars are missing.
- Aligned Tunnel mode accent color between mode selection and scoreboard surfaces.
- Unified Volley danger-overlay thresholds with board-pressure metrics to remove viewport-dependent danger FX inconsistency.
- Fixed PHASE brick transitions so intermediate phases cannot remain alive at `0` HP.
- Reduced render-loop allocation churn by reusing flash/overlay color buffers in `GameRenderer`.
- Added regression coverage for PHASE brick transition HP behavior.

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
