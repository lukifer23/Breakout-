#!/usr/bin/env bash
set -euo pipefail

KIND="${1:-phone}"
TAG="${2:-$(date +%Y%m%d_%H%M%S)}"
PACKAGE_BASE="${BP_PACKAGE:-com.breakoutplus}"
OUTPUT_ROOT="store_assets/screenshots/${KIND}/${TAG}"
DISPLAY_ID="${BP_DISPLAY_ID:-}"
AUTO_CAPTURE="${BP_AUTO:-0}"
SHOT_DELAY="${BP_SHOT_DELAY:-4}"
LAUNCH_WAIT="${BP_LAUNCH_WAIT:-3}"
LAUNCH_EACH="${BP_LAUNCH_EACH:-1}"
MANUAL_SHOTS="${BP_MANUAL_SHOTS-gameplay powerup pause}"
SHOTS_RAW="${BP_SHOTS:-}"
GAME_MODES_RAW="${BP_GAME_MODES:-}"
AUTO_PLAY="${BP_AUTO_PLAY:-${AUTO_CAPTURE}}"
PLAY_WAIT="${BP_PLAY_WAIT:-2}"
POWERUP_WAIT="${BP_POWERUP_WAIT:-4}"
POWERUP_TYPE="${BP_POWERUP_TYPE:-LASER}"
DEFAULT_GAME_MODE="${BP_GAME_MODE:-CLASSIC}"
PREFER_EMULATOR="${BP_PREFER_EMULATOR:-0}"
ADB_SERIAL="${BP_SERIAL:-${ANDROID_SERIAL:-}}"
EMULATOR_AVD="${BP_EMULATOR_AVD:-${BP_AVD:-}}"
EMULATOR_TIMEOUT="${BP_EMULATOR_TIMEOUT:-180}"
EMULATOR_HEADLESS="${BP_EMULATOR_HEADLESS:-0}"
EMULATOR_ARGS="${BP_EMULATOR_ARGS:-}"
APK_PATH="${BP_APK:-}"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found. Install Android platform-tools and ensure adb is in PATH." >&2
  exit 1
fi

adb_cmd() {
  if [[ -n "${ADB_SERIAL}" ]]; then
    adb -s "${ADB_SERIAL}" "$@"
  else
    adb "$@"
  fi
}

list_devices() {
  adb devices | awk 'NR>1 && $2=="device" {print $1}'
}

wait_for_boot() {
  local elapsed=0
  while [[ $elapsed -lt ${EMULATOR_TIMEOUT} ]]; do
    if [[ "$(adb_cmd shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
      adb_cmd shell input keyevent 82 >/dev/null 2>&1 || true
      adb_cmd shell wm dismiss-keyguard >/dev/null 2>&1 || true
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  echo "Timed out waiting for device boot completion." >&2
  return 1
}

start_emulator() {
  if [[ -z "${EMULATOR_AVD}" ]]; then
    return 1
  fi
  if ! command -v emulator >/dev/null 2>&1; then
    echo "emulator not found. Install Android SDK emulator and ensure it is in PATH." >&2
    return 1
  fi
  local args=(-avd "${EMULATOR_AVD}" -no-snapshot-save -no-audio)
  if [[ "${EMULATOR_HEADLESS}" == "1" ]]; then
    args+=(-no-window)
  fi
  if [[ -n "${EMULATOR_ARGS}" ]]; then
    # shellcheck disable=SC2206
    extra_args=(${EMULATOR_ARGS})
    args+=("${extra_args[@]}")
  fi
  echo "Starting emulator ${EMULATOR_AVD}..."
  emulator "${args[@]}" >/dev/null 2>&1 &
  local elapsed=0
  while [[ $elapsed -lt ${EMULATOR_TIMEOUT} ]]; do
    emulators=()
    while IFS= read -r line; do
      [[ -n "${line}" ]] && emulators+=("${line}")
    done < <(list_devices | grep '^emulator-' || true)
    if [[ ${#emulators[@]} -gt 0 ]]; then
      ADB_SERIAL="${emulators[0]}"
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  echo "Timed out waiting for emulator to appear in adb devices." >&2
  return 1
}

ensure_device() {
  if [[ -n "${ADB_SERIAL}" ]]; then
    if ! adb -s "${ADB_SERIAL}" get-state >/dev/null 2>&1; then
      echo "Device ${ADB_SERIAL} not available. Check adb devices." >&2
      exit 1
    fi
    return 0
  fi

  if [[ "${PREFER_EMULATOR}" == "1" && -n "${EMULATOR_AVD}" ]]; then
    if start_emulator; then
      adb_cmd wait-for-device >/dev/null 2>&1 || true
      wait_for_boot || true
      return 0
    fi
  fi

  devices=()
  while IFS= read -r line; do
    [[ -n "${line}" ]] && devices+=("${line}")
  done < <(list_devices || true)
  if [[ ${#devices[@]} -eq 0 ]]; then
    if ! start_emulator; then
      echo "No adb devices found. Connect a device or set BP_EMULATOR_AVD." >&2
      exit 1
    fi
    devices=()
    while IFS= read -r line; do
      [[ -n "${line}" ]] && devices+=("${line}")
    done < <(list_devices || true)
  fi

  if [[ ${#devices[@]} -eq 1 ]]; then
    ADB_SERIAL="${devices[0]}"
  else
    if [[ "${AUTO_CAPTURE}" == "1" ]]; then
      echo "Multiple devices detected: ${devices[*]}" >&2
      echo "Set BP_SERIAL to choose a device." >&2
      exit 1
    fi
    echo "Multiple devices detected: ${devices[*]}"
    read -r -p "Enter device serial to use: " ADB_SERIAL
  fi

  adb_cmd wait-for-device >/dev/null 2>&1 || true
  wait_for_boot || true
}

ensure_device
if [[ -n "${ADB_SERIAL}" ]]; then
  echo "Using device: ${ADB_SERIAL}"
fi
adb devices | sed -n '1,3p' || true

if [[ -z "${DISPLAY_ID}" ]]; then
  DISPLAY_ID=$(adb_cmd shell dumpsys display 2>/dev/null | tr -d '\r' | awk '
    /isActive=true/ && match($0, /local:[0-9]+/) {
      id=substr($0, RSTART+6, RLENGTH-6);
      print id;
      found=1
    }
    END { if (!found) exit 1 }')
fi

if [[ -n "${DISPLAY_ID}" ]] && [[ ! "${DISPLAY_ID}" =~ ^[0-9]+$ ]]; then
  DISPLAY_ID=""
fi

if [[ -z "${DISPLAY_ID}" ]]; then
  DISPLAY_IDS=$(adb_cmd shell dumpsys SurfaceFlinger --display-id 2>/dev/null | tr -d '\r' | awk '/Display/{print $2}' | tr -d '()' | paste -sd ',' -)
  if [[ -n "${DISPLAY_IDS}" ]] && [[ "${DISPLAY_IDS}" == *","* ]]; then
    if [[ "${AUTO_CAPTURE}" == "1" ]]; then
      echo "Multiple displays detected: ${DISPLAY_IDS}"
      echo "Set BP_DISPLAY_ID to the display you want to capture (auto mode will not guess)."
      exit 1
    else
      echo "Multiple displays detected: ${DISPLAY_IDS}"
      read -r -p "Enter display id to capture (blank for default): " DISPLAY_ID
    fi
  else
    DISPLAY_ID="${DISPLAY_IDS}"
  fi
fi

PACKAGES=$(adb_cmd shell pm list packages 2>/dev/null | tr -d '\r')

ensure_package_installed() {
  local apk="${APK_PATH}"
  if [[ -z "${apk}" && -f "app/build/outputs/apk/debug/app-debug.apk" ]]; then
    apk="app/build/outputs/apk/debug/app-debug.apk"
  fi
  if [[ -n "${apk}" ]]; then
    echo "Installing ${apk}..."
    adb_cmd install -r "${apk}" >/dev/null 2>&1 || true
    PACKAGES=$(adb_cmd shell pm list packages 2>/dev/null | tr -d '\r')
  fi
}
if [[ "${PACKAGE_BASE}" == *.debug ]]; then
  if echo "${PACKAGES}" | grep -q "^package:${PACKAGE_BASE}$"; then
    PACKAGE="${PACKAGE_BASE}"
  else
    ensure_package_installed
    if echo "${PACKAGES}" | grep -q "^package:${PACKAGE_BASE}$"; then
      PACKAGE="${PACKAGE_BASE}"
    else
      echo "Package ${PACKAGE_BASE} not found on device. Install the app first." >&2
      exit 1
    fi
  fi
else
  if echo "${PACKAGES}" | grep -q "^package:${PACKAGE_BASE}$"; then
    PACKAGE="${PACKAGE_BASE}"
  elif echo "${PACKAGES}" | grep -q "^package:${PACKAGE_BASE}.debug$"; then
    PACKAGE="${PACKAGE_BASE}.debug"
  else
    ensure_package_installed
    if echo "${PACKAGES}" | grep -q "^package:${PACKAGE_BASE}$"; then
      PACKAGE="${PACKAGE_BASE}"
    elif echo "${PACKAGES}" | grep -q "^package:${PACKAGE_BASE}.debug$"; then
      PACKAGE="${PACKAGE_BASE}.debug"
    else
      echo "Package ${PACKAGE_BASE} not found on device. Install the app first." >&2
      exit 1
    fi
  fi
fi

ACTIVITY_SPLASH="${PACKAGE}/com.breakoutplus.SplashActivity"
ACTIVITY_MAIN="${PACKAGE}/com.breakoutplus.MainActivity"
ACTIVITY_MODE="${PACKAGE}/com.breakoutplus.ModeSelectActivity"
ACTIVITY_SETTINGS="${PACKAGE}/com.breakoutplus.SettingsActivity"
ACTIVITY_SCOREBOARD="${PACKAGE}/com.breakoutplus.ScoreboardActivity"
ACTIVITY_DAILY="${PACKAGE}/com.breakoutplus.DailyChallengesActivity"
ACTIVITY_GAME="${PACKAGE}/com.breakoutplus.GameActivity"

wait_for_focus() {
  local pkg="$1"
  local retries=10
  local i=0
  while [[ $i -lt $retries ]]; do
    if adb_cmd shell dumpsys window 2>/dev/null | tr -d '\r' | grep -Eq "mCurrentFocus=.*${pkg}|mFocusedApp=.*${pkg}"; then
      return 0
    fi
    sleep 0.5
    i=$((i+1))
  done
  return 1
}

launch_activity() {
  local shot="$1"
  local mode="${2:-${DEFAULT_GAME_MODE}}"
  case "${shot}" in
    title) adb_cmd shell am start -n "${ACTIVITY_MAIN}" >/dev/null 2>&1 || true ;;
    mode_select) adb_cmd shell am start -n "${ACTIVITY_MODE}" >/dev/null 2>&1 || true ;;
    settings) adb_cmd shell am start -n "${ACTIVITY_SETTINGS}" >/dev/null 2>&1 || true ;;
    scoreboard) adb_cmd shell am start -n "${ACTIVITY_SCOREBOARD}" >/dev/null 2>&1 || true ;;
    daily_challenges) adb_cmd shell am start -n "${ACTIVITY_DAILY}" >/dev/null 2>&1 || true ;;
    gameplay|pause)
      adb_cmd shell am start -n "${ACTIVITY_GAME}" --es extra_mode "${mode}" >/dev/null 2>&1 || true
      ;;
    powerup)
      adb_cmd shell am start -n "${ACTIVITY_GAME}" --es extra_mode "${mode}" --es extra_debug_powerup "${POWERUP_TYPE}" >/dev/null 2>&1 || true
      ;;
    *)
      adb_cmd shell am start -n "${ACTIVITY_MAIN}" >/dev/null 2>&1 || true
      ;;
  esac
  wait_for_focus "${PACKAGE}" || true
  sleep "${LAUNCH_WAIT}"
}

tap_pause_if_needed() {
  local shot="$1"
  [[ "${shot}" != "pause" ]] && return 0
  local size
  size=$(adb_cmd shell wm size | tr -d '\r' | awk -F' ' '/Physical size/{print $3}')
  local w="${size%x*}"
  local h="${size#*x}"
  if [[ -n "${w}" && -n "${h}" ]]; then
    local x=$((w / 2))
    local y=$((h / 10))
    adb_cmd shell input tap "${x}" "${y}" >/dev/null 2>&1 || true
    sleep 1
  fi
}

SCREEN_W=""
SCREEN_H=""

ensure_screen_size() {
  if [[ -n "${SCREEN_W}" && -n "${SCREEN_H}" ]]; then
    return 0
  fi
  local size
  size=$(adb_cmd shell wm size | tr -d '\r' | awk -F' ' '/Physical size/{print $3}')
  SCREEN_W="${size%x*}"
  SCREEN_H="${size#*x}"
}

tap_center() {
  ensure_screen_size
  if [[ -n "${SCREEN_W}" && -n "${SCREEN_H}" ]]; then
    local x=$((SCREEN_W / 2))
    local y=$((SCREEN_H / 2))
    adb_cmd shell input tap "${x}" "${y}" >/dev/null 2>&1 || true
  fi
}

start_game_if_needed() {
  local shot="$1"
  if [[ "${AUTO_PLAY}" != "1" ]]; then
    return 0
  fi
  if [[ "${shot}" == "gameplay" || "${shot}" == "powerup" || "${shot}" == "pause" ]]; then
    tap_center
    sleep "${PLAY_WAIT}"
    if [[ "${shot}" == "powerup" ]]; then
      sleep "${POWERUP_WAIT}"
    fi
  fi
}

mkdir -p "${OUTPUT_ROOT}"

adb_cmd shell am force-stop "${PACKAGE}" >/dev/null 2>&1 || true
adb_cmd shell am start -n "${ACTIVITY_SPLASH}" >/dev/null 2>&1 || true
sleep "${LAUNCH_WAIT}"

if [[ -n "${SHOTS_RAW}" ]]; then
  # shellcheck disable=SC2206
  SHOTS=(${SHOTS_RAW})
else
  SHOTS=(
    "title"
    "mode_select"
    "gameplay"
    "powerup"
    "pause"
    "scoreboard"
    "daily_challenges"
    "settings"
  )
fi

if [[ -n "${GAME_MODES_RAW}" ]]; then
  # shellcheck disable=SC2206
  GAME_MODES=(${GAME_MODES_RAW})
else
  GAME_MODES=()
fi

capture_shot() {
  local shot="$1"
  local mode="$2"
  local label="$3"
  echo "\nPrepare screen: ${label}"
  if [[ "${LAUNCH_EACH}" == "1" ]]; then
    launch_activity "${shot}" "${mode}"
    start_game_if_needed "${shot}"
    tap_pause_if_needed "${shot}"
  fi
  manual_required=0
  for manual in ${MANUAL_SHOTS}; do
    if [[ "${shot}" == "${manual}" ]]; then
      manual_required=1
      break
    fi
  done
  if [[ "${AUTO_CAPTURE}" == "1" && "${manual_required}" == "0" ]]; then
    echo "Auto-capturing ${label} in ${SHOT_DELAY}s..."
    sleep "${SHOT_DELAY}"
  else
    read -r -p "Press Enter to capture, or type 'skip' to skip: " reply
    if [[ "${reply}" == "skip" ]]; then
      echo "Skipping ${label}"
      return 0
    fi
  fi
  DEVICE_PATH="/sdcard/Download/breakoutplus_${label}.png"
  LOCAL_PATH="${OUTPUT_ROOT}/${label}.png"
  if [[ -n "${DISPLAY_ID}" ]]; then
    if ! adb_cmd exec-out screencap -p -d "${DISPLAY_ID}" > "${LOCAL_PATH}" 2>/dev/null; then
      adb_cmd exec-out screencap -p > "${LOCAL_PATH}" 2>/dev/null || true
    fi
  else
    adb_cmd exec-out screencap -p > "${LOCAL_PATH}" 2>/dev/null || true
  fi
  if [[ ! -s "${LOCAL_PATH}" ]]; then
    if [[ -n "${DISPLAY_ID}" ]]; then
      adb_cmd shell screencap -p -d "${DISPLAY_ID}" "${DEVICE_PATH}" >/dev/null 2>&1 || true
    else
      adb_cmd shell screencap -p "${DEVICE_PATH}" >/dev/null 2>&1 || true
    fi
    adb_cmd pull "${DEVICE_PATH}" "${LOCAL_PATH}" >/dev/null 2>&1 || true
    adb_cmd shell rm "${DEVICE_PATH}" >/dev/null 2>&1 || true
  fi
  echo "Saved ${LOCAL_PATH}"
  sleep 1
}

for shot in "${SHOTS[@]}"; do
  if [[ "${shot}" == "gameplay" || "${shot}" == "powerup" || "${shot}" == "pause" ]]; then
    if [[ ${#GAME_MODES[@]} -eq 0 ]]; then
      capture_shot "${shot}" "" "${shot}"
    else
      for mode in "${GAME_MODES[@]}"; do
        mode_label=$(echo "${mode}" | tr '[:upper:]' '[:lower:]')
        capture_shot "${shot}" "${mode}" "${shot}_${mode_label}"
      done
    fi
  else
    capture_shot "${shot}" "" "${shot}"
  fi
done

echo "\nScreenshots saved under ${OUTPUT_ROOT}"
