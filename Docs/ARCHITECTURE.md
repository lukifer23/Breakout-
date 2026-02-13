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
- Drawing primitives: `Renderer2D.kt`
- Audio playback/feedback: `GameAudioManager.kt`

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

## Performance Notes
- OpenGL ES 2.0 rendering path.
- Spatial hash used for brick collision broad-phase.
- Particle/wave caps limit FX overhead in high-action scenes.
- Single-frame timestamp usage keeps animation oscillators coherent.
