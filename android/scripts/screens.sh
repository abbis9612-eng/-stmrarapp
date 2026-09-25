#!/usr/bin/env bash
# يثبّت نسخة المطوّر على المحاكي ويلتقط كل الشاشات (فاتح + داكن).
set -euo pipefail
APK="$1"; OUT="$2"
mkdir -p "$OUT"
adb install -r "$APK"
PKG=app.sanad.coach/.MainActivity

shot() {
  local name="$1"; local wait="$2"; shift 2
  adb shell am start -W -S -n "$PKG" "$@" > /dev/null
  sleep "$wait"
  adb exec-out screencap -p > "$OUT/$name.png"
  echo "captured $name"
}

scroll_shot() {
  local name="$1"
  adb shell input swipe 540 1900 540 700 400
  sleep 1.5
  adb exec-out screencap -p > "$OUT/$name.png"
  echo "captured $name"
}

adb shell cmd uimode night no
shot 00-intro 0.9 --ez fresh true
shot 01-welcome 3 --ez fresh true
shot 02-today-checkin 4 --ez demo true --ez skipIntro true
scroll_shot 03-today-checkin-scrolled
shot 04-today-plan 4 --ez demo true --ez skipIntro true --es checkin HIGH
scroll_shot 05-today-plan-scrolled
scroll_shot 06-today-plan-bottom
shot 07-eat 4 --ez skipIntro true --es route eat
shot 08-move 4 --ez skipIntro true --es route move
scroll_shot 09-move-library
shot 10-exercise 4 --ez skipIntro true --es route exercise/squat
shot 11-player 5 --ez skipIntro true --es route player/strength-10
shot 12-coach 4 --ez skipIntro true --es route coach
shot 13-progress 4 --ez skipIntro true --es route progress
scroll_shot 14-progress-scrolled

adb shell cmd uimode night yes
shot 20-dark-today 4 --ez demo true --ez skipIntro true --es checkin MID
shot 21-dark-move 4 --ez skipIntro true --es route move
shot 22-dark-player 5 --ez skipIntro true --es route player/low-impact-10
shot 23-dark-progress 4 --ez skipIntro true --es route progress
adb shell cmd uimode night no

# أي انهيار؟
if adb logcat -d | grep -E "FATAL EXCEPTION|AndroidRuntime: Process: app.sanad.coach" ; then
  adb logcat -d | grep -A 30 "FATAL EXCEPTION" > "$OUT/crash.txt" || true
  echo "::error::App crashed during screenshots"
  exit 1
fi
