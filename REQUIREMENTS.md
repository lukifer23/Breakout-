# Breakout+ Requirements

## Product Goals
- Ship a foldable-optimized Breakout/brickbreaker for Samsung Galaxy Z Fold 7.
- Deliver smooth 60 FPS gameplay with hardware-accelerated rendering.
- Provide multiple modes, levels, powerups, and brick variants with escalating difficulty.
- Include full UI/UX flow: splash, title, modes, gameplay, scoreboard, settings, how-to.
- Fully functional via CLI build and install (no Android Studio dependency).

## Target Device
- Samsung Galaxy Z Fold 7
- Android 14+ (app minSdk 26, targetSdk 35)
- Folded and unfolded layouts with hinge-aware padding.

## Functional Requirements
- Touch controls: drag to move paddle, tap to launch.
- Powerups with visible drops and HUD status.
- Multiple balls, laser, guardrail, shield, extra life, wide paddle, slow, fireball.
- Bricks: standard, reinforced, armored, explosive, unbreakable.
- Multiple game modes: Classic, Timed Challenge, Endless, God Mode, Level Rush.
- Scoreboard with persistent local storage.
- Settings: sound, music, vibration, tips, sensitivity, left-handed mode.
- How-to screen with tips and mechanics.
- Audio: sound effects + music loop.

## Non-Functional Requirements
- GPU-accelerated rendering via OpenGL ES 2.0.
- Stable delta-time update loop, capped to prevent large spikes.
- Foldable-aware layouts and dynamic padding to avoid hinge overlap.
- Minimum 60 FPS target on device.
- No stubs, placeholders, or mock assets in shipped content.

## Success Criteria
- Clean build from CLI: `./gradlew assembleDebug`.
- Installable APK that runs on Z Fold 7 via ADB.
- Smooth gameplay in both folded and unfolded states.
- Core gameplay loop and all modes are playable.
