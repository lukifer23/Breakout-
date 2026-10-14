# October 2026 modernization evidence

Work starts from clean remote clone c1be9b6 (Android 1.0.12 / code 12),
default branch main, on hardening/2026-10-modernization. The preexisting
checkout was clean and was preserved during implementation; a clean clone was
created in its Breakout- subdirectory. At the stopping point the original checkout
is fast-forwarded to remote main and the redundant task clone is removed.
No Git history rewrite is authorized or performed.

## Baseline inventory

5,325 tracked files, 640,032,774 bytes of tracked payload.
4,921 reproducible output files occupy 533,377,105 bytes: ios/.build,
ios/BreakoutPlusMac/.build, ios/test_ios_binary, an Xcode project backup,
and five stale store_assets/releases APKs. Initial Git pack is 176.66 MiB
in this new clone; removing current files does not remove historical objects.
GameEngine.kt: 3,467 lines; LevelFactory.kt: 1,498 lines.

## Runtime ownership map

- Activity/UI: GameActivity creates GameConfig and translates engine callbacks
  to GameHudController, progression/stats writes, overlays and dialogs.
- Input: GLSurfaceView copies MotionEvent to GL thread; engine tracks pointers,
  launch/aim/drag and laser. Android input is currently coupled to the core.
- Simulation: GL-thread GameEngine owns all entities, timers, score, combo,
  effects, mode state, RNG, challenge mutation and renderer feedback. Extracted
  extension files hold collision, scoring, aim, powerup and level-flow behavior.
- Loop: Choreographer requests frames; GameRenderer accumulates fixed ticks,
  but requested display FPS changes tick size and overload drops time.
- Rendering: EngineRenderer reads mutable engine state on the same GL thread.
  Renderer2D uses GLES2 client arrays. Batch APIs pool object records but flush
  per-object uniforms/matrices/draw calls. Shader compile/link status unchecked.
- Levels: LevelFactory contains authored grids, procedural templates, difficulty
  transforms, special brick assignment and Invaders/Tunnel generators.
- Modes: Classic/Endless use board clearing; Timed uses a session timer; Rush
  resets timer per level; God permits skip/guardrail; Zen removes scoring/XP;
  Survival accelerates; Volley handles queued launch/returns/descending rows;
  Tunnel tracks gate/supply/pity; Invaders controls formation/shots/shields.
- Persistence: six SharedPreferences stores: settings, scores (JSON), XP/best
  level, themes/cosmetic tier, lifetime stats, daily date+challenge JSON.
  No authoritative active-run snapshot or general schema boundary exists.
- Daily: three templates selected using millisecond seed; shared date formatter;
  generic accumulation mishandles combos. Granted flag precedes durable effect.
- Audio: SoundPool (6 streams), MediaPlayer and focus request. No collision
  sound rate gate; focus gain does not restore ducked volume in every path.
- UI/layout: XML phone/sw600dp/sw720dp resources, FoldAwareActivity observes
  WindowInfoTracker; GameActivity consumes size/orientation/density changes
  to preserve GL simulation and relayout bricks. Menus lack common inset handling.
- iOS: SwiftUI/SpriteKit port, separate Swift simulation/UserDefaults stores;
  daily rewards print messages and mark claimed. Xcode scheme has no testables.
  macOS fork is frozen and contains stale five-mode implementation.
- Tests: JVM JUnit suite for mode/layout/collision/brick/level helpers.
  No Android instrumentation directory; no real XCTest target at baseline.
- Release/CI: Android workflow tests/lints/builds debug and release; CI=true
  makes ordinary release artifacts debug signed. Fastlane before_all requires
  Play credentials even for local bundle build; version defaults are duplicated.

## Versions and local environment

Baseline: minSdk 26; compile/target 35; AGP 8.6.1; Kotlin 2.0.21; Gradle 8.9;
Java target 17. AndroidX core 1.13.1, appcompat 1.7.1, activity 1.9.3,
window 1.5.1, lifecycle 2.10.0, splashscreen 1.0.1, constraintlayout 2.2.1,
Material 1.13.0, JUnit 4.13.2. Ruby lock: Fastlane 2.232.0, json 2.18.1,
addressable 2.8.8, jwt 2.10.2, faraday 1.10.4. Open Dependabot PRs #4/#5/#6
cover json/addressable/jwt; inspect graph and audit instead of merging old PRs.

Host: Apple Silicon macOS, Temurin JDK 17.0.20.1 selected explicitly.
SDK platforms 34/35/36/37.0 and tools 35/36 available. Swift 6.4 command-line
tools available, full Xcode absent (xcodebuild -version fails); no iOS simulator
build/test evidence can be claimed here. Android emulator-5556 is available.
A physical Fold is attached; it has not been modified or tested yet.

## Command evidence

Clone, fetch all/tags/prune, git status and log -20 completed; clean main.
./gradlew clean succeeded in 39s. All baseline commands completed before architectural changes; see results below. Raw local logs live in
/tmp/breakout-modernization-evidence (not source artifacts).

## Hygiene result

Removed 4,921 reproducible outputs and eight zero-byte PNG captures. Existing
authored source/assets remain. Tracked-file count falls from 5,325 to 396 before
adding this evidence and sanity script. Reproducible-output payload reduced by
533,377,105 bytes (about 509 MiB); Git history remains about 177 MiB. Local
Gradle outputs subsequently add ignored working-tree data, so du of the entire
checkout is not a like-for-like source metric. Historical pack bloat requires
a separately authorized history rewrite; no rewrite is needed for this pass.

Baseline completed: 111 tests, zero failures; lint 51 warnings/no errors.
- testDebugUnitTest: exit 0, 42.41 seconds.
- lintDebug: exit 0, 33.66 seconds.
- assembleDebug: exit 0, 11.29 seconds.
- assembleRelease: exit 0, 37.11 seconds.
- bundleRelease: exit 0, 2.12 seconds.
Release tasks used the preexisting CI=true debug-signing fallback, not production signing.

## Core extraction qualification

Commit 0eac495 was exported from the Git index into an independent temporary
source tree and passed testDebugUnitTest, lintDebug and assembleDebug. Its 115
unit tests passed. GameEngine now consumes immutable settings/unlocks, neutral
input commands, a feedback queue and a diagnostic interface. Entities and input
control live in separate files. Simulation ticks use 120 Hz regardless of display
refresh. Gameplay and visual RNG streams are separate and seeded/versioned.
Current GameEngine length is 2,885 lines, versus 3,467 at baseline; responsibility
reduction is incremental and mode coordination remains in the engine.

## Stopping-point cleanup totals

After current docs/scripts and removal of timestamp/mislabeled screenshot output,
the source index has approximately 375 files / 11,626,340 bytes before final
evidence edits, compared with 5,325 / 640,032,774 bytes initially. About 628 MB
of reproducible/stale tracked payload is gone; authored code/assets remain.
Historical objects still occupy roughly the original 177 MiB Git pack; ignored
local builds add working-tree disk usage. No history rewrite/force push occurred.

Latest lint: 73 warnings / zero errors; see TESTING.md for categories and limits.

## Final milestone gates

Clean testDebugUnitTest/lintDebug/assembleDebug/assembleReleaseCheck/bundleReleaseCheck
passed together locally (97s), and again from an independent remote shallow clone
(37s with warm dependency caches). 139 tests passed; 73 lint warnings, no errors.
GitHub push CI 37388038939 and PR CI 37388128032 passed both Android and Ruby
jobs. Signing-negative test rejected CI=true bundleRelease without BP_RELEASE_*;
apksigner verified compile-check uses CN=Android Debug. Store validation accepted
a real capture and rejected duplicate, zero-byte and truncated images; strict
validation intentionally fails because the canonical capture set is missing.

Merged PR #7 into main at b465390. Security alerts all fixed; superseded branches
closed/deleted. The full modernization pauses here at the user's request, with
remaining tasks recorded in ROADMAP.md. No Play/TestFlight upload, history rewrite
or claim of full device/iOS qualification is made.
