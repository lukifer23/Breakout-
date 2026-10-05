# Tooling and Android 16 audit (2026-10-05)

Starting lock: Fastlane 2.232.0, json 2.18.1, addressable 2.8.8, jwt 2.10.2,
faraday 1.10.4. PRs #4/#5/#6 cover json/addressable/jwt; closed #3 proposed
faraday 1.10.5. No old PR was merged. RubyGems and Fastlane releases were
checked live, then Fastlane and affected transitive graph resolved together.
Result: Fastlane 2.240.1, json 2.21.2 (Fastlane requires <3), addressable 2.9.0,
jwt 3.3.0, faraday 2.14.4. Removed obsolete Faraday 1 adapter graph.

`bundle exec bundler-audit check --update`: no vulnerabilities found against
ruby-advisory-db commit 94dccfdbd4154b44d8a1c7ff7d13cc76727f87d6, 1,254 advisories.
`bundle exec fastlane lanes` and `compile_check` ran successfully on Ruby 4.0.7
with Play credential variables unset. CI also audits on Ruby 3.4; that runner
has not yet executed. Credentials are only checked by uploading lanes.
Actual Play/TestFlight upload is not part of this validation.

## Android 16

Compile/target SDK 36, stable AGP 8.10.1, Gradle 8.11.1, Kotlin 2.2.21, JDK 17.
Existing AndroidX Window 1.5.1, lifecycle 2.10.0, Material 1.13.0 and appcompat
1.7.1 retained; no blind library update. Dependency metadata/build/test checks
pass. Other AndroidX updates are evaluated with their behavioral changes.

The app is a game, so appCategory=game is appropriate. This also identifies
it for Android 16's game exemption from large-screen orientation enforcement.
It still requests fullSensor and supports resizing; the exemption is not used
to lock orientation or remove fold handling. Android 16 enforces edge-to-edge;
GameActivity already consumes system-bar/cutout/gesture insets. Common shell
insets and fold/configuration recreation remain part of the runtime review.

GameActivity consumes size/orientation/density changes to preserve the running
GL-thread engine and relayout authoritative brick geometry; removing that
without tested state restoration would lose progress. GLSurfaceView preserves
its EGL context on pause, but recreation must rebuild shader locations/resources.
Frame-rate requests remain hints, and simulation must not depend on their outcome.

## Sources

- [AGP compatibility](https://developer.android.com/build/releases/agp-8-10-0-release-notes)
- [Kotlin compatibility](https://kotlinlang.org/docs/gradle-configure-project.html)
- [Android 16 behavior](https://developer.android.com/about/versions/16/behavior-changes-16)
- [Play target API policy](https://support.google.com/googleplay/android-developer/answer/11926878)
- [Backup behavior/rules](https://developer.android.com/identity/data/autobackup)
- [Fastlane releases](https://github.com/fastlane/fastlane/releases)

API-36 gates passed: 111 JVM tests, lint (65 warnings, no errors), debug APK,
minified releaseCheck APK and AAB. Gradle bundleRelease with CI=true and no
signing failed as intended. releaseCheck has .compilecheck ID/-compilecheck
version suffix and a debug key; it cannot masquerade as a publishable package.

API-36 emulator smoke: emulator-5556, SDK 36, 1080x2400. All ten modes
started and emitted autoplay session events, four seconds per mode, no logged
fatal render/update errors. This does not establish lifecycle/fold/performance
quality or completion of every mode. Physical hardware remains untouched.
