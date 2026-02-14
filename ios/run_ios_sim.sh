#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"/.. && pwd)"
PROJECT_PATH="${PROJECT_PATH:-$ROOT_DIR/ios/BreakoutPlus/BreakoutPlus.xcodeproj}"
SCHEME="${SCHEME:-BreakoutPlus}"
CONFIGURATION="${CONFIGURATION:-Debug}"
SIMULATOR_NAME="${SIMULATOR_NAME:-iPhone 17 Pro}"

usage() {
  cat <<EOF
Usage: $(basename "$0") [--simulator <name>] [--scheme <name>] [--configuration <name>] [--project <path>]

Environment overrides:
  SIMULATOR_NAME, SCHEME, CONFIGURATION, PROJECT_PATH
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --simulator)
      SIMULATOR_NAME="$2"
      shift 2
      ;;
    --scheme)
      SCHEME="$2"
      shift 2
      ;;
    --configuration)
      CONFIGURATION="$2"
      shift 2
      ;;
    --project)
      PROJECT_PATH="$2"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ ! -d "$PROJECT_PATH" ]]; then
  echo "Project not found: $PROJECT_PATH" >&2
  exit 1
fi

echo "Locating simulator: $SIMULATOR_NAME"
DEVICE_LINE="$(xcrun simctl list devices available | awk -v name="$SIMULATOR_NAME" 'index($0, name " (") {print; exit}')"

if [[ -z "$DEVICE_LINE" ]]; then
  echo "No available simulator matched: $SIMULATOR_NAME" >&2
  echo "Available devices:" >&2
  xcrun simctl list devices available | sed -n '1,120p' >&2
  exit 1
fi

UDID="$(printf '%s\n' "$DEVICE_LINE" | sed -nE 's/.*\(([0-9A-F-]{36})\).*/\1/p' | head -n1)"

if [[ -z "$UDID" ]]; then
  echo "Could not resolve simulator UDID from: $DEVICE_LINE" >&2
  exit 1
fi

echo "Using simulator UDID: $UDID"

# Keep a single active simulator to avoid duplicate windows/devices.
BOOTED_UDIDS="$(xcrun simctl list devices | sed -nE 's/.*\(([0-9A-F-]{36})\) \(Booted\).*/\1/p')"
if [[ -n "$BOOTED_UDIDS" ]]; then
  while IFS= read -r booted; do
    [[ -z "$booted" ]] && continue
    if [[ "$booted" != "$UDID" ]]; then
      xcrun simctl shutdown "$booted" >/dev/null 2>&1 || true
    fi
  done <<< "$BOOTED_UDIDS"
fi

xcrun simctl boot "$UDID" >/dev/null 2>&1 || true
xcrun simctl bootstatus "$UDID" -b

echo "Building $SCHEME ($CONFIGURATION) for simulator"
BUILD_LOG="$(mktemp -t breakoutplus_ios_build.XXXXXX.log)"
if ! xcodebuild \
  -project "$PROJECT_PATH" \
  -scheme "$SCHEME" \
  -configuration "$CONFIGURATION" \
  -destination "platform=iOS Simulator,id=$UDID" \
  build >"$BUILD_LOG" 2>&1; then
  echo "Build failed. Log: $BUILD_LOG" >&2
  tail -n 80 "$BUILD_LOG" >&2
  exit 1
fi

echo "Build succeeded"
BUILD_SETTINGS="$(xcodebuild \
  -project "$PROJECT_PATH" \
  -scheme "$SCHEME" \
  -configuration "$CONFIGURATION" \
  -destination "platform=iOS Simulator,id=$UDID" \
  -showBuildSettings)"

TARGET_BUILD_DIR="$(printf '%s\n' "$BUILD_SETTINGS" | awk -F' = ' '/TARGET_BUILD_DIR/ {print $2; exit}')"
WRAPPER_NAME="$(printf '%s\n' "$BUILD_SETTINGS" | awk -F' = ' '/WRAPPER_NAME/ {print $2; exit}')"
BUNDLE_ID="$(printf '%s\n' "$BUILD_SETTINGS" | awk -F' = ' '/PRODUCT_BUNDLE_IDENTIFIER/ {print $2; exit}')"
APP_PATH="$TARGET_BUILD_DIR/$WRAPPER_NAME"

if [[ ! -d "$APP_PATH" ]]; then
  echo "Built app not found at: $APP_PATH" >&2
  exit 1
fi

echo "Installing: $APP_PATH"
if ! xcrun simctl install "$UDID" "$APP_PATH"; then
  echo "Initial install failed; rebooting simulator and retrying once..."
  xcrun simctl shutdown "$UDID" >/dev/null 2>&1 || true
  xcrun simctl boot "$UDID" >/dev/null 2>&1 || true
  xcrun simctl bootstatus "$UDID" -b
  xcrun simctl install "$UDID" "$APP_PATH"
fi

echo "Launching: $BUNDLE_ID"
if ! xcrun simctl launch "$UDID" "$BUNDLE_ID"; then
  echo "Initial launch failed; rebooting simulator and retrying once..."
  xcrun simctl shutdown "$UDID" >/dev/null 2>&1 || true
  xcrun simctl boot "$UDID" >/dev/null 2>&1 || true
  xcrun simctl bootstatus "$UDID" -b
  xcrun simctl launch "$UDID" "$BUNDLE_ID"
fi

echo "Done. CLI iOS build + simulator launch completed."
