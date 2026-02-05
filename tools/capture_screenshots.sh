#!/usr/bin/env bash
set -euo pipefail

KIND="${1:-phone}"
TAG="${2:-$(date +%Y%m%d_%H%M%S)}"
PACKAGE_BASE="${BP_PACKAGE:-com.breakoutplus}"
OUTPUT_ROOT="store_assets/screenshots/${KIND}/${TAG}"
DISPLAY_ID="${BP_DISPLAY_ID:-}"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found. Install Android platform-tools and ensure adb is in PATH." >&2
  exit 1
fi

adb devices | sed -n '1,3p'

if [[ -z "${DISPLAY_ID}" ]]; then
  DISPLAY_IDS=$(adb shell dumpsys SurfaceFlinger --display-id 2>/dev/null | tr -d '\r' | awk '{print $1}' | paste -sd ',' -)
  if [[ -n "${DISPLAY_IDS}" ]] && [[ "${DISPLAY_IDS}" == *","* ]]; then
    echo "Multiple displays detected: ${DISPLAY_IDS}"
    read -r -p "Enter display id to capture (blank for default): " DISPLAY_ID
  fi
fi

PACKAGE="$PACKAGE_BASE"
if ! adb shell pm list packages | grep -q "${PACKAGE_BASE}"; then
  if adb shell pm list packages | grep -q "${PACKAGE_BASE}.debug"; then
    PACKAGE="${PACKAGE_BASE}.debug"
  else
    echo "Package ${PACKAGE_BASE} not found on device. Install the app first." >&2
    exit 1
  fi
fi

ACTIVITY="${PACKAGE}/com.breakoutplus.SplashActivity"

mkdir -p "${OUTPUT_ROOT}"

adb shell am force-stop "${PACKAGE}" >/dev/null 2>&1 || true
adb shell am start -n "${ACTIVITY}" >/dev/null 2>&1 || true
sleep 2

declare -a SHOTS=(
  "title"
  "mode_select"
  "gameplay"
  "powerup"
  "pause"
  "scoreboard"
  "daily_challenges"
  "settings"
)

for shot in "${SHOTS[@]}"; do
  echo "\nPrepare screen: ${shot}"
  read -r -p "Press Enter to capture, or type 'skip' to skip: " reply
  if [[ "${reply}" == "skip" ]]; then
    echo "Skipping ${shot}"
    continue
  fi
  DEVICE_PATH="/sdcard/Download/breakoutplus_${shot}.png"
  LOCAL_PATH="${OUTPUT_ROOT}/${shot}.png"
  if [[ -n "${DISPLAY_ID}" ]]; then
    adb shell screencap -p -d "${DISPLAY_ID}" "${DEVICE_PATH}"
  else
    adb shell screencap -p "${DEVICE_PATH}"
  fi
  adb pull "${DEVICE_PATH}" "${LOCAL_PATH}" >/dev/null
  adb shell rm "${DEVICE_PATH}" >/dev/null 2>&1 || true
  echo "Saved ${LOCAL_PATH}"
  sleep 1
  done

echo "\nScreenshots saved under ${OUTPUT_ROOT}"
