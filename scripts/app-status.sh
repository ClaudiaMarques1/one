#!/usr/bin/env bash
# Reports the app's state on the attached device/emulator: installed build, whether it's running and
# in the foreground, and any crash since the last scripts/emulator.sh launch (which clears the crash
# buffer). Exits 1 if the app crashed or isn't in the foreground - so callers never need raw adb.
#
# Usage: scripts/app-status.sh
set -euo pipefail

cd "$(dirname "$0")/.."
. scripts/lib/android-sdk.sh

if [ "$("$adb" devices | awk 'NR > 1 && $2 == "device"' | wc -l)" -eq 0 ]; then
  echo "No device attached - run scripts/emulator.sh first." >&2
  exit 1
fi

status=0

updated="$("$adb" shell dumpsys package "$APP_ID" | sed -n 's/^ *lastUpdateTime=//p' | head -1 | tr -d '\r')"
if [ -z "$updated" ]; then
  echo "Installed:  no"
  exit 1
fi
echo "Installed:  yes (updated $updated)"

pid="$("$adb" shell pidof "$APP_ID" | tr -d '\r' || true)"
if [ -n "$pid" ]; then echo "Running:    yes (pid $pid)"; else echo "Running:    no"; fi

top="$("$adb" shell dumpsys activity activities | sed -n 's/.*topResumedActivity=ActivityRecord{[^ ]* [^ ]* \([^ ]*\).*/\1/p' | head -1 | tr -d '\r')"
echo "Foreground: ${top:-unknown}"
case "$top" in
  "$APP_ID"/*) ;;
  *) status=1 ;;
esac

# Each crash: the FATAL line, process, exception and its causes (no full stack).
crashes="$("$adb" logcat -d -b crash | grep -A3 "Process: $APP_ID," | grep -E "Process:|Exception|Error|Caused by" | sed 's/^.*AndroidRuntime: //' || true)"
if [ -n "$crashes" ]; then
  echo "Crashes:"
  echo "$crashes" | sed 's/ on path: DexPathList.*//; s/^/  /' | cut -c1-200
  status=1
else
  echo "Crashes:    none"
fi

exit $status
