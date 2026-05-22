# Build & Run

## Requirements
- **JDK 17** (required — JDK 21+ may work; JDK 26 is known to break Gradle in this project)
- Android SDK platform 35
- `adb` on PATH
- Ruby + Bundler (for Fastlane Play uploads)

Set JDK explicitly when multiple versions are installed:
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
```

## Android Debug Build
```bash
./gradlew :app:assembleDebug
```
Output:
- `app/build/outputs/apk/debug/app-debug.apk`

Current version: **1.0.11** (versionCode 11) — see `app/build.gradle.kts`.

## Install on Device
```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.breakoutplus.debug/com.breakoutplus.MainActivity
```

When multiple devices are connected, target a single device explicitly:
```bash
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <serial> shell am start -n com.breakoutplus.debug/com.breakoutplus.MainActivity
```

## Android Release Build
```bash
export BP_RELEASE_STORE_FILE="/absolute/path/to/keystore.jks"
export BP_RELEASE_STORE_PASSWORD="your_store_password"
export BP_RELEASE_KEY_ALIAS="your_key_alias"
export BP_RELEASE_KEY_PASSWORD="your_key_password"
./gradlew :app:assembleRelease
```
Output:
- `app/build/outputs/apk/release/app-release.apk`

## Play Store Bundle (AAB)
```bash
./gradlew :app:bundleRelease
```
Output:
- `app/build/outputs/bundle/release/app-release.aab`

Important:
- `BP_RELEASE_*` signing env vars are required for release tasks.
- Release/bundle tasks fail fast if signing vars are missing to avoid producing invalid artifacts.

## CI (GitHub Actions)
On every push/PR to `main`, `.github/workflows/android.yml` runs:
```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```
Uses JDK 17 on `ubuntu-latest`.

## Fastlane Play Uploads
Install gems:
```bash
bundle install
```
If Bundler version mismatch occurs with `Gemfile.lock`, install the expected Bundler first:
```bash
gem install bundler:2.5.11
```
Set service account JSON path (preferred var):
```bash
export GOOGLE_PLAY_JSON="/absolute/path/to/service-account.json"
```
Upload lanes:
```bash
bundle exec fastlane android build_and_upload_internal
bundle exec fastlane android upload_internal
```

## Validation Commands
```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

See also: [`TESTING.md`](TESTING.md), [`HARDENING_SIGNOFF.md`](HARDENING_SIGNOFF.md).

## Device Mode Smoke Test
```bash
tools/mode_smoke_test.sh
```

## Regenerate Audio Assets
```bash
python3 tools/generate_sfx.py
```

## iOS (Out of Android Release Scope)
```bash
cd ios/BreakoutPlus
xcodebuild -scheme BreakoutPlus -sdk iphonesimulator -configuration Debug build
```
Or from repo root: `./ios/run_ios_sim.sh --simulator "iPhone 17 Pro"`

iOS parity status: [`PARITY.md`](PARITY.md). Mac target (`BreakoutPlusMac`) is frozen dev-only.
