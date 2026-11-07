#!/usr/bin/env bash
set -euo pipefail
SDK="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
AVD="${AVD_NAME:-tv36}"
LOG="/tmp/opencode/emulator.log"

if ! adb devices | grep -q "emulator-.*device"; then
  mkdir -p /tmp/opencode
  nohup "$SDK/emulator/emulator" -avd "$AVD" -no-window -no-audio -no-snapshot \
    -gpu swiftshader_indirect >"$LOG" 2>&1 &
  echo "emulator başlatıldı, log: $LOG"
fi

adb wait-for-device
for _ in $(seq 1 90); do
  booted="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
  if [ "$booted" = "1" ]; then
    echo "boot completed"
    adb shell settings put global window_animation_scale 0 >/dev/null 2>&1 || true
    adb shell settings put global transition_animation_scale 0 >/dev/null 2>&1 || true
    adb shell settings put global animator_duration_scale 0 >/dev/null 2>&1 || true
    exit 0
  fi
  sleep 2
done
echo "emülatör 180 s içinde açılmadı" >&2
exit 1
