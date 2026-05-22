# Breakout+ Requirements

## Product Goals
- Ship a foldable-optimized Android brick breaker with stable performance and complete gameplay coverage.
- Maintain CLI-only build/test/deploy workflow.
- Preserve feature breadth with no mode regressions.
- Reduce implementation complexity in core runtime code without removing functionality.

## Target Platform (Current)
- Android (primary): minSdk 26, targetSdk 35
- Primary optimization target: Samsung Galaxy Z Fold class devices (folded + unfolded)

## Functional Requirements

### Core Gameplay
- 10 modes: `CLASSIC`, `TIMED`, `ENDLESS`, `GOD`, `RUSH`, `VOLLEY`, `TUNNEL`, `SURVIVAL`, `INVADERS`, `ZEN`
- 10 brick types with mode-appropriate behavior and HP scaling
- 18 powerups with visual feedback and timer/charge handling
- Accurate paddle/ball collision response with controllable launch aiming
- Level progression and mode-specific completion/failure conditions

### UI / UX
- Full screen flow: Main, Mode Select, Game, Settings, Scoreboard, How-To, Daily Challenges, Privacy
- Responsive HUD scaling across phone/tablet/foldable aspect ratios
- Reliable overlays for pause, level complete, game over, and high-score entry
- Left-handed control layout support
- Mode-readable telegraphing for high-pressure states (Volley breach lane, Tunnel gate/supply readiness)

### Persistence
- Local settings persistence
- Local scoreboards (per mode + all modes)
- Local progression/unlock/challenge/lifetime stats data
- Optional local debug log persistence when enabled

## Non-Functional Requirements

### Rendering & Performance
- OpenGL ES 2.0 renderer
- Choreographer-paced frame requests (`RENDERMODE_WHEN_DIRTY`)
- Fixed-step-only simulation in renderer loop for stable gameplay timing
- FX/object caps to prevent runaway perf degradation in heavy scenes
- Consistent gameplay event feedback profiles (shake/impact/combo/clear) across all modes

### Quality Constraints
- No feature removals for existing Android modes
- No intentional regressions in mode rules or progression flow
- Build/test/lint must remain green in CI-style CLI runs (GitHub Actions + local Gradle)
- No placeholder/stub/mock gameplay paths in production code
- Cross-platform parity tracked in [`PARITY.md`](PARITY.md); Android is authoritative

## Success Criteria
- `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:lintDebug` succeed (locally and in CI).
- Latest debug APK installs and launches on connected Android device.
- All 10 modes are playable end-to-end with expected mode identity.
- Folded/unfolded/slate form-factor matrix shows stable HUD scaling and readable gameplay surfaces.
- Android 1.0.11 hardening criteria met — see [`HARDENING_SIGNOFF.md`](HARDENING_SIGNOFF.md).
