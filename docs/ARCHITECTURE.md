# Runtime architecture

Android is the reference implementation. UI uses native Activities/XML, and
GLSurfaceView runs OpenGL ES 2.0 gameplay. This is an incremental extraction of
the existing game, not a new engine or KMP conversion.

## Ownership and flow

1. GameActivity loads immutable GameSettings/GameUnlocks into GameConfig, resolves
   the saved run identity and reward reservation, and owns native overlays/HUD.
2. AndroidInputAdapter converts MotionEvent into normalized PointerInput commands
   on the UI thread. GameGLSurfaceView queues those immutable values to GL.
3. Choreographer requests frames. FixedStepClock advances at 120 Hz independently
   of display FPS. Elapsed catch-up is bounded to 0.1 seconds / 12 updates.
4. GameEngine owns mutable gameplay entities, mode state, RNG, scoring and effects.
   GameEntities and GameInputSystem are extracted; collision, scoring, powerup and
   level-flow extension systems remain. Mode systems retain existing policies.
5. GameFeedbackQueue emits audio/haptics; GameVisualFeedback and GameDiagnostics
   are interfaces. Native audio/render/logger adapters consume those outputs.
6. EngineRenderer reads the same-thread simulation and submits ordered primitives.
   Renderer2D uploads reused position/color triangle streams to a dynamic VBO,
   flushes layers/capacity boundaries, and checks shader/program status.
7. Engine events update HUD and copy daily state. LocalDataWriter serializes
   background transactions and coalesces superseded progress/checkpoint writes.

The pure input/config/model/RNG seams do not require Context, MotionEvent,
SharedPreferences or View. GameEngine still coordinates many mode/physics/VFX
responsibilities (2,885 lines at this milestone); this is not a fully isolated
shared-core architecture. LevelFactory remains 1,498 lines and index-seeded;
its extraction and an explicit generation seed contract are unfinished.

## Persistence and lifecycle

Six legacy preference stores remain: settings, scores, XP/best level, unlocks,
lifetime stats and daily objectives. Daily schema 2 and reward ledger schema 1
have explicit semantics/migration. Active-run schema 1 captures authoritative
entities/timers/layout/mode/RNG/reward balances into atomic no-backup files.
Resume after recreation is paused. Native input, particles, trails, flashes,
audio position and GL handles are not restored. See [PERSISTENCE](PERSISTENCE.md)
for transaction boundaries, corruption handling and remaining cross-store gaps.

GameActivity continues to consume orientation/size/layout/density configuration
changes to preserve and relayout the live engine. FoldAwareActivity observes
WindowManager hinge state, and DeviceLayoutPolicy is shared by HUD/board tuning.
Removing manual configuration handling is deferred until the full resize and
recreation matrix is verified. Common menu insets and HUD contrast remain open
issues; their existence is not evidence of verified foldable behavior.

## Platform relationship and quality gates

The iOS SwiftUI/SpriteKit simulation is separate. It has known physics, mode,
daily reward and lifecycle gaps and no current XCTest gate. The macOS fork is
archived/experimental. No KMP migration is approved or implemented; assess it
only after clean core boundaries and deterministic parity fixtures exist.

[TESTING](TESTING.md), [PERFORMANCE](PERFORMANCE.md), [PARITY](PARITY.md) and
[BUILD](BUILD.md) define actual evidence and remaining qualification. Android CI
runs clean tests/lint/debug/minified APK/AAB checks; a Ruby job audits locked tools.
