# Build & Run

## Requirements
- JDK 17
- Android SDK (platform 35 installed)
- ADB in PATH

## Build
```bash
./gradlew assembleDebug
```

## Install to Device
```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.breakoutplus.debug/com.breakoutplus.SplashActivity
```

## Regenerate Audio
```bash
python3 tools/generate_sfx.py
```

## Troubleshooting
- If build tools are missing, use `sdkmanager` to install platform 35.
- If ADB does not see the device, confirm USB debugging and cable.
