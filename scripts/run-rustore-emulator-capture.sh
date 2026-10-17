#!/usr/bin/env bash
set -euo pipefail

adb shell wm size 1080x1920
adb shell wm density 420
adb shell settings put system screen_off_timeout 1800000
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard || true

adb install -r android/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n ru.doshikologiya.app/.MainActivity
sleep 10
pid="$(adb shell pidof ru.doshikologiya.app | tr -d '\r' | awk '{print $1}')"
if [[ -z "$pid" ]]; then
  echo "::error::Android app process not running"
  adb logcat -d -t 200 | tail -160 || true
  exit 1
fi
echo "App PID=$pid"

socket=""
for attempt in $(seq 1 30); do
  socket="$(adb shell cat /proc/net/unix | tr -d '\r' | grep -oE "webview_devtools_remote_$pid" | head -n 1 || true)"
  if [[ -n "$socket" ]]; then break; fi
  sleep 2
done
echo "WebView debugging socket=$socket"
if [[ -z "$socket" ]]; then
  echo "::error::Debug Android WebView socket not found"
  adb shell cat /proc/net/unix | grep webview || true
  adb logcat -d -t 300 | tail -150 || true
  exit 1
fi

adb forward tcp:9222 "localabstract:$socket"
curl -fsS http://127.0.0.1:9222/json/list | head -c 3000 || true
node scripts/capture-rustore.mjs
