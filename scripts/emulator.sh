#!/usr/bin/env bash
# Opens the app on an emulator for manual testing: boots the AVD if no device is attached,
# installs the current debug build, and launches it. Nothing else - the user tests by hand from the
# ticket's Manual steps (see .claude/rules/test-execution.md).
#
# Usage: scripts/emulator.sh [avd-name]      (default: Pixel_7, or $AVD)
set -euo pipefail

cd "$(dirname "$0")/.."

AVD="${1:-${AVD:-Pixel_7}}"
BOOT_TIMEOUT=240

. scripts/lib/android-sdk.sh

# ./gradlew needs a JDK to launch; fall back to Android Studio's bundled one on Windows.
if [ -z "${JAVA_HOME:-}" ] && [ -d "/c/Program Files/Android/Android Studio/jbr" ]; then
  export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
fi

device_count() {
  "$adb" devices | awk 'NR > 1 && $2 == "device"' | wc -l
}

if [ "$(device_count)" -eq 0 ]; then
  if ! "$emulator" -list-avds | grep -qx "$AVD"; then
    echo "No device attached and no AVD named '$AVD'. Available:" >&2
    "$emulator" -list-avds >&2
    exit 1
  fi
  echo "Starting emulator $AVD..."
  nohup "$emulator" -avd "$AVD" >/dev/null 2>&1 &
  "$adb" wait-for-device

  echo "Waiting for boot..."
  waited=0
  until [ "$("$adb" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
    if [ "$waited" -ge "$BOOT_TIMEOUT" ]; then
      echo "Emulator didn't finish booting within ${BOOT_TIMEOUT}s." >&2
      exit 1
    fi
    sleep 3
    waited=$((waited + 3))
  done
elif [ "$(device_count)" -gt 1 ]; then
  echo "More than one device attached - set ANDROID_SERIAL to pick one." >&2
  [ -n "${ANDROID_SERIAL:-}" ] || exit 1
fi

echo "Installing debug build..."
./gradlew installDebug --console=plain -q

# Clear the crash buffer so scripts/app-status.sh only reports crashes from this launch.
"$adb" logcat -b crash -c
"$adb" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
"$adb" shell am start -W -n "$APP_ID/.MainActivity" >/dev/null
sleep 2

# A launch that crashes still "succeeds" from am start's point of view - check it actually came up.
if ! scripts/app-status.sh >/dev/null; then
  scripts/app-status.sh >&2 || true
  echo "App was installed but didn't open cleanly - see above." >&2
  exit 1
fi
echo "App installed and opened on $("$adb" shell getprop ro.product.model | tr -d '\r') - ready for manual testing."
