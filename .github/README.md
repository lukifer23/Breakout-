# Breakout+ repository automation

The [root README](../README.md) is the project source of truth.
[Android CI](workflows/android.yml) runs clean unit/lint/debug/minified APK/AAB
compile checks plus Ruby advisory and release-contract checks. The advisory job also runs weekly. Dependabot security alerts remain enabled;
routine version PRs are disabled for the single-main-branch workflow. Generated products never belong in source.
See [build/release](../Docs/BUILD.md) and [release checklist](../Docs/RELEASE_CHECKLIST.md).
