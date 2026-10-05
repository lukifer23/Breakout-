# Build and release

Android is the reference platform. Version code/name live in `version.properties`
(current release line 1.0.12 / 12). Increment both intentionally before publishing
a new Play release; compile-check artifacts are never upload candidates.

## Clean clone

```sh
gh repo clone lukifer23/Breakout- Breakout-
cd Breakout-
export JAVA_HOME=/absolute/path/to/jdk-17
export ANDROID_HOME=/absolute/path/to/android-sdk
# Install SDK platform 36 and build-tools 35.0.0 with SDK Manager.
./gradlew clean testDebugUnitTest lintDebug assembleDebug
./gradlew assembleReleaseCheck bundleReleaseCheck
```

AGP 8.10.1 / Gradle 8.11.1 / Kotlin 2.2.21 are stable and compatible.
Compile/target SDK 36; minimum 26; Java 17. The wrapper checksum is verified.
The `.compilecheck` application ID and `-compilecheck` version suffix identify
debug-signed minified validation builds. `CI=true` does not permit release signing
fallback. Compile-check APK/AAB live under `app/build/outputs/*/releaseCheck/`.

```sh
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <serial> shell am start -n com.breakoutplus.debug/com.breakoutplus.MainActivity
```

## Signed release build

Set `BP_RELEASE_STORE_FILE` (absolute keystore path), `BP_RELEASE_STORE_PASSWORD`,
`BP_RELEASE_KEY_ALIAS`, `BP_RELEASE_KEY_PASSWORD` in the environment; never put
their values in source. Then:

```sh
./gradlew assembleRelease bundleRelease
```

Packaging a release fails without signing even in CI. Local compilation,
validation and signed bundle builds require no Google Play publishing credentials.
Verify your upload certificate matches Play Console; build success is not proof
of signing-key acceptance or store compliance. Release AAB is
`app/build/outputs/bundle/release/app-release.aab`.

## Ruby release tools

Ruby 3.3-4.0 and Bundler (locked version in Gemfile.lock):

```sh
bundle install
bundle exec bundler-audit check --update
ruby tools/check_release_contract.rb
bundle exec fastlane lanes
bundle exec fastlane android compile_check
bundle exec fastlane android build_release
```

Only upload/publish lanes require `GOOGLE_PLAY_JSON` (path to service-account
JSON; aliases PLAY_SERVICE_ACCOUNT_JSON / GOOGLE_PLAY_SERVICE_ACCOUNT_JSON
remain supported). AAB upload lanes rebuild a signed release before upload,
preventing accidental use of stale/debug-signed compilation outputs.
`upload_internal` creates a draft; `publish_internal` completes the internal
release. `upload_metadata` and `publish_internal_existing` use the version code
from version.properties. Publishing is an explicit operator action.

## iOS

Full Xcode is required (command-line tools alone are insufficient). Choose an
installed simulator with `xcrun simctl list devices available`.

```sh
xcodebuild -project ios/BreakoutPlus/BreakoutPlus.xcodeproj \
  -scheme BreakoutPlus -configuration Debug \
  -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

Physical-device archives require Apple signing/provisioning. The TestFlight
upload script invokes Fastlane; App Store Connect processing remains a separate
verification gate. iOS parity and current local verification limitations are
recorded in PARITY.md and MODERNIZATION_BASELINE.md. The macOS fork is archived.
