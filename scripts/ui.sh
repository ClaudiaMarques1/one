#!/usr/bin/env bash
# Drives the app on the attached device/emulator for /verify's QA pass (#72): read what's on screen, tap
# by visible text, type, swipe, wait, screenshot. Finds elements through `uiautomator dump`, so it works on
# text and content descriptions - not on ids or icons without a label (use tap-xy for those).
#
# Usage: scripts/ui.sh <command> [args]
#   dump                     every labelled element: "<text>  @x,y" (centre)
#   has "<text>"             exit 0 if an element contains <text>, else 1 ("=<text>" = exact match, any command)
#   tap "<text>"             tap the first element containing <text>    (exit 1 if none)
#   tap-last "<text>"        tap the lowest one on screen - e.g. a bottom-nav label also used higher up
#   tap-xy <x> <y>           tap a point (screen pixels)
#   type "<text>"            type into the focused field
#   swipe up|down [times]    scroll the content (up = move further down the page)
#   back                     the Back key
#   wait "<text>" [secs]     wait until <text> shows (default 10 s; exit 1 on timeout)
#   shot <file.png>          screenshot (creates the folder)
#   launch                   restart the app fresh (force-stop + launch, so no saved screen state)
set -euo pipefail

cd "$(dirname "$0")/.."
. scripts/lib/android-sdk.sh

# Git Bash on Windows rewrites /sdcard/... into a Windows path unless told not to.
export MSYS_NO_PATHCONV=1

# One line per element that has a text or content-desc: "<label>\t<cx>\t<cy>".
elements() {
  "$adb" shell uiautomator dump /sdcard/ui.xml >/dev/null
  "$adb" exec-out cat /sdcard/ui.xml | tr '>' '\n' | grep -E '(text|content-desc)="[^"]+"' | while IFS= read -r node; do
    label="$(printf '%s' "$node" | sed -nE 's/.* text="([^"]+)".*/\1/p')"
    [ -n "$label" ] || label="$(printf '%s' "$node" | sed -nE 's/.*content-desc="([^"]+)".*/\1/p')"
    bounds="$(printf '%s' "$node" | sed -nE 's/.*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]".*/\1 \2 \3 \4/p')"
    [ -n "$label" ] && [ -n "$bounds" ] || continue
    set -- $bounds
    label="$(printf '%s' "$label" | sed 's/&amp;/\&/g; s/&lt;/</g; s/&gt;/>/g; s/&quot;/"/g; s/&#39;/'"'"'/g')"
    printf '%s\t%s\t%s\n' "$label" $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))
  done
}

# The matches for <text>, top to bottom: substring by default, exact when <text> starts with "=" ("=2" is day 2, not 20).
matches() {
  elements | awk -F '\t' -v t="$1" '(substr(t, 1, 1) == "=") ? ($1 == substr(t, 2)) : (index($1, t) > 0)' |
    sort -t "$(printf '\t')" -k3,3n
}

tap_match() {
  local pick="$1" text="$2" hit
  hit="$(matches "$text" | $pick -1)"
  if [ -z "$hit" ]; then
    echo "Not on screen: \"$text\"" >&2
    exit 1
  fi
  "$adb" shell input tap "$(printf '%s' "$hit" | cut -f2)" "$(printf '%s' "$hit" | cut -f3)"
  sleep 1
}

cmd="${1:-}"
shift || true
case "$cmd" in
  dump) elements | awk -F '\t' '{ printf "%s  @%s,%s\n", $1, $2, $3 }' ;;
  has) [ -n "$(matches "$1")" ] ;;
  tap) tap_match head "$1" ;;
  tap-last) tap_match tail "$1" ;;
  tap-xy) "$adb" shell input tap "$1" "$2"; sleep 1 ;;
  type) "$adb" shell input text "$(printf '%s' "$1" | sed 's/ /%s/g')" ;;
  swipe)
    size="$("$adb" shell wm size | sed -nE 's/.*: ([0-9]+)x([0-9]+).*/\1 \2/p' | tail -1)"
    set -- "${1:-up}" "${2:-1}" $size
    for _ in $(seq "$2"); do
      if [ "$1" = up ]; then
        "$adb" shell input swipe $(($3 / 2)) $(($4 * 3 / 4)) $(($3 / 2)) $(($4 / 4)) 300
      else
        "$adb" shell input swipe $(($3 / 2)) $(($4 / 4)) $(($3 / 2)) $(($4 * 3 / 4)) 300
      fi
    done
    sleep 1
    ;;
  back) "$adb" shell input keyevent KEYCODE_BACK; sleep 1 ;;
  wait)
    deadline=$((SECONDS + ${2:-10}))
    until [ -n "$(matches "$1")" ]; do
      if [ "$SECONDS" -ge "$deadline" ]; then
        echo "Timed out waiting for \"$1\"" >&2
        exit 1
      fi
      sleep 1
    done
    ;;
  shot)
    mkdir -p "$(dirname "$1")"
    "$adb" exec-out screencap -p > "$1"
    echo "$1"
    ;;
  launch)
    "$adb" shell am force-stop "$APP_ID"
    "$adb" shell am start -W -n "$APP_ID/.MainActivity" >/dev/null
    sleep 2
    ;;
  *)
    sed -n '2,/^set /p' "$0" | grep '^#' | sed 's/^# \{0,1\}//'
    exit 2
    ;;
esac
