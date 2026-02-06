#!/usr/bin/env bash
set -euo pipefail

KIND="${1:-phone}"
TAG="${2:-$(date +%Y%m%d_%H%M%S)}"
PACKAGE_BASE="${BP_PACKAGE:-com.breakoutplus}"
OUTPUT_ROOT="store_assets/screenshots/${KIND}/${TAG}"
DISPLAY_ID="${BP_DISPLAY_ID:-}"
AUTO_CAPTURE="${BP_AUTO:-0}"
SHOT_DELAY="${BP_SHOT_DELAY:-3}"
LAUNCH_WAIT="${BP_LAUNCH_WAIT:-2}"
LAUNCH_EACH="${BP_LAUNCH_EACH:-1}"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found. Install Android platform-tools and ensure adb is in PATH." >&2
  exit 1
fi

adb devices | sed -n '1,3p'

if [[ -z "${DISPLAY_ID}" ]]; then
  DISPLAY_IDS=$(adb shell dumpsys SurfaceFlinger --display-id 2>/dev/null | tr -d '\r' | awk '/Display/{print $2}' | tr -d '()' | paste -sd ',' -)
  if [[ -n "${DISPLAY_IDS}" ]] && [[ "${DISPLAY_IDS}" == *","* ]]; then
    if [[ "${AUTO_CAPTURE}" == "1" ]]; then
      DISPLAY_ID="${DISPLAY_IDS%%,*}"
      echo "Multiple displays detected: ${DISPLAY_IDS}. Defaulting to ${DISPLAY_ID} (set BP_DISPLAY_ID to override)."
    else
      echo "Multiple displays detected: ${DISPLAY_IDS}"
      read -r -p "Enter display id to capture (blank for default): " DISPLAY_ID
    fi
  else
    DISPLAY_ID="${DISPLAY_IDS}"
  fi
fi

PACKAGES=$(adb shell pm list packages 2>/dev/null | tr -d '\r')
if [[ "${PACKAGE_BASE}" == *.debug ]]; then
  if echo "${PACKAGES}" | grep -q "^package:${PACKAGE_BASE}$"; then
    PACKAGE="${PACKAGE_BASE}"
  else
    echo "Package ${PACKAGE_BASE} not found on device. Install the app first." >&2
    exit 1
  fi
else
  if echo "${PACKAGES}" | grep -q "^package:${PACKAGE_BASE}$"; then
    PACKAGE="${PACKAGE_BASE}"
  elif echo "${PACKAGES}" | grep -q "^package:${PACKAGE_BASE}.debug$"; then
    PACKAGE="${PACKAGE_BASE}.debug"
  else
    echo "Package ${PACKAGE_BASE} not found on device. Install the app first." >&2
    exit 1
  fi
fi

ACTIVITY_SPLASH="${PACKAGE}/com.breakoutplus.SplashActivity"
ACTIVITY_MAIN="${PACKAGE}/com.breakoutplus.MainActivity"
ACTIVITY_MODE="${PACKAGE}/com.breakoutplus.ModeSelectActivity"
ACTIVITY_SETTINGS="${PACKAGE}/com.breakoutplus.SettingsActivity"
ACTIVITY_SCOREBOARD="${PACKAGE}/com.breakoutplus.ScoreboardActivity"
ACTIVITY_DAILY="${PACKAGE}/com.breakoutplus.DailyChallengesActivity"
ACTIVITY_GAME="${PACKAGE}/com.breakoutplus.GameActivity"

launch_activity() {
  local shot="$1"
  local mode="${BP_GAME_MODE:-CLASSIC}"
  case "${shot}" in
    title) adb shell am start -n "${ACTIVITY_MAIN}" >/dev/null 2>&1 || true ;;
    mode_select) adb shell am start -n "${ACTIVITY_MODE}" >/dev/null 2>&1 || true ;;
    settings) adb shell am start -n "${ACTIVITY_SETTINGS}" >/dev/null 2>&1 || true ;;
    scoreboard) adb shell am start -n "${ACTIVITY_SCOREBOARD}" >/dev/null 2>&1 || true ;;
    daily_challenges) adb shell am start -n "${ACTIVITY_DAILY}" >/dev/null 2>&1 || true ;;
    gameplay|powerup|pause)
      adb shell am start -n "${ACTIVITY_GAME}" --es extra_mode "${mode}" >/dev/null 2>&1 || true
      ;;
    *)
      adb shell am start -n "${ACTIVITY_MAIN}" >/dev/null 2>&1 || true
      ;;
  esac
  sleep "${LAUNCH_WAIT}"
}

tap_pause_if_needed() {
  local shot="$1"
  [[ "${shot}" != "pause" ]] && return 0
  local size
  size=$(adb shell wm size | tr -d '\r' | awk -F' ' '/Physical size/{print $3}')
  local w="${size%x*}"
  local h="${size#*x}"
  if [[ -n "${w}" && -n "${h}" ]]; then
    local x=$((w / 2))
    local y=$((h / 10))
    adb shell input tap "${x}" "${y}" >/dev/null 2>&1 || true
    sleep 1
  fi
}

mkdir -p "${OUTPUT_ROOT}"

adb shell am force-stop "${PACKAGE}" >/dev/null 2>&1 || true
adb shell am start -n "${ACTIVITY_SPLASH}" >/dev/null 2>&1 || true
sleep "${LAUNCH_WAIT}"

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
  if [[ "${LAUNCH_EACH}" == "1" ]]; then
    launch_activity "${shot}"
    tap_pause_if_needed "${shot}"
  fi
  if [[ "${AUTO_CAPTURE}" == "1" ]]; then
    echo "Auto-capturing ${shot} in ${SHOT_DELAY}s..."
    sleep "${SHOT_DELAY}"
  else
    read -r -p "Press Enter to capture, or type 'skip' to skip: " reply
    if [[ "${reply}" == "skip" ]]; then
      echo "Skipping ${shot}"
      continue
    fi
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
