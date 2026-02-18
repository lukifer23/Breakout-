# Breakout+ Architecture

## Android Runtime Flow
1. `GameActivity` creates `GameConfig` (mode + settings + unlocks + challenge state).
2. `GameGLSurfaceView` initializes `GameRenderer`.
3. `GameGLSurfaceView.FramePacer` drives `requestRender()` via `Choreographer`.
4. `GameRenderer` performs fixed-step simulation and render.
5. `GameEngine` updates gameplay state and emits HUD/events through `GameEventListener`.

## Main Components
- UI layer: `app/src/main/java/com/breakoutplus/*.kt`
- Render surface: `GameGLSurfaceView.kt`
- Render/sim loop: `GameRenderer.kt`
- Core gameplay: `GameEngine.kt`
- Layout generation: `LevelFactory.kt`
- Mode tuning: `GameMode.kt`, `ModeBalance.kt`
- Mode status formatting: `ModeStatusText.kt`
- Tunnel mode pacing/supply logic: `TunnelModeSystem.kt`
- Powerup drop-rate model: `PowerupDropModel.kt`
- Drawing primitives: `Renderer2D.kt`
- Audio playback/feedback: `GameAudioManager.kt`

## Current Hotspots (Complexity)
- `GameEngine.kt` is the primary complexity concentration (gameplay, mode logic, collisions, FX, and status output mixed together).
- `GameActivity.kt` contains significant UI/hud/orchestration logic that needs continued cleanup.
- `LevelFactory.kt` is large and contains multiple generation strategies with high branching.

Current decomposition strategy is incremental extraction with behavior parity:
1. Extract pure status/formatting logic first.
2. Extract collision and mode-state systems next.
3. Extract powerup/effects lifecycle systems after that.

## Loop Details
- Render mode: `RENDERMODE_WHEN_DIRTY` (not continuous).
- Frame requests: Choreographer callback pacing in `FramePacer`.
- Simulation: fixed-step (`setTargetFrameRate` controls step size; clamped to 45-240 FPS bounds).
- Accumulator limit prevents runaway update bursts on frame drops.

## State & Events
- Core states: `READY`, `RUNNING`, `PAUSED`, `GAME_OVER`.
- `GameEngine` owns gameplay entities and progression.
- `GameEventListener` updates HUD, overlays, score/lives/time, mode-specific indicators.

## Data Persistence
- `SettingsManager`: user settings in SharedPreferences.
- `ScoreboardManager`: high scores by mode plus all-modes view.
- `DailyChallengeStore`: local challenge progress.
- `ProgressionManager` / `UnlockManager` / `LifetimeStatsManager`: progression and run stats.

## Foldable/Large-Screen Strategy
- Resource qualifiers for larger devices (`sw600dp`, `sw720dp`).
- `FoldAwareActivity` applies hinge/inset-aware layout padding.
- `GameActivity` applies responsive HUD scaling and reserved HUD height for varied aspect ratios.

## Engineering Rules For Refactor Work
- No feature removals.
- No placeholder systems or mock behavior paths.
- Keep mode identity intact while reducing class size and coupling.
- Every extraction must keep build/test/lint green.

## Performance Notes
- OpenGL ES 2.0 rendering path.
- Spatial hash used for brick collision broad-phase.
- Particle/wave caps limit FX overhead in high-action scenes.
- Single-frame timestamp usage keeps animation oscillators coherent.
