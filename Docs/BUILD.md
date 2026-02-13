# Build & Run

## Requirements
- JDK 17
- Android SDK platform 35
- `adb` on PATH
- Ruby + Bundler (for Fastlane Play uploads)

## Android Debug Build
```bash
./gradlew :app:assembleDebug
```
Output:
- `app/build/outputs/apk/debug/app-debug.apk`

## Install on Device
```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.breakoutplus.debug/com.breakoutplus.MainActivity
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

## Fastlane Play Uploads
Install gems:
```bash
bundle install
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
```

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
