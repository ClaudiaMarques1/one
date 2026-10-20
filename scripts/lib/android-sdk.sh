# Sourced by scripts/*.sh: finds the Android SDK and sets $adb, $emulator and APP_ID.
# SDK lookup: ANDROID_HOME, ANDROID_SDK_ROOT, then sdk.dir from local.properties (Windows paths are
# escaped there, e.g. C\:\\Users\\...).

APP_ID="app.theone.android"

sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$sdk" ] && [ -f local.properties ]; then
  sdk="$(sed -n 's/^sdk\.dir=//p' local.properties | sed 's/\\:/:/g; s/\\\\/\//g')"
fi
if [ -z "$sdk" ] || [ ! -d "$sdk" ]; then
  echo "Android SDK not found: set ANDROID_HOME or sdk.dir in local.properties." >&2
  exit 1
fi
adb="$sdk/platform-tools/adb"
emulator="$sdk/emulator/emulator"
