fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android build_release

```sh
[bundle exec] fastlane android build_release
```

Build release AAB

### android upload_internal

```sh
[bundle exec] fastlane android upload_internal
```

Upload AAB and metadata/screenshots to Play (internal track)

### android upload_metadata

```sh
[bundle exec] fastlane android upload_metadata
```

Upload metadata/screenshots only (no AAB)

### android build_and_upload_internal

```sh
[bundle exec] fastlane android build_and_upload_internal
```

Build and upload AAB to Play (internal track)

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
