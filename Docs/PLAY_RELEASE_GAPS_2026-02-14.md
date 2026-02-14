# Play Release Gaps (Android) - 2026-02-14

## Build Status
- New release candidate built: `app/build/outputs/bundle/release/app-release.aab`
- Current app version: `versionCode 10`, `versionName 1.0.9`
- SHA-256: `9af783291fd26260539a49a36cfba2abffd29d6a75ed6662cc7295f792cd7464`
- Fastlane upload attempt to internal track failed due signing key mismatch.
  - Found (bundle): `63:58:54:90:7B:E5:31:53:4D:C4:E5:01:F0:7E:E4:4A:40:B0:CA:CE`
  - Expected (Play): `23:82:B9:A4:95:32:2E:5D:44:9C:BC:88:06:77:88:D2:D6:35:D8:B4`

## P0 Blockers (Must Fix Before Upload)
1. Release signing env vars are not configured in the current shell:
   - `BP_RELEASE_STORE_FILE`
   - `BP_RELEASE_STORE_PASSWORD`
   - `BP_RELEASE_KEY_ALIAS`
   - `BP_RELEASE_KEY_PASSWORD`
2. Current `bundleRelease` falls back to debug signing when release signing vars are missing, which is rejected by Play for this app.
3. Bundler lock mismatch blocks `bundle exec fastlane ...`:
   - Missing Bundler `2.5.11` per `Gemfile.lock`.

## Play Console Submission Gaps
1. App Content declarations (`Policy`, `Ads`, `App access`, `Content rating`, `Target audience`, `News apps`, `Data safety`) must be complete and current in Play Console.
2. Data safety form must match app behavior exactly.
   - Current app appears offline-only with local storage; verify declarations against actual runtime behavior.
3. Privacy policy:
   - In-app file exists: `app/src/main/res/raw/privacy_policy.txt`.
   - Ensure a public HTTPS privacy policy URL is set in Play Console and matches Data safety answers.
4. Store listing assets:
   - Present: icon (`512x512`), feature graphic (`1024x500`), multiple phone screenshots.
   - Gap: no tablet screenshots checked in (`store_assets/screenshots/tablet/` only has README).
5. Release notes:
   - Added changelog for code `10`: `fastlane/metadata/android/en-US/changelogs/10.txt`
6. Monetization readiness:
   - No ads/billing SDK detected in Android app module currently.
   - If monetization is planned, configure App Content declarations before rollout.

## Account/Policy Readiness Gaps
1. Developer verification requirements are active; verify account status and required deadlines in Play Console.
2. If this is a personal developer account created after Nov 13, 2023, closed testing requirements apply before production access.
3. If that same account was created after Nov 13, 2023, device verification steps may be required for testing enrollment.

## Technical Compliance Snapshot
1. Target API:
   - App targets SDK 35 (`app/build.gradle.kts`), which aligns with current Play target API guidance.
2. Android App Bundle:
   - `.aab` generation path is working; signing is the remaining blocker.

## Source Links
- App bundles: https://developer.android.com/guide/app-bundle
- Target API requirements: https://developer.android.com/google/play/requirements/target-sdk
- Prepare app for review / App content: https://support.google.com/googleplay/android-developer/answer/9859455
- Data safety: https://support.google.com/googleplay/android-developer/answer/10787469
- User data / privacy policy expectations: https://support.google.com/googleplay/android-developer/answer/10144311
- Store listing and graphics/screenshot specs: https://support.google.com/googleplay/android-developer/answer/9866151
- Payments policy (for monetization): https://support.google.com/googleplay/android-developer/answer/10281818
- Developer verification: https://developer.android.com/developer-verification
- Personal account closed testing requirement: https://support.google.com/googleplay/android-developer/answer/14151465
- Personal account device verification: https://support.google.com/googleplay/android-developer/answer/14177239
