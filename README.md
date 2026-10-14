# Breakout+

An Android-first, offline brick breaker with ten modes, eighteen powerups, ten
brick types, local progression/unlocks, scoreboards, daily challenges and lifetime
stats. Existing gameplay and native platform implementations are preserved.

Android is the reference platform. The current release line remains **1.0.12
(version code 12)**, defined in [version.properties](version.properties).
The October 2026 maintenance changes are not a newly published store release.
iOS is a substantial SwiftUI/SpriteKit port with known parity gaps; the macOS
fork is archived/experimental.

## Current maintenance status — October 5, 2026

- Android compile/target API 36; minimum API 26; stable AGP 8.10.1,
  Gradle 8.11.1, Kotlin 2.2.21 and JDK 17.
- Seeded gameplay RNG, independent visual RNG and fixed 120 Hz simulation.
- Platform-neutral input/settings/unlocks and queued audio/visual/diagnostic seams.
- Correct daily progress semantics, date-based generation, durable reward receipts
  and consumables reserved for the next scored run.
- Atomic active-run checkpoints, restored paused after Activity/process recreation.
- Real GLES batching and bounded background diagnostic writes.
- 139 JVM tests passed locally; lint has no errors but warnings remain.
- Android debug and distinct minified compile-check APK/AAB paths; publishing
  requires actual release signing. Local builds need no Play credentials.
- Locked Ruby tools updated and checked against the advisory database.

[Baseline](docs/MODERNIZATION_BASELINE.md), [persistence limits](docs/PERSISTENCE.md)
and [measured performance](docs/PERFORMANCE.md) contain the evidence and boundaries.
API 36 emulator checks cover mode launch, six stress cases and one process-recovery
case. They do not establish full device/foldable QA or iOS parity.

## Build

Install JDK 17, Android SDK platform 36 and build-tools 35.0.0, then:

```sh
gh repo clone lukifer23/Breakout- Breakout-
cd Breakout-
export JAVA_HOME=/absolute/path/to/jdk-17
export ANDROID_HOME=/absolute/path/to/android-sdk
./gradlew clean testDebugUnitTest lintDebug assembleDebug
./gradlew assembleReleaseCheck bundleReleaseCheck
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <serial> shell am start -n com.breakoutplus.debug/com.breakoutplus.MainActivity
```

Compile-check artifacts have a `.compilecheck` package and `-compilecheck` version
suffix, use a debug key, and are never upload candidates. See [BUILD](docs/BUILD.md)
for real signing and Fastlane procedures.

## Gameplay and architecture

Modes: Classic, Timed, Endless, God, Rush, Volley, Tunnel, Survival, Invaders and
Zen. The existing OpenGL ES 2.0 gameplay surface is driven by Choreographer.
The GL thread owns simulation; Android input becomes neutral commands, and UI
callbacks/persistence consume copies. [ARCHITECTURE](docs/ARCHITECTURE.md) describes
the boundaries; [GAMEPLAY](docs/GAMEPLAY.md) defines the rules.

```text
app/           Android app and JVM tests
docs/          Current authoritative docs; Archive/ contains historical records
ios/           iOS port; BreakoutPlusMac/ is archived, Archive/ is historical
fastlane/      Ruby release tooling and Play metadata
store_assets/  Canonical icon/feature assets; verified screenshots still pending
tools/         Repository, release, stress and device utilities
```

## Release readiness and remaining work

This is a tested maintenance milestone, not a declaration that the full
modernization or Play release is complete. Remaining priorities include HUD
contrast, versioning/corruption handling for all older preference stores,
cross-store crash side effects, physics and lifecycle/device qualification,
LevelFactory extraction, accessibility/audio review, curated Challenge Journey,
iOS XCTest/parity, and verified phone/large-screen store captures.

Stale timestamp captures and mislabeled duplicate screenshots were removed.
No screenshot is advertised here until its current screen has been captured and
validated. Publishing remains gated on a complete screenshot set and operator
review. See [ROADMAP](docs/ROADMAP.md) and [RELEASE_CHECKLIST](docs/RELEASE_CHECKLIST.md).

## Documentation

- [Build and release](docs/BUILD.md) · [Testing](docs/TESTING.md)
- [Architecture](docs/ARCHITECTURE.md) · [Persistence](docs/PERSISTENCE.md)
- [Gameplay](docs/GAMEPLAY.md) · [Design](docs/DESIGN.md) · [Assets](docs/ASSETS.md)
- [Privacy](docs/PRIVACY_POLICY.md) · [Data safety](docs/DATA_SAFETY.md)
- [Security/tooling audit](docs/TOOLING_SECURITY.md) · [Performance](docs/PERFORMANCE.md)
- [Platform parity](docs/PARITY.md) · [iOS](ios/README.md)
- [Release checklist](docs/RELEASE_CHECKLIST.md) · [Release notes](docs/RELEASE_NOTES.md)
- [Roadmap](docs/ROADMAP.md) · [Requirements](docs/REQUIREMENTS.md)

MIT — [LICENSE](LICENSE).
