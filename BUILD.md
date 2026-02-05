# Build & Run

## Requirements
- JDK 17
- Android SDK (platform 35 installed)
- ADB in PATH

## Build
```bash
./gradlew assembleDebug
```

## Release Bundle (Play Store)
Build the Android App Bundle required by Google Play:
```bash
./gradlew bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab`

## Play Console Uploads (Service Account)
Google Play uploads require a service account JSON key (not an API key). Store it outside git and point tools to it.

Store the key outside the repo or use a path referenced only via `GOOGLE_PLAY_JSON`. Example location (do not commit the file):

```
/path/to/your/service-account.json
```

The repo `.gitignore` already excludes common key filenames. Do not commit the key file.

## Play Console Uploads (Fastlane)
Fastlane is configured for Play uploads.

Install dependencies:
```bash
bundle install
```

Optional (override JSON path):
```bash
export GOOGLE_PLAY_JSON="/absolute/path/to/service-account.json"
```

Build + upload to internal track:
```bash
bundle exec fastlane android build_and_upload_internal
```

Upload only (AAB + metadata/screenshots):
```bash
bundle exec fastlane android upload_internal
```

## Store Listing Metadata
Text metadata used by Fastlane lives in `fastlane/metadata/android/en-US/`.
Update `title.txt`, `short_description.txt`, `full_description.txt`, and changelogs before uploads.

## Release Build (Signed)
Set signing environment variables, then build:
```bash
export BP_RELEASE_STORE_FILE="/absolute/path/to/keystore.jks"
export BP_RELEASE_STORE_PASSWORD="your_store_password"
export BP_RELEASE_KEY_ALIAS="your_key_alias"
export BP_RELEASE_KEY_PASSWORD="your_key_password"
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

If signing variables are not set, the release build will use the debug keystore for local testing.

## Google Play API Key (If Required)
If Play services require an API key, set it via `local.properties` or an environment variable so it never lands in git.

Option A: `local.properties` (recommended)
```
GOOGLE_PLAY_API_KEY=your_key_here
```

Option B: environment variable
```bash
export GOOGLE_PLAY_API_KEY="your_key_here"
```

## Install to Device
```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.breakoutplus.debug/com.breakoutplus.SplashActivity
```

## Performance
Breakout+ targets 60+ FPS gameplay with OpenGL ES 2.0 hardware acceleration. Rendering is paced via Choreographer-driven frame scheduling and surface frame-rate hints, with delta time clamping at 50ms to prevent large jumps on frame drops. Vsync is handled by the Android system for smooth animation.

## iOS Build

```bash
cd ios/BreakoutPlus && xcodebuild -scheme BreakoutPlus -sdk iphonesimulator -configuration Debug build
```

## Regenerate Audio
```bash
python3 tools/generate_sfx.py
```

## Tests
Run unit tests:
```bash
./gradlew test
```

## Troubleshooting
- If build tools are missing, use `sdkmanager` to install platform 35.
- If ADB does not see the device, confirm USB debugging and cable.
