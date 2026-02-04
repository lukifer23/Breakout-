# Build & Run

## Requirements
- JDK 17
- Android SDK (platform 35 installed)
- ADB in PATH

## Build
```bash
./gradlew assembleDebug
```

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

## Install to Device
```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.breakoutplus.debug/com.breakoutplus.SplashActivity
```

## Performance
Breakout+ targets 60 FPS gameplay with OpenGL ES 2.0 hardware acceleration. The game loop uses continuous rendering with delta time clamping at 50ms to prevent large jumps on frame drops. Vsync is handled by the Android system for smooth animation.

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
